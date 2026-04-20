package com.detoxmate.group.service;

import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeStatus;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.dto.GroupMemberResponse;
import com.detoxmate.group.dto.GroupResponse;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.group.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GroupServiceTest {

    private static final String INVITE_CODE = "ABCDE";
    private static final String SECOND_INVITE_CODE = "FGH12";
    private static final String GROUP_NAME = "주말 디톡스";
    private static final String TOO_LONG_GROUP_NAME = "1234567890123";
    private static final Long GROUP_ID = 1L;
    private static final Long OWNER_USER_ID = 1L;
    private static final Long MEMBER_USER_ID = 2L;
    private static final Long RECRUITING_CHALLENGE_ID = 10L;
    private static final Long JOINED_GROUP_MEMBER_ID = 101L;
    private static final LocalDateTime GROUP_CREATED_AT = LocalDateTime.of(2026, 4, 19, 10, 0);
    private static final LocalDateTime MEMBER_JOINED_AT = LocalDateTime.of(2026, 4, 19, 10, 30);

    // DB 경계 — mock
    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final GroupMemberRepository groupMemberRepository = mock(GroupMemberRepository.class);
    private final GroupChallengeRepository groupChallengeRepository = mock(GroupChallengeRepository.class);
    private final GroupChallengeParticipantRepository groupChallengeParticipantRepository = mock(GroupChallengeParticipantRepository.class);

    // 내부 그룹 서비스 — 실객체
    private final GroupMemberService groupMemberService = new GroupMemberService(groupMemberRepository);
    private final GroupChallengeService groupChallengeService = new GroupChallengeService(groupChallengeRepository);
    private final GroupChallengeParticipantService groupChallengeParticipantService = new GroupChallengeParticipantService(groupChallengeParticipantRepository);
    private final InviteCodeGenerator inviteCodeGenerator = mock(InviteCodeGenerator.class);

    private final GroupService groupService = new GroupService(
            groupRepository, groupMemberService, groupChallengeService, groupChallengeParticipantService, inviteCodeGenerator
    );

    @BeforeEach
    void setUp() {
        when(groupRepository.save(any(Group.class))).thenAnswer(i -> i.getArgument(0));
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(i -> i.getArgument(0));
        when(groupChallengeRepository.save(any(GroupChallenge.class))).thenAnswer(i -> i.getArgument(0));
        when(groupChallengeParticipantRepository.save(any(GroupChallengeParticipant.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void 그룹_생성_시_생성자가_OWNER_멤버로_등록된다() {
        when(groupMemberRepository.existsByUserIdAndStatus(OWNER_USER_ID, "ACTIVE")).thenReturn(false);
        when(inviteCodeGenerator.generate()).thenReturn(INVITE_CODE);

        GroupResponse response = groupService.createGroup(1L, "테스트그룹");

        assertThat(response.myRole()).isEqualTo("OWNER");
    }

    @Test
    void 그룹_생성_시_그룹_이름이_반환된다() {
        when(groupMemberRepository.existsByUserIdAndStatus(OWNER_USER_ID, "ACTIVE")).thenReturn(false);
        when(inviteCodeGenerator.generate()).thenReturn(INVITE_CODE);

        GroupResponse response = groupService.createGroup(1L, "나의그룹");

        assertThat(response.name()).isEqualTo("나의그룹");
    }

    @Test
    void 내가_속한_그룹_목록을_조회하면_그룹_응답_리스트를_반환한다() {
        when(groupMemberRepository.findAllByUserIdAndStatus(OWNER_USER_ID, "ACTIVE"))
                .thenReturn(List.of(ownerGroupMember()));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(recruitingGroup()));
        when(groupChallengeRepository.findTopByGroupIdOrderByChallengeNoDesc(GROUP_ID))
                .thenReturn(Optional.of(activeChallenge()));
        when(groupMemberRepository.findMembersWithUserByGroupId(GROUP_ID))
                .thenReturn(List.of(ownerMember()));

        List<GroupResponse> responses = groupService.getMyGroups(OWNER_USER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(GROUP_ID);
        assertThat(responses.getFirst().myRole()).isEqualTo("OWNER");
        assertThat(responses.getFirst().currentChallenge().status()).isEqualTo("ACTIVE");
    }

    @Test
    void 이미_다른_그룹에_속한_유저면_새로운_그룹을_생성할_수_없다() {
        when(groupMemberRepository.existsByUserIdAndStatus(OWNER_USER_ID, "ACTIVE")).thenReturn(true);

        assertThatThrownBy(() -> groupService.createGroup(OWNER_USER_ID, GROUP_NAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 그룹이 있어서, 새로운 그룹을 생성할 수 없습니다.");
    }

    @Test
    void 그룹_이름이_12자를_초과하면_예외를_던진다() {
        when(groupMemberRepository.existsByUserIdAndStatus(OWNER_USER_ID, "ACTIVE")).thenReturn(false);
        when(inviteCodeGenerator.generate()).thenReturn(INVITE_CODE);

        assertThatThrownBy(() -> groupService.createGroup(OWNER_USER_ID, TOO_LONG_GROUP_NAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("그룹 이름은 12자 이하여야 합니다.");
    }

    @Test
    void 그룹_생성_시_5자리_초대코드를_반환한다() {
        when(groupMemberRepository.existsByUserIdAndStatus(OWNER_USER_ID, "ACTIVE")).thenReturn(false);
        when(inviteCodeGenerator.generate()).thenReturn(INVITE_CODE);

        GroupResponse response = groupService.createGroup(OWNER_USER_ID, GROUP_NAME);

        assertThat(response.inviteCode()).isEqualTo(INVITE_CODE);
        assertThat(response.inviteCode()).hasSize(5);
    }

    @Test
    void 그룹_생성_시_초대코드가_중복되면_새로운_코드로_재생성한다() {
        when(groupMemberRepository.existsByUserIdAndStatus(OWNER_USER_ID, "ACTIVE")).thenReturn(false);
        when(inviteCodeGenerator.generate()).thenReturn(INVITE_CODE, SECOND_INVITE_CODE);
        when(groupRepository.save(any(Group.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate invite code"))
                .thenAnswer(i -> i.getArgument(0));

        GroupResponse response = groupService.createGroup(OWNER_USER_ID, GROUP_NAME);

        assertThat(response.inviteCode()).isEqualTo(SECOND_INVITE_CODE);
    }

    @Test
    void 초대코드에_해당하는_그룹이_없으면_예외를_던진다() {
        // given
        when(groupRepository.findByInviteCode(INVITE_CODE)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> groupService.joinGroup(INVITE_CODE, OWNER_USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("초대코드에 해당하는 그룹이 없습니다.");
    }

    @Test
    void 이미_다른_그룹에_속한_유저면_예외를_던진다() {
        // given
        when(groupRepository.findByInviteCode(INVITE_CODE)).thenReturn(Optional.of(recruitingGroup()));
        when(groupMemberRepository.existsByUserIdAndStatus(MEMBER_USER_ID, "ACTIVE")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> groupService.joinGroup(INVITE_CODE, MEMBER_USER_ID))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void 그룹_챌린지가_이미_실행중이면_예외를_던진다() {
        // given
        when(groupRepository.findByInviteCode(INVITE_CODE)).thenReturn(Optional.of(recruitingGroup()));
        when(groupMemberRepository.existsByUserIdAndStatus(MEMBER_USER_ID, "ACTIVE")).thenReturn(false);
        when(groupChallengeRepository.findTopByGroupIdOrderByChallengeNoDesc(GROUP_ID))
                .thenReturn(Optional.of(activeChallenge()));

        // when & then
        assertThatThrownBy(() -> groupService.joinGroup(INVITE_CODE, MEMBER_USER_ID))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void 모집중인_그룹에_참여하면_그룹_멤버와_챌린지_참가자가_추가된다() {
        // given
        when(groupRepository.findByInviteCode(INVITE_CODE)).thenReturn(Optional.of(recruitingGroup()));
        when(groupMemberRepository.existsByUserIdAndStatus(MEMBER_USER_ID, "ACTIVE")).thenReturn(false);
        when(groupChallengeRepository.findTopByGroupIdOrderByChallengeNoDesc(GROUP_ID))
                .thenReturn(Optional.of(recruitingChallenge()));
        when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(joinedMember());
        when(groupMemberRepository.findMembersWithUserByGroupId(GROUP_ID)).thenReturn(groupMembers());

        // when
        GroupResponse response = groupService.joinGroup(INVITE_CODE, MEMBER_USER_ID);

        // then
        verify(groupMemberRepository).save(any(GroupMember.class));
        verify(groupChallengeParticipantRepository).save(any(GroupChallengeParticipant.class));
        assertThat(response.id()).isEqualTo(GROUP_ID);
        assertThat(response.inviteCode()).isEqualTo(INVITE_CODE);
        assertThat(response.myRole()).isEqualTo("MEMBER");
        assertThat(response.members()).hasSize(2);
        assertThat(response.currentChallenge().status()).isEqualTo("RECRUITING");
    }

    private Group recruitingGroup() {
        Group group = Group.createNew(GROUP_NAME, INVITE_CODE);
        ReflectionTestUtils.setField(group, "id", GROUP_ID);
        ReflectionTestUtils.setField(group, "createdAt", GROUP_CREATED_AT);
        ReflectionTestUtils.setField(group, "updatedAt", MEMBER_JOINED_AT);
        return group;
    }

    private GroupChallenge activeChallenge() {
        GroupChallenge challenge = recruitingChallenge();
        ReflectionTestUtils.setField(challenge, "status", GroupChallengeStatus.ACTIVE);
        return challenge;
    }

    private GroupChallenge recruitingChallenge() {
        GroupChallenge challenge = GroupChallenge.createFirst(GROUP_ID);
        ReflectionTestUtils.setField(challenge, "id", RECRUITING_CHALLENGE_ID);
        return challenge;
    }

    private GroupMember joinedMember() {
        GroupMember groupMember = GroupMember.createMember(MEMBER_USER_ID, GROUP_ID);
        ReflectionTestUtils.setField(groupMember, "id", JOINED_GROUP_MEMBER_ID);
        return groupMember;
    }

    private GroupMember ownerGroupMember() {
        GroupMember groupMember = GroupMember.createOwner(OWNER_USER_ID, GROUP_ID);
        ReflectionTestUtils.setField(groupMember, "id", 100L);
        return groupMember;
    }

    private List<GroupMemberResponse> groupMembers() {
        return List.of(ownerMember(), joinedMemberResponse());
    }

    private GroupMemberResponse ownerMember() {
        return new GroupMemberResponse(
                100L,
                OWNER_USER_ID,
                "지민",
                "https://...",
                "OWNER",
                "ACTIVE",
                GROUP_CREATED_AT,
                null
        );
    }

    private GroupMemberResponse joinedMemberResponse() {
        return new GroupMemberResponse(
                JOINED_GROUP_MEMBER_ID,
                MEMBER_USER_ID,
                "민수",
                null,
                "MEMBER",
                "ACTIVE",
                MEMBER_JOINED_AT,
                null
        );
    }
}
