package com.detoxmate.notification.listener;

import com.detoxmate.notification.domain.NotificationTargetType;
import com.detoxmate.notification.domain.NotificationTypeCode;
import com.detoxmate.notification.dto.ChallengeRecordNotificationRow;
import com.detoxmate.notification.event.CertificationCreatedEvent;
import com.detoxmate.notification.event.CertificationStartTomorrowEvent;
import com.detoxmate.notification.event.CommentCreatedEvent;
import com.detoxmate.notification.event.GoalSettingReminderEvent;
import com.detoxmate.notification.event.GroupJoinedEvent;
import com.detoxmate.notification.event.PokeCreatedEvent;
import com.detoxmate.notification.event.PokeGoalSettingReminderEvent;
import com.detoxmate.notification.event.ReactionCreatedEvent;
import com.detoxmate.notification.service.NotificationCommand;
import com.detoxmate.notification.service.NotificationService;
import com.detoxmate.notification.util.NotificationCommentReader;
import com.detoxmate.notification.util.NotificationGroupReader;
import com.detoxmate.notification.util.NotificationRecipientReader;
import com.detoxmate.notification.util.NotificationUserReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRecipientReader recipientReader;

    @Mock
    private NotificationUserReader userReader;

    @Mock
    private NotificationCommentReader commentReader;

    @Mock
    private NotificationGroupReader groupReader;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    @DisplayName("그룹 합류 이벤트는 합류자를 제외한 그룹 멤버에게 합류 알림 커맨드를 만든다")
    void groupJoinedEvent_createsCommandsForGroupMembers() {
        // given
        Long groupId = 10L;
        Long joinedUserId = 1L;
        when(userReader.findDisplayName(joinedUserId)).thenReturn("슬빈");
        when(groupReader.findGroupName(groupId)).thenReturn("디톡스방");
        when(recipientReader.findActiveGroupMemberUserIds(groupId)).thenReturn(List.of(1L, 2L));

        // when
        listener.on(new GroupJoinedEvent(groupId, joinedUserId));

        // then
        List<NotificationCommand> commands = captureCommands(1);
        assertThat(commands)
                .extracting(NotificationCommand::recipientUserId)
                .containsExactly(2L);
        assertThat(commands).allSatisfy(command -> {
            assertThat(command.typeCode()).isEqualTo(NotificationTypeCode.GROUP_JOINED);
            assertThat(command.context().get("nickname")).isEqualTo("슬빈");
            assertThat(command.context().get("groupName")).isEqualTo("디톡스방");
            assertThat(command.payload().targetType()).isEqualTo(NotificationTargetType.GROUP);
            assertThat(command.payload().targetId()).isEqualTo(groupId);
            assertThat(command.saveHistory()).isTrue();
        });
    }

    @Test
    @DisplayName("인증 생성 이벤트는 인증자를 제외한 챌린지 참여자에게 피드 알림 커맨드를 만든다")
    void certificationCreatedEvent_createsCommandsForChallengeParticipants() {
        // given
        Long challengeRecordId = 100L;
        Long groupChallengeId = 20L;
        Long actorUserId = 1L;
        when(recipientReader.findChallengeRecordInfo(challengeRecordId))
                .thenReturn(new ChallengeRecordNotificationRow(challengeRecordId, groupChallengeId, actorUserId, "슬빈"));
        when(userReader.findDisplayName(actorUserId)).thenReturn("슬빈");
        when(recipientReader.findGroupChallengeParticipantUserIds(groupChallengeId)).thenReturn(List.of(1L, 2L, 3L));

        // when
        listener.on(new CertificationCreatedEvent(challengeRecordId, actorUserId));

        // then
        List<NotificationCommand> commands = captureCommands(2);
        assertThat(commands)
                .extracting(NotificationCommand::recipientUserId)
                .containsExactly(2L, 3L);
        assertThat(commands).allSatisfy(command -> {
            assertThat(command.typeCode()).isEqualTo(NotificationTypeCode.CERTIFICATION_CREATED);
            assertThat(command.context().get("nickname")).isEqualTo("슬빈");
            assertThat(command.payload().targetType()).isEqualTo(NotificationTargetType.FEED);
            assertThat(command.payload().targetId()).isEqualTo(groupChallengeId);
            assertThat(command.saveHistory()).isTrue();
        });
    }

    @Test
    @DisplayName("콕 찌르기 이벤트는 수신자에게 피드 알림 커맨드를 만든다")
    void pokeCreatedEvent_createsCommandForReceiver() {
        // given
        Long challengeRecordId = 100L;
        Long groupChallengeId = 20L;
        Long senderUserId = 1L;
        Long receiverUserId = 2L;
        when(recipientReader.findChallengeRecordInfo(challengeRecordId))
                .thenReturn(new ChallengeRecordNotificationRow(challengeRecordId, groupChallengeId, 3L, "작성자"));
        when(userReader.findDisplayNames(Set.of(senderUserId, receiverUserId)))
                .thenReturn(Map.of(senderUserId, "슬빈", receiverUserId, "지민"));

        // when
        listener.on(new PokeCreatedEvent(challengeRecordId, senderUserId, receiverUserId));

        // then
        NotificationCommand command = captureCommand();
        assertThat(command.recipientUserId()).isEqualTo(receiverUserId);
        assertThat(command.typeCode()).isEqualTo(NotificationTypeCode.POKE_RECEIVED);
        assertThat(command.context().get("nickname")).isEqualTo("슬빈");
        assertThat(command.context().get("me")).isEqualTo("지민");
        assertThat(command.payload().targetType()).isEqualTo(NotificationTargetType.FEED);
        assertThat(command.payload().targetId()).isEqualTo(groupChallengeId);
        assertThat(command.saveHistory()).isTrue();
    }

    @Test
    @DisplayName("반응 이벤트는 게시물 작성자에게 피드 상세 알림 커맨드를 만든다")
    void reactionCreatedEvent_createsCommandForRecordAuthor() {
        // given
        Long challengeRecordId = 100L;
        Long groupChallengeId = 20L;
        Long authorUserId = 2L;
        Long reactorUserId = 1L;
        when(recipientReader.findChallengeRecordInfo(challengeRecordId))
                .thenReturn(new ChallengeRecordNotificationRow(challengeRecordId, groupChallengeId, authorUserId, "작성자"));
        when(userReader.findDisplayName(reactorUserId)).thenReturn("슬빈");

        // when
        listener.on(new ReactionCreatedEvent(challengeRecordId, reactorUserId));

        // then
        NotificationCommand command = captureCommand();
        assertThat(command.recipientUserId()).isEqualTo(authorUserId);
        assertThat(command.typeCode()).isEqualTo(NotificationTypeCode.REACTION_CREATED);
        assertThat(command.context().get("nickname")).isEqualTo("슬빈");
        assertThat(command.context().get("me")).isEqualTo("작성자");
        assertThat(command.payload().targetType()).isEqualTo(NotificationTargetType.FEED_DETAIL);
        assertThat(command.payload().targetId()).isEqualTo(challengeRecordId);
        assertThat(command.saveHistory()).isTrue();
    }

    @Test
    @DisplayName("작성자가 자신의 게시물에 반응하면 알림 커맨드를 만들지 않는다")
    void reactionCreatedEvent_skipsCommandWhenReactorIsRecordAuthor() {
        // given
        Long challengeRecordId = 100L;
        Long groupChallengeId = 20L;
        Long authorUserId = 2L;
        when(recipientReader.findChallengeRecordInfo(challengeRecordId))
                .thenReturn(new ChallengeRecordNotificationRow(challengeRecordId, groupChallengeId, authorUserId, "작성자"));

        // when
        listener.on(new ReactionCreatedEvent(challengeRecordId, authorUserId));

        // then
        verify(notificationService, never()).send(any(NotificationCommand.class));
    }

    @Test
    @DisplayName("댓글 이벤트는 게시물 작성자에게 댓글 본문을 60자로 줄인 피드 상세 알림 커맨드를 만든다")
    void commentCreatedEvent_createsCommandWithTruncatedCommentBody() {
        // given
        Long challengeRecordId = 100L;
        Long groupChallengeId = 20L;
        Long authorUserId = 2L;
        Long commenterUserId = 1L;
        Long commentId = 30L;
        String commentBody = "1234567890".repeat(7);
        when(recipientReader.findChallengeRecordInfo(challengeRecordId))
                .thenReturn(new ChallengeRecordNotificationRow(challengeRecordId, groupChallengeId, authorUserId, "작성자"));
        when(userReader.findDisplayName(commenterUserId)).thenReturn("슬빈");
        when(commentReader.findCommentBody(commentId)).thenReturn(commentBody);

        // when
        listener.on(new CommentCreatedEvent(challengeRecordId, commenterUserId, commentId));

        // then
        NotificationCommand command = captureCommand();
        assertThat(command.recipientUserId()).isEqualTo(authorUserId);
        assertThat(command.typeCode()).isEqualTo(NotificationTypeCode.COMMENT_CREATED);
        assertThat(command.context().get("nickname")).isEqualTo("슬빈");
        assertThat(command.context().get("commentBody")).isEqualTo(commentBody.substring(0, 60) + "...");
        assertThat(command.payload().targetType()).isEqualTo(NotificationTargetType.FEED_DETAIL);
        assertThat(command.payload().targetId()).isEqualTo(challengeRecordId);
        assertThat(command.saveHistory()).isTrue();
    }

    @Test
    @DisplayName("작성자가 자신의 게시물에 댓글을 달면 알림 커맨드를 만들지 않는다")
    void commentCreatedEvent_skipsCommandWhenCommenterIsRecordAuthor() {
        // given
        Long challengeRecordId = 100L;
        Long groupChallengeId = 20L;
        Long authorUserId = 2L;
        Long commentId = 30L;
        when(recipientReader.findChallengeRecordInfo(challengeRecordId))
                .thenReturn(new ChallengeRecordNotificationRow(challengeRecordId, groupChallengeId, authorUserId, "작성자"));

        // when
        listener.on(new CommentCreatedEvent(challengeRecordId, authorUserId, commentId));

        // then
        verify(notificationService, never()).send(any(NotificationCommand.class));
    }

    @Test
    @DisplayName("내일부터 인증 시작 이벤트는 그룹 멤버 전체에게 그룹 알림 커맨드를 만든다")
    void certificationStartTomorrowEvent_createsCommandsForGroupMembers() {
        // given
        Long groupId = 10L;
        when(groupReader.findGroupName(groupId)).thenReturn("디톡스방");
        when(recipientReader.findActiveGroupMemberUserIds(groupId)).thenReturn(List.of(1L, 2L));

        // when
        listener.on(new CertificationStartTomorrowEvent(groupId));

        // then
        List<NotificationCommand> commands = captureCommands(2);
        assertThat(commands)
                .extracting(NotificationCommand::recipientUserId)
                .containsExactly(1L, 2L);
        assertThat(commands).allSatisfy(command -> {
            assertThat(command.typeCode()).isEqualTo(NotificationTypeCode.CERTIFICATION_START_TOMORROW);
            assertThat(command.context().get("groupName")).isEqualTo("디톡스방");
            assertThat(command.payload().targetType()).isEqualTo(NotificationTargetType.GROUP);
            assertThat(command.payload().targetId()).isEqualTo(groupId);
            assertThat(command.saveHistory()).isTrue();
        });
    }

    @Test
    @DisplayName("목표 설정 리마인드 이벤트는 목표 미설정 사용자에게 그룹 알림 커맨드를 만든다")
    void goalSettingReminderEvent_createsCommandForTargetUser() {
        // given
        Long groupId = 10L;
        Long targetUserId = 2L;
        when(userReader.findDisplayName(targetUserId)).thenReturn("지민");

        // when
        listener.on(new GoalSettingReminderEvent(groupId, targetUserId));

        // then
        NotificationCommand command = captureCommand();
        assertThat(command.recipientUserId()).isEqualTo(targetUserId);
        assertThat(command.typeCode()).isEqualTo(NotificationTypeCode.GOAL_SETTING_REMINDER);
        assertThat(command.context().get("nickname")).isEqualTo("지민");
        assertThat(command.payload().targetType()).isEqualTo(NotificationTargetType.GROUP);
        assertThat(command.payload().targetId()).isEqualTo(groupId);
        assertThat(command.saveHistory()).isTrue();
    }

    @Test
    @DisplayName("목표 설정 재촉 이벤트는 수신자에게 피드 알림 커맨드를 만든다")
    void pokeGoalSettingReminderEvent_createsCommandForReceiver() {
        // given
        Long challengeRecordId = 100L;
        Long groupChallengeId = 20L;
        Long senderUserId = 1L;
        Long receiverUserId = 2L;
        when(recipientReader.findChallengeRecordInfo(challengeRecordId))
                .thenReturn(new ChallengeRecordNotificationRow(challengeRecordId, groupChallengeId, 3L, "작성자"));
        when(userReader.findDisplayNames(Set.of(senderUserId, receiverUserId)))
                .thenReturn(Map.of(senderUserId, "슬빈", receiverUserId, "지민"));

        // when
        listener.on(new PokeGoalSettingReminderEvent(challengeRecordId, senderUserId, receiverUserId));

        // then
        NotificationCommand command = captureCommand();
        assertThat(command.recipientUserId()).isEqualTo(receiverUserId);
        assertThat(command.typeCode()).isEqualTo(NotificationTypeCode.POKE_GOAL_SETTING_REMINDER);
        assertThat(command.context().get("nickname")).isEqualTo("슬빈");
        assertThat(command.context().get("me")).isEqualTo("지민");
        assertThat(command.payload().targetType()).isEqualTo(NotificationTargetType.FEED);
        assertThat(command.payload().targetId()).isEqualTo(groupChallengeId);
        assertThat(command.saveHistory()).isTrue();
    }

    private NotificationCommand captureCommand() {
        return captureCommands(1).getFirst();
    }

    private List<NotificationCommand> captureCommands(int count) {
        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationService, org.mockito.Mockito.times(count)).send(captor.capture());
        return captor.getAllValues();
    }
}
