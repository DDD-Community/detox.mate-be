package com.detoxmate.feed.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.activityrecordchallengestatus.domain.ActivityRecordChallengeStatus;
import com.detoxmate.activityrecordchallengestatus.repository.ActivityRecordChallengeStatusRepository;
import com.detoxmate.feed.dto.response.HomeFeedMemberCard;
import com.detoxmate.feed.dto.response.HomeFeedResponse;
import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.group.repository.GroupRepository;
import com.detoxmate.poke.domain.Poke;
import com.detoxmate.poke.repository.PokeRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FeedServiceTest {

    @Autowired
    FeedService feedService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    GroupRepository groupRepository;

    @Autowired
    GroupMemberRepository groupMemberRepository;

    @Autowired
    GroupChallengeRepository groupChallengeRepository;

    @Autowired
    GroupChallengeParticipantRepository participantRepository;

    @Autowired
    ActivityRecordRepository activityRecordRepository;

    @Autowired
    ActivityRecordChallengeStatusRepository statusRepository;

    @Autowired
    PokeRepository pokeRepository;

    @Test
    @DisplayName("홈 피드는 그룹 챌린지의 멤버 카드와 인증글 집계 상태를 반환한다")
    void getHomeFeed_returnsMemberCardsWithActivityRecordStatus() {
        // given
        User currentUser = userRepository.save(User.createNew("나", "https://img/me"));
        User targetUser = userRepository.save(User.createNew("상대", "https://img/target"));

        Group group = groupRepository.save(Group.createNew("수능방", "A1B2C"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        GroupMember currentGroupMember = groupMemberRepository.save(
                GroupMember.createMember(currentUser.getId(), group.getId())
        );
        GroupMember targetGroupMember = groupMemberRepository.save(
                GroupMember.createMember(targetUser.getId(), group.getId())
        );

        participantRepository.save(GroupChallengeParticipant.join(currentGroupMember.getId(), challenge.getId()));
        GroupChallengeParticipant targetParticipant = participantRepository.save(
                GroupChallengeParticipant.join(targetGroupMember.getId(), challenge.getId())
        );

        ActivityRecord activityRecord = activityRecordRepository.save(
                ActivityRecord.create(
                        targetUser.getId(),
                        targetParticipant.getId(),
                        "https://activity/image.png",
                        "오늘은 30분 줄였다"
                )
        );

        ActivityRecordChallengeStatus status = ActivityRecordChallengeStatus.create(
                challenge.getId(),
                activityRecord.getId()
        );
        status.increaseCommentCount();
        status.increaseReactionCount();
        status.increaseReactionCount();
        status.increasePokeCount();
        statusRepository.save(status);

        pokeRepository.save(Poke.create(
                challenge.getId(),
                activityRecord.getId(),
                currentUser.getId(),
                targetUser.getId(),
                LocalDate.now()
        ));

        // when
        HomeFeedResponse response = feedService.getHomeFeed(
                challenge.getId(),
                currentUser.getId()
        );

        // then
        assertThat(response.challenge().groupChallengeId()).isEqualTo(challenge.getId());
        assertThat(response.challenge().groupChallengeName()).isEqualTo("수능방");

        assertThat(response.members()).hasSize(2);

        HomeFeedMemberCard targetCard = response.members().stream()
                .filter(card -> card.userId().equals(targetUser.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(targetCard.groupMemberId()).isEqualTo(targetGroupMember.getId());
        assertThat(targetCard.displayName()).isEqualTo("상대");
        assertThat(targetCard.profileImageUrl()).isEqualTo("https://img/target");
        assertThat(targetCard.challengeStatus()).isEqualTo("VERIFIED");
        assertThat(targetCard.activityImageUrl()).isEqualTo("https://activity/image.png");
        assertThat(targetCard.oneLineReview()).isEqualTo("오늘은 30분 줄였다");


        assertThat(targetCard.activityRecordId()).isEqualTo(activityRecord.getId());

        assertThat(targetCard.commentCount()).isEqualTo(1);
        assertThat(targetCard.reactionCount()).isEqualTo(2);
        assertThat(targetCard.pokeCount()).isEqualTo(1);
        assertThat(targetCard.isPoked()).isTrue();
    }

    @Test
    @DisplayName("인증글이 없는 멤버 카드는 NOT_YET 상태와 빈 집계값을 반환한다")
    void getHomeFeed_returnsNotYetCardWhenActivityRecordDoesNotExist() {
        // given
        User currentUser = userRepository.save(User.createNew("나"));
        User targetUser = userRepository.save(User.createNew("미인증"));

        Group group = groupRepository.save(Group.createNew("수능방", "B1C2D"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        GroupMember currentGroupMember = groupMemberRepository.save(GroupMember.createMember(currentUser.getId(), group.getId()));
        GroupMember targetGroupMember = groupMemberRepository.save(GroupMember.createMember(targetUser.getId(), group.getId()));

        participantRepository.save(GroupChallengeParticipant.join(currentGroupMember.getId(), challenge.getId()));
        participantRepository.save(GroupChallengeParticipant.join(targetGroupMember.getId(), challenge.getId()));

        // when
        HomeFeedResponse response = feedService.getHomeFeed(challenge.getId(), currentUser.getId());

        // then
        HomeFeedMemberCard targetCard = response.members().stream()
                .filter(card -> card.userId().equals(targetUser.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(targetCard.challengeStatus()).isEqualTo("NOT_YET");
        assertThat(targetCard.activityImageUrl()).isNull();
        assertThat(targetCard.oneLineReview()).isNull();
        assertThat(targetCard.activityRecordId()).isNull();
        assertThat(targetCard.commentCount()).isZero();
        assertThat(targetCard.reactionCount()).isZero();
        assertThat(targetCard.pokeCount()).isZero();
        assertThat(targetCard.isPoked()).isFalse();
    }

}
