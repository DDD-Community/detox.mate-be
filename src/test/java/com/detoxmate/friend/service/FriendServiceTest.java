package com.detoxmate.friend.service;

import com.detoxmate.friend.domain.Friend;
import com.detoxmate.friend.domain.FriendStatus;
import com.detoxmate.friend.dto.FriendRelationshipStatus;
import com.detoxmate.friend.dto.FriendRequestResponse;
import com.detoxmate.friend.dto.FriendResponse;
import com.detoxmate.friend.dto.FriendUserResponse;
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
    @DisplayName("이메일로 검색하면 일치하는 활성 사용자의 공개 정보와 관계 상태를 반환한다")
    void searchByEmail_returnsTheMatchingUserWithoutExposingEmail() {
        User me = saveUser("나", "me@example.com");
        User target = saveUser("친구", "friend@example.com", "profile-images/friend/profile.png");

        FriendUserResponse response = friendService.searchByEmail("  FRIEND@EXAMPLE.COM ", me.getId());

        assertThat(response).isEqualTo(new FriendUserResponse(
                target.getId(),
                "친구",
                "https://example.com/media/profile-images/friend/profile.png",
                FriendRelationshipStatus.NONE,
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
        assertThat(response.user()).isEqualTo(new FriendUserResponse(
                sender.getId(),
                "보낸 사람",
                null,
                FriendRelationshipStatus.FRIEND,
                null
        ));
        assertThat(response.acceptedAt()).isEqualTo(FIXED_NOW);
        assertThat(friendRepository.findById(request.requestId()))
                .get()
                .extracting(Friend::getStatus)
                .isEqualTo(FriendStatus.ACCEPTED);
    }

    @Test
    @DisplayName("친구 요청을 보낸 사용자는 대기 중인 요청을 취소할 수 있다")
    void deletePendingRequest_allowsSenderCancellation() {
        User sender = saveUser("보낸 사람", "sender@example.com");
        User receiver = saveUser("받는 사람", "receiver@example.com");
        FriendRequestResponse request = friendService.sendRequest(sender.getId(), receiver.getId());

        friendService.deletePendingRequest(request.requestId(), sender.getId());

        assertThat(friendRepository.findById(request.requestId())).isEmpty();
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

    private User saveUser(String displayName, String email) {
        return saveUser(displayName, email, null);
    }

    private User saveUser(String displayName, String email, String profileImageObjectKey) {
        return userRepository.saveAndFlush(User.createNew(displayName, profileImageObjectKey, email));
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
