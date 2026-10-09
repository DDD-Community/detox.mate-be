package com.detoxmate.notification;

import com.detoxmate.applock.domain.TimeLimit;
import com.detoxmate.applock.repository.TimeLimitRepository;
import com.detoxmate.applock.service.TimeLimitService;
import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.friend.domain.Friend;
import com.detoxmate.friend.repository.FriendRepository;
import com.detoxmate.friend.service.FriendService;
import com.detoxmate.notification.domain.DevicePlatform;
import com.detoxmate.notification.domain.FcmToken;
import com.detoxmate.notification.domain.NotificationHistory;
import com.detoxmate.notification.event.GoalSettingReminderEvent;
import com.detoxmate.notification.repository.FcmTokenRepository;
import com.detoxmate.notification.repository.NotificationHistoryRepository;
import com.detoxmate.notification.repository.NotificationRepository;
import com.detoxmate.notification.util.FcmSender;
import com.detoxmate.support.UserFixtures;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationPolicyIntegrationTest {

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:notification-policy;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
    }

    @Autowired private WebApplicationContext applicationContext;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private FriendRepository friendRepository;
    @Autowired private TimeLimitRepository timeLimitRepository;
    @Autowired private FcmTokenRepository tokenRepository;
    @Autowired private NotificationHistoryRepository historyRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private FriendService friendService;
    @Autowired private TimeLimitService timeLimitService;
    @Autowired private TransactionTemplate transactions;
    @Autowired private ApplicationEventPublisher events;
    @Autowired private ScheduledAnnotationBeanPostProcessor scheduledTasks;
    @Autowired private RecordingFcmSender fcm;

    private MockMvc mockMvc;
    private User actor;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        historyRepository.deleteAllInBatch();
        tokenRepository.deleteAllInBatch();
        friendRepository.deleteAllInBatch();
        timeLimitRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        fcm.messages.clear();
        actor = createUser("프로필이름");
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    @DisplayName("재잠금 알림 요청은 예약 없이 본인에게 즉시 푸시만 보낸다")
    void requestRelockReminder_sendsImmediatePushOnlyToCurrentUser() throws Exception {
        // given
        createUser("다른친구");

        // when
        authenticated(actor, post("/notifications/app-relock-reminders"))
                .andExpect(status().isNoContent());

        // then
        assertThat(fcm.messages).singleElement().satisfies(message -> {
            assertThat(message.token()).isEqualTo(token(actor));
            assertThat(message.title()).isEqualTo("Detoxmate");
            assertThat(message.body()).isEqualTo("앱 사용 시간이 얼마 남지 않았어요!");
        });
        assertThat(historyRepository.count()).isZero();
    }

    @Test
    @DisplayName("친구 요청은 커밋 후 수신자에게 요청자 이름과 친구 요청 화면을 보낸다")
    void sendFriendRequest_deliversAfterCommitAndPersistsNavigableHistory() throws Exception {
        // given
        User receiver = createUser("받는친구");

        // when
        transactions.executeWithoutResult(status -> {
            friendService.sendRequest(actor.getId(), receiver.getId());
            assertThat(fcm.messages).isEmpty();
            assertThat(historyRepository.count()).isZero();
        });

        // then
        assertSinglePush(receiver, "프로필이름님이 친구 요청을 보냈어요.", "FRIEND_REQUESTS");
        assertThat(friendRepository.findByUserPair(actor.getId(), receiver.getId())).get()
                .satisfies(friend -> assertThat(friend.isPending()).isTrue());
        assertFriendHistory(receiver, actor, "FRIEND_REQUESTS");
    }

    @Test
    @DisplayName("친구 수락은 커밋 후 최초 요청자에게 수락자 이름과 친구 목록을 보낸다")
    void acceptFriendRequest_deliversToOriginalSenderAfterCommit() throws Exception {
        // given
        User sender = createUser("요청친구");
        Friend pending = friendRepository.saveAndFlush(Friend.request(sender.getId(), actor.getId()));

        // when
        transactions.executeWithoutResult(status -> {
            friendService.acceptRequest(pending.getId(), actor.getId());
            assertThat(fcm.messages).isEmpty();
        });

        // then
        assertSinglePush(sender, "프로필이름님과 친구가 되었어요! 함께 스크린타임을 줄여봐요.", "FRIENDS");
        assertThat(friendRepository.findById(pending.getId())).get()
                .satisfies(friend -> assertThat(friend.isAccepted()).isTrue());
        assertFriendHistory(sender, actor, "FRIENDS");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{나}", "나😀!"})
    @DisplayName("허용된 특수문자와 이모지 프로필 이름은 템플릿 문법으로 해석하지 않고 그대로 전달한다")
    void sendFriendRequest_preservesProfileNameLiterally(String name) throws Exception {
        // given
        actor.changeDisplayName(name);
        actor = userRepository.saveAndFlush(actor);
        User receiver = createUser("친구");

        // when
        authenticated(actor, post("/friends/requests").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("targetUserId", receiver.getId()))))
                .andExpect(status().isCreated());

        // then
        assertSinglePush(receiver, name + "님이 친구 요청을 보냈어요.", "FRIEND_REQUESTS");
    }

    @Test
    @DisplayName("롤백된 친구 요청은 관계와 알림 이력을 남기거나 푸시하지 않는다")
    void sendFriendRequest_rollbackLeavesNoRelationshipOrNotification() {
        // given
        User receiver = createUser("친구");

        // when
        transactions.executeWithoutResult(status -> {
            friendService.sendRequest(actor.getId(), receiver.getId());
            status.setRollbackOnly();
        });

        // then
        assertThat(friendRepository.findByUserPair(actor.getId(), receiver.getId())).isEmpty();
        assertNoDeliveryOrHistory();
    }

    @Test
    @DisplayName("롤백된 친구 수락은 대기 관계를 유지하고 알림을 보내지 않는다")
    void acceptFriendRequest_rollbackKeepsPendingWithoutNotification() {
        // given
        User sender = createUser("친구");
        Friend pending = friendRepository.saveAndFlush(Friend.request(sender.getId(), actor.getId()));

        // when
        transactions.executeWithoutResult(status -> {
            friendService.acceptRequest(pending.getId(), actor.getId());
            status.setRollbackOnly();
        });

        // then
        assertThat(friendRepository.findById(pending.getId())).get()
                .satisfies(friend -> assertThat(friend.isPending()).isTrue());
        assertNoDeliveryOrHistory();
    }

    @Test
    @DisplayName("중복 친구 요청과 권한 없는 수락은 추가 알림 없이 거부한다")
    void friendRequests_rejectDuplicateOrUnauthorizedAcceptanceWithoutNotification() throws Exception {
        // given
        User receiver = createUser("친구");
        Friend pending = friendRepository.saveAndFlush(Friend.request(actor.getId(), receiver.getId()));

        // when & then
        authenticated(actor, post("/friends/requests").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("targetUserId", receiver.getId()))))
                .andExpect(status().isConflict());
        authenticated(actor, post("/friends/requests/{requestId}/accept", pending.getId()))
                .andExpect(status().isForbidden());
        assertNoDeliveryOrHistory();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 5})
    @DisplayName("제한 초과 후 일시 해제는 양방향 활성 친구 중 최대 세 명에게만 분 수를 보낸다")
    void reportTemporaryUnlock_sendsToAtMostThreeActiveAcceptedFriends(int friendCount) throws Exception {
        // given
        List<User> friends = createFriends(friendCount);
        addIneligibleFriends();

        // when
        authenticated(actor, jsonPost("/notifications/app-unlocks",
                "{\"unlockMinutes\":10,\"limitExceeded\":true}"))
                .andExpect(status().isNoContent());

        // then
        assertThat(fcm.messages).hasSize(Math.min(3, friendCount));
        assertThat(fcm.messages).extracting(SentPush::token).doesNotHaveDuplicates()
                .isSubsetOf(friends.stream().map(this::token).toList());
        assertThat(fcm.messages).allSatisfy(message -> {
            assertThat(message.title()).isEqualTo("Detoxmate");
            assertThat(message.body()).isEqualTo("프로필이름님이 잠긴 앱을 10분 동안 일시 해제했어요.");
        });
        assertThat(historyRepository.count()).isZero();
    }

    @Test
    @DisplayName("제한을 넘기지 않은 일시 해제 보고는 성공하되 알림을 보내지 않는다")
    void reportTemporaryUnlock_belowLimitDoesNotNotify() throws Exception {
        // given
        createFriends(2);

        // when
        authenticated(actor, jsonPost("/notifications/app-unlocks",
                "{\"unlockMinutes\":10,\"limitExceeded\":false}"))
                .andExpect(status().isNoContent());

        // then
        assertNoDeliveryOrHistory();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"unlockMinutes\":10}", "{\"limitExceeded\":true}",
            "{\"unlockMinutes\":null,\"limitExceeded\":true}",
            "{\"unlockMinutes\":10,\"limitExceeded\":null}",
            "{\"unlockMinutes\":0,\"limitExceeded\":true}",
            "{\"unlockMinutes\":-1,\"limitExceeded\":true}",
            "{\"unlockMinutes\":1.5,\"limitExceeded\":true}"
    })
    @DisplayName("해제 분 수와 초과 여부가 없거나 분 수가 양의 정수가 아니면 발송 없이 거부한다")
    void reportTemporaryUnlock_rejectsInvalidReport(String body) throws Exception {
        // given
        createFriends(1);

        // when & then
        authenticated(actor, jsonPost("/notifications/app-unlocks", body))
                .andExpect(status().isBadRequest());
        assertNoDeliveryOrHistory();
    }

    @ParameterizedTest
    @CsvSource({"120, 2시간", "90, 1시간 30분", "30, 30분", "0, 0시간"})
    @DisplayName("실제 목표 시간 변경은 분 정보를 보존하여 활성 친구 최대 세 명에게 보낸다")
    void changeTimeLimit_preservesWholeHoursAndRemainingMinutes(int minutes, String label) throws Exception {
        // given
        registerTimeLimit(60);
        List<User> friends = createFriends(5);
        addIneligibleFriends();

        // when
        authenticated(actor, put("/me/time-limit").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("totalLockMinutes", minutes))))
                .andExpect(status().isOk());

        // then
        assertThat(timeLimitRepository.findByUser_Id(actor.getId())).get()
                .extracting(TimeLimit::getTotalLockMinutes).isEqualTo(minutes);
        assertThat(fcm.messages).hasSize(3).extracting(SentPush::token).doesNotHaveDuplicates()
                .isSubsetOf(friends.stream().map(this::token).toList());
        assertThat(fcm.messages).allSatisfy(message -> {
            assertThat(message.title()).isEqualTo("Detoxmate");
            assertThat(message.body()).isEqualTo("프로필이름님이 목표 제한 시간을 " + label + "으로 변경했어요.");
        });
        assertThat(historyRepository.count()).isZero();
    }

    @Test
    @DisplayName("최초 목표 설정과 동일한 값의 저장은 친구에게 알리지 않는다")
    void setTimeLimit_initialOrUnchangedValueDoesNotNotify() {
        // given
        createFriends(2);

        // when
        timeLimitService.set(actor.getId(), 60);
        timeLimitService.set(actor.getId(), 60);

        // then
        assertThat(timeLimitService.get(actor.getId())).isEqualTo(60);
        assertNoDeliveryOrHistory();
    }

    @Test
    @DisplayName("목표 변경 트랜잭션이 롤백되면 이전 값을 유지하고 푸시하지 않는다")
    void changeTimeLimit_rollbackKeepsPreviousValueWithoutNotification() {
        // given
        registerTimeLimit(60);
        createFriends(2);

        // when
        transactions.executeWithoutResult(status -> {
            timeLimitService.set(actor.getId(), 120);
            assertThat(fcm.messages).isEmpty();
            status.setRollbackOnly();
        });

        // then
        assertThat(timeLimitService.get(actor.getId())).isEqualTo(60);
        assertNoDeliveryOrHistory();
    }

    @Test
    @DisplayName("쉴드 해제 요청은 본인에게 십 초 해제 타이머로 이동하는 푸시만 보낸다")
    void requestShieldUnlock_targetsUnlockTimerWithoutHistory() throws Exception {
        // when
        authenticated(actor, post("/notifications/app-unlock-requests"))
                .andExpect(status().isNoContent());

        // then
        assertSinglePush(actor, "앱을 사용하려면, 이 알림을 클릭해주세요!", "APP_UNLOCK_TIMER");
        assertThat(historyRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 5})
    @DisplayName("등록 해제 미리보기는 활성 친구 최대 세 명의 식별자와 이름만 반환하며 알리지 않는다")
    void previewRemovalRecipients_returnsUpToThreeFriendsWithoutSending(int friendCount) throws Exception {
        // given
        List<User> friends = createFriends(friendCount);
        addIneligibleFriends();

        // when
        JsonNode response = body(authenticated(actor, post("/notifications/app-lock-removal-recipients"))
                .andExpect(status().isOk()));

        // then
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.get("recipients").size()).isEqualTo(Math.min(3, friendCount));
        List<Long> ids = new ArrayList<>();
        for (JsonNode recipient : response.get("recipients")) {
            assertThat(recipient.size()).isEqualTo(2);
            long id = recipient.get("userId").asLong();
            User friend = friends.stream().filter(user -> user.getId().equals(id)).findFirst().orElseThrow();
            assertThat(recipient.get("displayName").asText()).isEqualTo(friend.getDisplayName());
            ids.add(id);
        }
        assertThat(ids).doesNotHaveDuplicates();
        assertNoDeliveryOrHistory();
    }

    @Test
    @DisplayName("등록 해제 확정은 미리보기에서 표시한 친구들에게 재추첨 없이 정확히 보낸다")
    void confirmRemoval_sendsOnlyToPreviewedRecipientIds() throws Exception {
        // given
        List<User> friends = createFriends(5);
        JsonNode preview = body(authenticated(actor, post("/notifications/app-lock-removal-recipients"))
                .andExpect(status().isOk()));
        List<Long> selectedIds = new ArrayList<>();
        preview.get("recipients").forEach(recipient -> selectedIds.add(recipient.get("userId").asLong()));
        assertNoDeliveryOrHistory();

        // when
        authenticated(actor, removalRequest(selectedIds)).andExpect(status().isNoContent());

        // then
        assertThat(fcm.messages).hasSize(3).extracting(SentPush::token)
                .containsExactlyInAnyOrderElementsOf(friends.stream()
                        .filter(friend -> selectedIds.contains(friend.getId())).map(this::token).toList());
        assertThat(fcm.messages).allSatisfy(message -> {
            assertThat(message.title()).isEqualTo("Detoxmate");
            assertThat(message.body()).isEqualTo("프로필이름님이 잠근 앱을 등록 해제했어요.");
        });
        assertThat(historyRepository.count()).isZero();
    }

    @Test
    @DisplayName("빈 등록 해제 수신자 목록은 친구가 없는 사용자의 정상 요청이다")
    void confirmRemoval_acceptsEmptyRecipientsWithoutSending() throws Exception {
        // when
        authenticated(actor, removalRequest(List.of())).andExpect(status().isNoContent());

        // then
        assertNoDeliveryOrHistory();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"recipientUserIds\":null}", "{\"recipientUserIds\":[null]}"})
    @DisplayName("등록 해제 수신자 목록이나 항목이 null이면 발송 없이 거부한다")
    void confirmRemoval_rejectsNullRecipients(String requestBody) throws Exception {
        // when & then
        authenticated(actor, jsonPost("/notifications/app-lock-removals", requestBody))
                .andExpect(status().isBadRequest());
        assertNoDeliveryOrHistory();
    }

    @Test
    @DisplayName("중복되거나 네 명인 등록 해제 수신자 목록은 발송 없이 거부한다")
    void confirmRemoval_rejectsDuplicateOrTooManyRecipients() throws Exception {
        // given
        List<Long> ids = createFriends(4).stream().map(User::getId).toList();

        // when & then
        authenticated(actor, removalRequest(List.of(ids.getFirst(), ids.getFirst())))
                .andExpect(status().isBadRequest());
        authenticated(actor, removalRequest(ids)).andExpect(status().isBadRequest());
        assertNoDeliveryOrHistory();
    }

    @Test
    @DisplayName("등록 해제 수신자 중 비친구가 있으면 유효한 친구에게도 부분 발송하지 않는다")
    void confirmRemoval_rejectsEntireSelectionContainingNonfriend() throws Exception {
        // given
        User friend = createFriends(1).getFirst();
        User outsider = createUser("다른사람");

        // when & then
        authenticated(actor, removalRequest(List.of(friend.getId(), outsider.getId())))
                .andExpect(status().isForbidden());
        assertNoDeliveryOrHistory();
    }

    @Test
    @DisplayName("미리보기 이후 친구 해제된 수신자는 확정 시 다시 검증하여 발송하지 않는다")
    void confirmRemoval_revalidatesFriendshipAtConfirmation() throws Exception {
        // given
        User friend = createFriends(1).getFirst();
        authenticated(actor, post("/notifications/app-lock-removal-recipients")).andExpect(status().isOk());
        friendRepository.deleteAllInBatch();

        // when & then
        authenticated(actor, removalRequest(List.of(friend.getId()))).andExpect(status().isForbidden());
        assertNoDeliveryOrHistory();
    }

    @Test
    @DisplayName("탈퇴한 친구나 본인은 등록 해제 수신자로 사용할 수 없다")
    void confirmRemoval_rejectsWithdrawnFriendAndSelf() throws Exception {
        // given
        User friend = createFriends(1).getFirst();
        friend.withdraw();
        userRepository.saveAndFlush(friend);

        // when & then
        authenticated(actor, removalRequest(List.of(friend.getId()))).andExpect(status().isForbidden());
        authenticated(actor, removalRequest(List.of(actor.getId()))).andExpect(status().isForbidden());
        assertNoDeliveryOrHistory();
    }

    @Test
    @DisplayName("시간 제한 설정 삭제나 친구 해제는 기기 등록 해제 알림을 발송하지 않는다")
    void deleteTimeLimitOrFriendship_doesNotTriggerAppRegistrationRemoval() throws Exception {
        // given
        registerTimeLimit(60);
        User friend = createFriends(1).getFirst();
        Friend friendship = friendRepository.findByUserPair(actor.getId(), friend.getId()).orElseThrow();

        // when
        authenticated(actor, delete("/me/time-limit")).andExpect(status().isNoContent());
        authenticated(actor, delete("/friends/{friendshipId}", friendship.getId())).andExpect(status().isNoContent());

        // then
        assertThat(timeLimitRepository.findByUser_Id(actor.getId())).isEmpty();
        assertThat(friendRepository.findById(friendship.getId())).isEmpty();
        assertNoDeliveryOrHistory();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/notifications/app-unlocks", "/notifications/app-unlock-requests",
            "/notifications/app-relock-reminders", "/notifications/app-lock-removal-recipients",
            "/notifications/app-lock-removals"})
    @DisplayName("알림 요청은 인증 없이는 발송이나 수신자 노출 없이 거부한다")
    void notificationRequests_requireAuthentication(String path) throws Exception {
        // when & then
        mockMvc.perform(jsonPost(path, "{}")).andExpect(status().isUnauthorized());
        assertNoDeliveryOrHistory();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/notifications/app-unlocks", "/notifications/app-unlock-requests",
            "/notifications/app-relock-reminders", "/notifications/app-lock-removal-recipients",
            "/notifications/app-lock-removals"})
    @DisplayName("탈퇴 사용자의 알림 요청은 발송이나 수신자 노출 없이 거부한다")
    void notificationRequests_rejectWithdrawnActor(String path) throws Exception {
        // given
        actor.withdraw();
        actor = userRepository.saveAndFlush(actor);

        // when & then
        authenticated(actor, jsonPost(path, "{}")).andExpect(status().isUnauthorized());
        assertNoDeliveryOrHistory();
    }

    @Test
    @DisplayName("알림을 끈 사용자의 재잠금 요청은 성공하되 푸시하지 않는다")
    void requestRelockReminder_respectsPushPreference() throws Exception {
        // given
        actor.updatePushNotificationEnabled(false);
        actor = userRepository.saveAndFlush(actor);

        // when
        authenticated(actor, post("/notifications/app-relock-reminders")).andExpect(status().isNoContent());

        // then
        assertNoDeliveryOrHistory();
    }

    @Test
    @DisplayName("기존 목표 설정 이벤트는 스케줄러 제거 후에도 이력과 Detoxmate 제목을 보낸다")
    void legacyGoalReminder_stillDeliversOnCommittedEvent() {
        // when
        transactions.executeWithoutResult(status ->
                events.publishEvent(new GoalSettingReminderEvent(10L, 20L, actor.getId())));

        // then
        assertSinglePush(actor, "프로필이름님의 목표 설정을 멤버들이 기다리고 있어요. 목표 설정하러 가볼까요?", "FEED");
        assertThat(historyRepository.findAll()).singleElement()
                .satisfies(history -> assertThat(history.getUserId()).isEqualTo(actor.getId()));
    }

    @Test
    @DisplayName("기존 이벤트를 포함한 모든 알림 템플릿의 제목은 Detoxmate다")
    void notificationTemplates_useSharedAppTitle() {
        // then
        assertThat(notificationRepository.findAll()).isNotEmpty()
                .allSatisfy(template -> assertThat(template.getTitle()).isEqualTo("Detoxmate"));
    }

    @Test
    @DisplayName("정기 푸시 알림 작업은 스케줄러에 등록하지 않는다")
    void scheduledTasks_doNotRegisterNotificationJobs() {
        // then
        // The four legacy notification jobs are the application's only scheduled jobs.
        assertThat(scheduledTasks.getScheduledTasks()).isEmpty();
    }

    private void assertFriendHistory(User receiver, User sender, String target) throws Exception {
        NotificationHistory history = historyRepository.findAll().getFirst();
        assertThat(historyRepository.count()).isEqualTo(1);
        assertThat(history.getUserId()).isEqualTo(receiver.getId());
        assertThat(history.getSenderUserId()).isEqualTo(sender.getId());
        assertThat(history.getTitle()).isEqualTo("Detoxmate");
        assertThat(history.getMessage()).isEqualTo(fcm.messages.getFirst().body());
        assertThat(history.getTargetType().name()).isEqualTo(target);
        authenticated(receiver, get("/notifications/{id}/navigation", history.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.navigable").value(true))
                .andExpect(jsonPath("$.targetType").value(target))
                .andExpect(jsonPath("$.targetId").isEmpty());
    }

    private void assertSinglePush(User recipient, String body, String target) {
        assertThat(fcm.messages).singleElement().satisfies(message -> {
            assertThat(message.token()).isEqualTo(token(recipient));
            assertThat(message.title()).isEqualTo("Detoxmate");
            assertThat(message.body()).isEqualTo(body);
            assertThat(message.data()).containsEntry("targetType", target);
        });
    }

    private void assertNoDeliveryOrHistory() {
        assertThat(fcm.messages).isEmpty();
        assertThat(historyRepository.count()).isZero();
    }

    private List<User> createFriends(int count) {
        List<User> friends = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            User friend = createUser("친구" + index);
            acceptFriendship(index % 2 == 0 ? actor : friend, index % 2 == 0 ? friend : actor);
            friends.add(friend);
        }
        return friends;
    }

    private void addIneligibleFriends() {
        User pending = createUser("대기친구");
        friendRepository.saveAndFlush(Friend.request(actor.getId(), pending.getId()));
        User withdrawn = createUser("탈퇴친구");
        acceptFriendship(actor, withdrawn);
        withdrawn.withdraw();
        userRepository.saveAndFlush(withdrawn);
    }

    private void acceptFriendship(User from, User to) {
        Friend pending = friendRepository.saveAndFlush(Friend.request(from.getId(), to.getId()));
        transactions.executeWithoutResult(status ->
                friendRepository.acceptPendingRequest(pending.getId(), to.getId(), LocalDateTime.now()));
    }

    private void registerTimeLimit(int minutes) {
        timeLimitRepository.saveAndFlush(TimeLimit.create(actor, minutes));
    }

    private MockHttpServletRequestBuilder jsonPost(String path, String body) {
        return post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder removalRequest(List<Long> ids) throws Exception {
        return jsonPost("/notifications/app-lock-removals", json.writeValueAsString(Map.of("recipientUserIds", ids)));
    }

    private JsonNode body(ResultActions response) throws Exception {
        return json.readTree(response.andReturn().getResponse().getContentAsString());
    }

    private User createUser(String name) {
        User user = userRepository.saveAndFlush(UserFixtures.createUser(name));
        tokenRepository.saveAndFlush(FcmToken.create(user.getId(), token(user), DevicePlatform.IOS));
        return user;
    }

    private String token(User user) {
        return "notification-policy-token-" + user.getId();
    }

    private ResultActions authenticated(User user, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtTokenProvider.createAccessToken(user.getId())));
    }

    record SentPush(String token, String title, String body, Map<String, String> data) { }

    static class RecordingFcmSender implements FcmSender {
        final List<SentPush> messages = new CopyOnWriteArrayList<>();

        @Override
        public void send(String token, String title, String body, Map<String, String> data) {
            messages.add(new SentPush(token, title, body, Map.copyOf(data)));
        }
    }

    @TestConfiguration
    static class DeliveryTestConfiguration {
        @Bean
        @Primary
        RecordingFcmSender recordingFcmSender() {
            return new RecordingFcmSender();
        }

        @Bean(name = "notificationTaskExecutor")
        Executor notificationTaskExecutor() {
            return command -> {
                // Preserve the production thread/transaction boundary, but wait deterministically.
                FutureTask<Void> delivery = new FutureTask<>(command, null);
                Thread.ofPlatform().name("notification-policy-test").start(delivery);
                try {
                    delivery.get(10, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Notification delivery interrupted", exception);
                } catch (Exception exception) {
                    throw new IllegalStateException("Notification delivery failed", exception);
                }
            };
        }
    }
}
