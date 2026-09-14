package com.detoxmate.friend.repository;

import com.detoxmate.friend.domain.Friend;
import com.detoxmate.friend.domain.FriendStatus;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class FriendRepositoryTest {

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자 쌍의 순서가 달라도 동일한 친구 관계를 조회한다")
    void findByUserPair_findsTheSameRelationshipRegardlessOfDirection() {
        User from = userRepository.save(User.createNew("보낸 사람"));
        User to = userRepository.save(User.createNew("받은 사람"));
        Friend request = friendRepository.saveAndFlush(Friend.request(from.getId(), to.getId()));

        assertThat(friendRepository.findByUserPair(to.getId(), from.getId()))
                .get()
                .extracting(Friend::getId)
                .isEqualTo(request.getId());
    }

    @Test
    @DisplayName("받은 사용자가 수락한 대기 요청 행만 친구 상태로 변경한다")
    void acceptPendingRequest_updatesOnlyTheReceiverOwnedPendingRow() {
        User from = userRepository.save(User.createNew("보낸 사람"));
        User to = userRepository.save(User.createNew("받은 사람"));
        Friend request = friendRepository.saveAndFlush(Friend.request(from.getId(), to.getId()));
        LocalDateTime acceptedAt = LocalDateTime.of(2026, 9, 14, 18, 0);

        int updated = friendRepository.acceptPendingRequest(
                request.getId(),
                to.getId(),
                acceptedAt
        );

        assertThat(updated).isEqualTo(1);
        Friend accepted = friendRepository.findById(request.getId()).orElseThrow();
        assertThat(accepted.getStatus()).isEqualTo(FriendStatus.ACCEPTED);
        assertThat(accepted.getAcceptedAt()).isEqualTo(acceptedAt);
    }
}
