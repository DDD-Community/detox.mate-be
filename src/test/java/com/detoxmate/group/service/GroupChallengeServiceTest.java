package com.detoxmate.group.service;

import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeStatus;
import com.detoxmate.group.dto.GroupChallengeParticipantRow;
import com.detoxmate.group.dto.GroupChallengeParticipantResponse;
import com.detoxmate.group.dto.GroupChallengeResponse;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupChallengeServiceTest {

    private static final Long CHALLENGE_ID = 10L;
    private static final Long GROUP_ID = 1L;
    private static final Long OWNER_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 3L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 4, 19, 10, 0);
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 4, 20, 9, 0);

    private final GroupChallengeRepository groupChallengeRepository = mock(GroupChallengeRepository.class);
    private final GroupChallengeParticipantRepository groupChallengeParticipantRepository = mock(GroupChallengeParticipantRepository.class);
    private final GroupRepository groupRepository = mock(GroupRepository.class);

    private final GroupChallengeService groupChallengeService = new GroupChallengeService(
            groupChallengeRepository,
            groupChallengeParticipantRepository,
            groupRepository
    );

    @Test
    void 내가_참가한_그룹_챌린지_목록을_조회하면_챌린지_응답_리스트를_반환한다() {
        when(groupChallengeRepository.findAllByParticipantUserIdAndStatus(OWNER_USER_ID, null))
                .thenReturn(List.of(activeChallenge()));
        when(groupRepository.findAllById(List.of(GROUP_ID))).thenReturn(List.of(group()));
        when(groupChallengeParticipantRepository.findParticipantRowsByGroupChallengeIds(List.of(CHALLENGE_ID)))
                .thenReturn(List.of(ownerParticipantRow(), memberParticipantRow()));

        List<GroupChallengeResponse> responses = groupChallengeService.getMyGroupChallenges(OWNER_USER_ID, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(CHALLENGE_ID);
        assertThat(responses.getFirst().status()).isEqualTo("ACTIVE");
        assertThat(responses.getFirst().participants()).hasSize(2);
        assertThat(responses.getFirst().participants().getFirst().goalTimes()).isEmpty();
    }

    @Test
    void 상태값을_전달하면_해당_상태의_그룹_챌린지만_조회한다() {
        when(groupChallengeRepository.findAllByParticipantUserIdAndStatus(OWNER_USER_ID, GroupChallengeStatus.ACTIVE))
                .thenReturn(List.of(activeChallenge()));
        when(groupRepository.findAllById(List.of(GROUP_ID))).thenReturn(List.of(group()));
        when(groupChallengeParticipantRepository.findParticipantRowsByGroupChallengeIds(List.of(CHALLENGE_ID)))
                .thenReturn(List.of(ownerParticipantRow(), memberParticipantRow()));

        List<GroupChallengeResponse> responses = groupChallengeService.getMyGroupChallenges(OWNER_USER_ID, "ACTIVE");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().status()).isEqualTo("ACTIVE");
    }

    @Test
    void 잘못된_상태값으로_목록을_조회하면_400_에러를_던진다() {
        assertThatThrownBy(() -> groupChallengeService.getMyGroupChallenges(OWNER_USER_ID, "WRONG"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void 참가자인_경우_그룹_챌린지_상세를_반환한다() {
        when(groupChallengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(activeChallenge()));
        when(groupChallengeParticipantRepository.existsByGroupChallengeIdAndUserId(CHALLENGE_ID, OWNER_USER_ID))
                .thenReturn(true);
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
        when(groupChallengeParticipantRepository.findParticipantResponsesByGroupChallengeId(CHALLENGE_ID))
                .thenReturn(List.of(ownerParticipant(), memberParticipant()));

        GroupChallengeResponse response = groupChallengeService.getGroupChallenge(CHALLENGE_ID, OWNER_USER_ID);

        assertThat(response.id()).isEqualTo(CHALLENGE_ID);
        assertThat(response.groupId()).isEqualTo(GROUP_ID);
        assertThat(response.participants()).hasSize(2);
        assertThat(response.participants().getFirst().goalTimes()).isEmpty();
    }

    @Test
    void 존재하지_않는_그룹_챌린지를_조회하면_404_에러를_던진다() {
        when(groupChallengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupChallengeService.getGroupChallenge(CHALLENGE_ID, OWNER_USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    void 참가자가_아닌_사용자가_그룹_챌린지_상세를_조회하면_403_에러를_던진다() {
        when(groupChallengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(activeChallenge()));
        when(groupChallengeParticipantRepository.existsByGroupChallengeIdAndUserId(CHALLENGE_ID, OTHER_USER_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> groupChallengeService.getGroupChallenge(CHALLENGE_ID, OTHER_USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    private Group group() {
        Group group = Group.createNew("주말 디톡스", "AB123");
        ReflectionTestUtils.setField(group, "id", GROUP_ID);
        return group;
    }

    private GroupChallenge activeChallenge() {
        GroupChallenge challenge = GroupChallenge.createFirst(GROUP_ID);
        ReflectionTestUtils.setField(challenge, "id", CHALLENGE_ID);
        ReflectionTestUtils.setField(challenge, "status", GroupChallengeStatus.ACTIVE);
        ReflectionTestUtils.setField(challenge, "startAt", LocalDateTime.of(2026, 4, 20, 9, 0));
        ReflectionTestUtils.setField(challenge, "endAt", LocalDateTime.of(2026, 4, 27, 9, 0));
        ReflectionTestUtils.setField(challenge, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(challenge, "updatedAt", UPDATED_AT);
        return challenge;
    }

    private GroupChallengeParticipantResponse ownerParticipant() {
        return new GroupChallengeParticipantResponse(
                1000L,
                100L,
                OWNER_USER_ID,
                "지민",
                "https://...",
                "JOINED",
                CREATED_AT,
                null,
                null
        );
    }

    private GroupChallengeParticipantRow ownerParticipantRow() {
        return new GroupChallengeParticipantRow(
                CHALLENGE_ID,
                1000L,
                100L,
                OWNER_USER_ID,
                "지민",
                "https://...",
                "JOINED",
                CREATED_AT,
                null
        );
    }

    private GroupChallengeParticipantResponse memberParticipant() {
        return new GroupChallengeParticipantResponse(
                1001L,
                101L,
                2L,
                "민수",
                null,
                "JOINED",
                LocalDateTime.of(2026, 4, 19, 10, 30),
                null,
                null
        );
    }

    private GroupChallengeParticipantRow memberParticipantRow() {
        return new GroupChallengeParticipantRow(
                CHALLENGE_ID,
                1001L,
                101L,
                2L,
                "민수",
                null,
                "JOINED",
                LocalDateTime.of(2026, 4, 19, 10, 30),
                null
        );
    }
}
