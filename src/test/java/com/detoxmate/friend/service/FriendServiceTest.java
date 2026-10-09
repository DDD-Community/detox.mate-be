package com.detoxmate.friend.service;

import com.detoxmate.support.UserFixtures;
import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.user.UserCodeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.detoxmate.friend.domain.Friend;
import com.detoxmate.friend.domain.FriendStatus;
import com.detoxmate.friend.dto.FriendRelationshipStatus;
import com.detoxmate.friend.dto.FriendRequestResponse;
import com.detoxmate.friend.dto.FriendResponse;
import com.detoxmate.friend.dto.FriendSearchResponse;
import com.detoxmate.friend.dto.FriendListUserResponse;
import com.detoxmate.friend.repository.FriendInviteRepository;
import com.detoxmate.friend.repository.FriendRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@org.springframework.context.annotation.Import(FriendServiceTest.FixedClockConfig.class)
class FriendServiceTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 9, 14, 18, 0);

    @Autowired
    private FriendService friendService;

    @Autowired
    private FriendInviteRepository friendInviteRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("내 초대코드를 처음 조회하면 생성하고 다시 조회해도 같은 코드를 반환한다")
    void getMyInvite_returnsOneStableInviteCode() {
        User me = saveUser("나", "me@example.com");

        String firstCode = friendService.getMyInvite(me.getId()).code();
        String secondCode = friendService.getMyInvite(me.getId()).code();

        assertThat(firstCode)
                .hasSize(64)
                .isEqualTo(secondCode);
        assertThat(friendInviteRepository.findByUserId(me.getId()))
                .get()
                .extracting(invite -> invite.getCode())
                .isEqualTo(firstCode);
    }

    @Test
    @DisplayName("사용자 코드로 검색하면 일치하는 활성 사용자의 공개 정보와 관계 상태를 반환한다")
    void searchByUserCode_returnsTheMatchingUserWithoutExposingEmail() {
        User me = saveUser("나", "me@example.com");
        User target = saveUser("친구", "friend@example.com", "profile-images/friend/profile.png");

        FriendSearchResponse response = friendService.searchByUserCode("  " + target.getUserCode().toLowerCase(Locale.ROOT) + " ", me.getId());

        assertThat(response).isEqualTo(new FriendSearchResponse(
                target.getId(),
                "친구",
                "https://example.com/media/profile-images/friend/profile.png",
                FriendRelationshipStatus.NONE,
                null,
                0,
                null
        ));
    }

    @Test
    @DisplayName("대기 중인 요청의 상대가 탈퇴하면 요청 목록에서 식별 정보를 노출하지 않는다")
    void getSentRequests_hidesWithdrawnRequestTarget() {
        User sender = saveUser("보낸 사람", "sender@example.com");
        User receiver = saveUser("받는 사람", "receiver@example.com");
        friendService.sendRequest(sender.getId(), receiver.getId());

        receiver.withdraw();
        userRepository.saveAndFlush(receiver);

        assertThat(friendService.getSentRequests(sender.getId())).isEmpty();
    }

    @Test
    @DisplayName("이미 대기 중인 친구 요청이 반대 방향으로 존재하면 새 요청을 거부한다")
    void sendRequest_rejectsTheOppositePendingDirection() {
        User sender = saveUser("보낸 사람", "sender@example.com");
        User receiver = saveUser("받는 사람", "receiver@example.com");
        FriendRequestResponse originalRequest = friendService.sendRequest(sender.getId(), receiver.getId());

        assertThatThrownBy(() -> friendService.sendRequest(receiver.getId(), sender.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(409);
        assertThat(friendRepository.findById(originalRequest.requestId()))
                .get()
                .extracting(Friend::getStatus)
                .isEqualTo(FriendStatus.PENDING);
    }

    @Test
    @DisplayName("받은 친구 요청을 수락하면 동일 관계가 친구 상태로 전환된다")
    void acceptRequest_createsAnAcceptedFriendship() {
        User sender = saveUser("보낸 사람", "sender@example.com");
        User receiver = saveUser("받는 사람", "receiver@example.com");
        FriendRequestResponse request = friendService.sendRequest(sender.getId(), receiver.getId());

        FriendResponse response = friendService.acceptRequest(request.requestId(), receiver.getId());

        assertThat(response.friendshipId()).isEqualTo(request.requestId());
        assertThat(response.user()).isEqualTo(new FriendListUserResponse(
                sender.getId(),
                "보낸 사람",
                null,
                FriendRelationshipStatus.FRIEND,
                null,
                sender.getUserCode()
        ));
        assertThat(response.acceptedAt()).isEqualTo(FIXED_NOW);
        assertThat(friendRepository.findById(request.requestId()))
                .get()
                .extracting(Friend::getStatus)
                .isEqualTo(FriendStatus.ACCEPTED);
    }

    @Test
    @DisplayName("친구 요청을 보낸 사용자는 대기 중인 요청을 취소할 수 없다")
    void deletePendingRequest_rejectsSenderCancellation() {
        User sender = saveUser("보낸 사람", "sender@example.com");
        User receiver = saveUser("받는 사람", "receiver@example.com");
        FriendRequestResponse request = friendService.sendRequest(sender.getId(), receiver.getId());

        assertStatus(403, () -> friendService.deletePendingRequest(request.requestId(), sender.getId()));

        assertThat(friendRepository.findById(request.requestId())).isPresent();
    }

    @Test
    @DisplayName("친구 요청을 받은 사용자는 대기 중인 요청을 거절할 수 있다")
    void deletePendingRequest_allowsReceiverRejection() {
        User sender = saveUser("보낸 사람", "sender@example.com");
        User receiver = saveUser("받는 사람", "receiver@example.com");
        FriendRequestResponse request = friendService.sendRequest(sender.getId(), receiver.getId());

        friendService.deletePendingRequest(request.requestId(), receiver.getId());

        assertThat(friendRepository.findById(request.requestId())).isEmpty();
    }

    @Test
    @DisplayName("친구 관계를 끊으면 친구 목록에서 해당 관계가 사라진다")
    void unfriend_removesAcceptedRelationship() {
        User sender = saveUser("보낸 사람", "sender@example.com");
        User receiver = saveUser("받는 사람", "receiver@example.com");
        FriendRequestResponse request = friendService.sendRequest(sender.getId(), receiver.getId());
        friendService.acceptRequest(request.requestId(), receiver.getId());

        friendService.unfriend(request.requestId(), sender.getId());

        assertThat(friendService.getFriends(sender.getId())).isEmpty();
        assertThat(friendRepository.findById(request.requestId())).isEmpty();
    }

    @Test
    void getMyInvite_includesOwnUserCode() {
        User me = saveUser("나", "me@example.com");
        assertThat(json(friendService.getMyInvite(me.getId())).path("userCode").asText())
                .isEqualTo(me.getUserCode());
    }

    @Test
    void getInvitee_includesPublicSummaryWithoutEmail() {
        User me = saveUser("나", "me@example.com");
        User target = saveUser("친구", "friend@example.com");
        String code = friendService.getMyInvite(target.getId()).code();
        JsonNode response = json(friendService.getInvitee(code, me.getId()));
        assertThat(response.has("daysSinceStart")).isTrue();
        assertThat(response.path("targetSuccessCount").asLong(-1)).isZero();
        assertThat(response.has("email")).isFalse();
    }

    @Test
    void receivedAndFriends_includeCounterpartUserCode() {
        User sender = saveUser("보낸 사람", "sender@example.com");
        User receiver = saveUser("받는 사람", "receiver@example.com");
        FriendRequestResponse request = friendService.sendRequest(sender.getId(), receiver.getId());
        assertThat(json(friendService.getReceivedRequests(receiver.getId()).getFirst()).path("user").path("userCode").asText())
                .isEqualTo(sender.getUserCode());
        friendService.acceptRequest(request.requestId(), receiver.getId());
        assertThat(json(friendService.getFriends(sender.getId()).getFirst()).path("user").path("userCode").asText())
                .isEqualTo(receiver.getUserCode());
    }

    @Test
    void searchByUserCode_includesMutualSummary() {
        User me = saveUser("나", "me@example.com");
        User target = saveUser("친구", "friend@example.com");
        User common = saveUser("공통 친구", "common@example.com");
        accept(me, common);
        accept(common, target);
        JsonNode response = json(friendService.searchByUserCode(target.getUserCode(), me.getId()));
        assertThat(response.path("mutualFriendCount").asLong(-1)).isEqualTo(1);
        assertThat(response.path("mutualFriendPreviewName").asText()).isEqualTo("공통 친구");
        assertThat(response.has("email")).isFalse();
    }

    @Test
    void relationshipFlow_requiresExplicitRecipientAcceptAndUnfriendsBothSides() {
        User sender = saveUser("보낸 사람", "sender@example.com");
        User receiver = saveUser("받는 사람", "receiver@example.com");
        User thirdParty = saveUser("제삼자", "third@example.com");
        String code = friendService.getMyInvite(receiver.getId()).code();
        assertThat(friendService.getInvitee(code, sender.getId()).relationshipStatus()).isEqualTo(FriendRelationshipStatus.NONE);
        friendService.searchByUserCode(receiver.getUserCode(), sender.getId());
        assertThat(friendRepository.findByUserPair(sender.getId(), receiver.getId())).isEmpty();
        assertStatus(400, () -> friendService.sendRequest(sender.getId(), sender.getId()));
        FriendRequestResponse request = friendService.sendRequest(sender.getId(), receiver.getId());
        assertThat(json(request).path("user").has("email")).isFalse();
        assertThat(friendService.getFriends(sender.getId())).isEmpty();
        assertThat(friendService.getFriends(receiver.getId())).isEmpty();
        assertThat(friendService.searchByUserCode(receiver.getUserCode(), sender.getId()).relationshipStatus())
                .isEqualTo(FriendRelationshipStatus.PENDING_SENT);
        assertThat(friendService.searchByUserCode(sender.getUserCode(), receiver.getId()).relationshipStatus())
                .isEqualTo(FriendRelationshipStatus.PENDING_RECEIVED);
        assertThat(friendService.getInvitee(code, sender.getId()).relationshipStatus()).isEqualTo(FriendRelationshipStatus.PENDING_SENT);
        assertStatus(409, () -> friendService.sendRequest(sender.getId(), receiver.getId()));
        assertStatus(403, () -> friendService.acceptRequest(request.requestId(), sender.getId()));
        assertStatus(403, () -> friendService.acceptRequest(request.requestId(), thirdParty.getId()));
        assertStatus(403, () -> friendService.deletePendingRequest(request.requestId(), thirdParty.getId()));
        assertStatus(409, () -> friendService.unfriend(request.requestId(), sender.getId()));
        friendService.acceptRequest(request.requestId(), receiver.getId());
        assertThat(friendService.getFriends(sender.getId())).hasSize(1);
        assertThat(friendService.getFriends(receiver.getId())).hasSize(1);
        assertThat(friendService.getReceivedRequests(receiver.getId())).isEmpty();
        assertThat(friendService.getSentRequests(sender.getId())).isEmpty();
        assertStatus(409, () -> friendService.deletePendingRequest(request.requestId(), receiver.getId()));
        assertStatus(409, () -> friendService.acceptRequest(request.requestId(), receiver.getId()));
        assertStatus(409, () -> friendService.sendRequest(receiver.getId(), sender.getId()));
        assertStatus(403, () -> friendService.unfriend(request.requestId(), thirdParty.getId()));
        friendService.unfriend(request.requestId(), receiver.getId());
        assertThat(friendService.getFriends(sender.getId())).isEmpty();
        assertThat(friendService.getFriends(receiver.getId())).isEmpty();
        assertThat(friendService.searchByUserCode(receiver.getUserCode(), sender.getId()).relationshipStatus())
                .isEqualTo(FriendRelationshipStatus.NONE);
        FriendRequestResponse newRequest = friendService.sendRequest(sender.getId(), receiver.getId());
        assertThat(newRequest.requestId()).isNotEqualTo(request.requestId());
        assertStatus(404, () -> friendService.deletePendingRequest(request.requestId(), receiver.getId()));
        assertThat(friendRepository.findById(newRequest.requestId())).isPresent();
    }

    @Test
    void rejectionThenReRequest_doesNotAllowOldRequestIdToActOnNewRequest() {
        User sender = saveUser("보낸 사람", "sender@example.com");
        User receiver = saveUser("받는 사람", "receiver@example.com");
        FriendRequestResponse previous = friendService.sendRequest(sender.getId(), receiver.getId());
        friendService.deletePendingRequest(previous.requestId(), receiver.getId());
        FriendRequestResponse current = friendService.sendRequest(receiver.getId(), sender.getId());
        assertStatus(404, () -> friendService.acceptRequest(previous.requestId(), receiver.getId()));
        assertStatus(404, () -> friendService.deletePendingRequest(previous.requestId(), receiver.getId()));
        assertThat(friendRepository.findById(current.requestId())).get().extracting(Friend::getStatus)
                .isEqualTo(FriendStatus.PENDING);
    }

    @Test
    void searchByUserCode_countsAcceptedDistinctActiveMutualFriendsWithStablePreview() {
        User me = saveUser("나", "me@example.com");
        User target = saveUser("상대", "target@example.com");
        User earliest = saveUser("첫 공통 친구", "first@example.com");
        User later = saveUser("두 번째 공통 친구", "later@example.com");
        User pending = saveUser("요청 대기", "pending@example.com");
        User withdrawn = saveUser("탈퇴", "withdrawn@example.com");
        User oneSided = saveUser("한쪽만", "one@example.com");
        accept(earliest, me);
        accept(target, earliest);
        accept(me, later);
        accept(later, target);
        accept(me, pending);
        friendService.sendRequest(target.getId(), pending.getId());
        accept(withdrawn, me);
        accept(target, withdrawn);
        accept(me, oneSided);
        accept(me, target);
        withdrawn.withdraw();
        userRepository.saveAndFlush(withdrawn);
        var response = friendService.searchByUserCode(target.getUserCode(), me.getId());
        assertThat(response.mutualFriendCount()).isEqualTo(2);
        assertThat(response.mutualFriendPreviewName()).isEqualTo("첫 공통 친구");
        assertThat(response.relationshipStatus()).isEqualTo(FriendRelationshipStatus.FRIEND);
        var reverse = friendService.searchByUserCode(me.getUserCode(), target.getId());
        assertThat(reverse.mutualFriendCount()).isEqualTo(2);
        assertThat(reverse.mutualFriendPreviewName()).isEqualTo(response.mutualFriendPreviewName());
        var self = friendService.searchByUserCode(me.getUserCode(), me.getId());
        assertThat(self.relationshipStatus()).isEqualTo(FriendRelationshipStatus.SELF);
        assertThat(self.mutualFriendCount()).isZero();
        assertThat(self.mutualFriendPreviewName()).isNull();
    }

    @Test
    void searchByUserCode_rejectsInvalidMissingAndWithdrawnTargets() {
        User me = saveUser("나", "me@example.com");
        User target = saveUser("탈퇴", "target@example.com");
        target.withdraw();
        userRepository.saveAndFlush(target);
        assertThatThrownBy(() -> friendService.searchByUserCode("invalid", me.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserCodeException.INVALID_USER_CODE);
        assertStatus(404, () -> friendService.searchByUserCode("ZZZZZ", me.getId()));
        assertStatus(404, () -> friendService.searchByUserCode(target.getUserCode(), me.getId()));
    }

    @Test
    void lists_preserveWithdrawnFriendRedactionAndHideWithdrawnPendingSender() {
        User me = saveUser("나", "me@example.com");
        User friend = saveUser("친구", "friend@example.com", "friend.png");
        User pendingSender = saveUser("대기", "pending@example.com");
        accept(me, friend);
        friendService.sendRequest(pendingSender.getId(), me.getId());
        friend.withdraw();
        pendingSender.withdraw();
        userRepository.saveAndFlush(friend);
        userRepository.saveAndFlush(pendingSender);
        assertThat(friendService.getReceivedRequests(me.getId())).isEmpty();
        var redacted = friendService.getFriends(me.getId()).getFirst().user();
        assertThat(redacted.displayName()).isEqualTo(User.WITHDRAWN_DISPLAY_NAME);
        assertThat(redacted.profileImageUrl()).isNull();
        assertThat(redacted.userCode()).isEqualTo(friend.getUserCode());
    }

    @Test
    void usersWithoutEmail_returnUserCodeAndEmptyListsRemainEmpty() {
        User me = saveUser("나", null);
        User target = saveUser("상대", null);
        assertThat(friendService.getMyInvite(me.getId()).userCode()).isEqualTo(me.getUserCode());
        assertThat(friendService.getFriends(me.getId())).isEmpty();
        assertThat(friendService.getReceivedRequests(me.getId())).isEmpty();
        FriendRequestResponse request = friendService.sendRequest(target.getId(), me.getId());
        assertThat(friendService.getReceivedRequests(me.getId()).getFirst().user().userCode()).isEqualTo(target.getUserCode());
        friendService.acceptRequest(request.requestId(), me.getId());
        assertThat(friendService.getFriends(me.getId()).getFirst().user().userCode()).isEqualTo(target.getUserCode());
    }

    private void accept(User sender, User receiver) {
        FriendRequestResponse request = friendService.sendRequest(sender.getId(), receiver.getId());
        friendService.acceptRequest(request.requestId(), receiver.getId());
    }

    private JsonNode json(Object response) {
        return JsonMapper.builder().findAndAddModules().build().valueToTree(response);
    }

    private void assertStatus(int expected, Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
                .isEqualTo(expected);
    }

    private User saveUser(String displayName, String email) {
        return saveUser(displayName, email, null);
    }

    private User saveUser(String displayName, String email, String profileImageObjectKey) {
        return userRepository.saveAndFlush(UserFixtures.createUser(displayName, profileImageObjectKey, email));
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    Instant.parse("2026-09-14T09:00:00Z"),
                    ZoneId.of("Asia/Seoul")
            );
        }
    }
}
