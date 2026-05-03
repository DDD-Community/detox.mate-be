package com.detoxmate.feed.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
    ChallengeRecordRepository challengeRecordRepository;

    @Autowired
    ChallengeRecordStatusCountRepository statusCountRepository;

    @Autowired
    ActivityRecordRepository activityRecordRepository;

    @Autowired
    PokeRepository pokeRepository;

    @Test
    @DisplayName("홈 피드를 조회하면 오늘 챌린지 기록이 없는 참여자에게 빈 챌린지 기록을 생성하고 카드에 내려준다")
    void getHomeFeed_createsTodayChallengeRecordForParticipantWithoutRecord() {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User targetUser = userRepository.save(User.createNew("민준"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupChallengeParticipant targetParticipant =
                saveParticipant(group.getId(), challenge.getId(), targetUser);

        assertThat(challengeRecordRepository.findAll()).isEmpty();

        // when
        HomeFeedResponse response = feedService.getHomeFeed(challenge.getId(), currentUser.getId());

        // then
        assertThat(response.challenge().groupChallengeId()).isEqualTo(challenge.getId());
        assertThat(response.members()).hasSize(2);

        HomeFeedMemberCard targetCard = findCard(response, targetUser.getId());

        assertThat(targetCard.challengeRecordId()).isNotNull();
        assertThat(targetCard.activityRecordId()).isNull();
        assertThat(targetCard.verifiedAt()).isNull();
        assertThat(targetCard.challengeStatus()).isEqualTo("BEFORE_RECORD");
        assertThat(targetCard.activityImageUrl()).isNull();
        assertThat(targetCard.oneLineReview()).isNull();
        assertThat(targetCard.reactionCount()).isZero();
        assertThat(targetCard.commentCount()).isZero();
        assertThat(targetCard.pokeCount()).isZero();
        assertThat(targetCard.isPoked()).isFalse();

        ChallengeRecord targetRecord = challengeRecordRepository.findById(targetCard.challengeRecordId())
                .orElseThrow();

        assertThat(targetRecord.getGroupChallengeId()).isEqualTo(challenge.getId());
        assertThat(targetRecord.getGroupChallengeParticipantId()).isEqualTo(targetParticipant.getId());
        assertThat(targetRecord.getRecordDate()).isEqualTo(LocalDate.now());
        assertThat(targetRecord.getActivityRecordId()).isNull();
    }

    @Test
    @DisplayName("홈 피드는 인증한 참여자를 최신 인증순으로 먼저 보여주고 미인증자는 이름순으로 보여준다")
    void getHomeFeed_ordersVerifiedMembersByLatestAndNotYetMembersByDisplayName() {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User alice = userRepository.save(User.createNew("Alice"));
        User bob = userRepository.save(User.createNew("Bob"));
        User oldVerifiedUser = userRepository.save(User.createNew("OldVerified"));
        User latestVerifiedUser = userRepository.save(User.createNew("LatestVerified"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        saveParticipant(group.getId(), challenge.getId(), alice);
        saveParticipant(group.getId(), challenge.getId(), bob);

        GroupChallengeParticipant oldParticipant =
                saveParticipant(group.getId(), challenge.getId(), oldVerifiedUser);
        GroupChallengeParticipant latestParticipant =
                saveParticipant(group.getId(), challenge.getId(), latestVerifiedUser);

        ChallengeRecord oldRecord = saveCertifiedRecord(challenge.getId(), oldParticipant, oldVerifiedUser);
        ChallengeRecord latestRecord = saveCertifiedRecord(challenge.getId(), latestParticipant, latestVerifiedUser);

        // 최신순을 안정적으로 만들기 위해 최신 인증 activity record가 나중에 생성되도록 저장 순서를 둔다.
        assertThat(latestRecord.getActivityRecordId()).isGreaterThan(oldRecord.getActivityRecordId());

        // when
        HomeFeedResponse response = feedService.getHomeFeed(challenge.getId(), currentUser.getId());

        // then
        assertThat(response.members())
                .extracting(HomeFeedMemberCard::displayName)
                .containsExactly("LatestVerified", "OldVerified", "Alice", "Bob", "나");
    }

    @Test
    @DisplayName("이미 콕 찌른 미인증 참여자 카드는 isPoked가 true다")
    void getHomeFeed_marksPokedCardTrue() {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User targetUser = userRepository.save(User.createNew("민준"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupChallengeParticipant targetParticipant =
                saveParticipant(group.getId(), challenge.getId(), targetUser);

        ChallengeRecord targetRecord = challengeRecordRepository.save(
                ChallengeRecord.create(challenge.getId(), targetParticipant.getId(), LocalDate.now())
        );

        pokeRepository.save(
                Poke.create(
                        targetRecord.getId(),
                        currentUser.getId(),
                        targetUser.getId(),
                        LocalDate.now()
                )
        );

        // when
        HomeFeedResponse response = feedService.getHomeFeed(challenge.getId(), currentUser.getId());

        // then
        HomeFeedMemberCard targetCard = findCard(response, targetUser.getId());

        assertThat(targetCard.challengeRecordId()).isEqualTo(targetRecord.getId());
        assertThat(targetCard.isPoked()).isTrue();
        assertThat(targetCard.activityRecordId()).isNull();
        assertThat(targetCard.challengeStatus()).isEqualTo("BEFORE_RECORD");
    }

    @Test
    @DisplayName("홈 피드는 챌린지 기록 카운트를 카드에 반영한다")
    void getHomeFeed_includesChallengeRecordCounts() {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User targetUser = userRepository.save(User.createNew("민준"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupChallengeParticipant targetParticipant =
                saveParticipant(group.getId(), challenge.getId(), targetUser);

        ChallengeRecord targetRecord = challengeRecordRepository.save(
                ChallengeRecord.create(challenge.getId(), targetParticipant.getId(), LocalDate.now())
        );

        ChallengeRecordStatusCount statusCount = ChallengeRecordStatusCount.create(targetRecord.getId());
        statusCount.increaseBeforeCommentCount();
        statusCount.increaseBeforeCommentCount();
        statusCount.increasePokeCount();

        statusCountRepository.save(statusCount);

        // when
        HomeFeedResponse response = feedService.getHomeFeed(challenge.getId(), currentUser.getId());

        // then
        HomeFeedMemberCard targetCard = findCard(response, targetUser.getId());

        assertThat(targetCard.commentCount()).isEqualTo(2);
        assertThat(targetCard.pokeCount()).isEqualTo(1);
        assertThat(targetCard.reactionCount()).isZero();
    }

    @Test
    @DisplayName("인증한 참여자 카드는 인증 기록 정보를 포함한다")
    void getHomeFeed_includesActivityRecordInfoForCertifiedMember() {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User targetUser = userRepository.save(User.createNew("민준"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupChallengeParticipant targetParticipant =
                saveParticipant(group.getId(), challenge.getId(), targetUser);

        ChallengeRecord challengeRecord = saveCertifiedRecord(challenge.getId(), targetParticipant, targetUser);

        // when
        HomeFeedResponse response = feedService.getHomeFeed(challenge.getId(), currentUser.getId());

        // then
        HomeFeedMemberCard targetCard = findCard(response, targetUser.getId());

        assertThat(targetCard.challengeRecordId()).isEqualTo(challengeRecord.getId());
        assertThat(targetCard.activityRecordId()).isEqualTo(challengeRecord.getActivityRecordId());
        assertThat(targetCard.verifiedAt()).isNotNull();
        assertThat(targetCard.challengeStatus()).isEqualTo("AFTER_RECORD_SUCCESS");
        assertThat(targetCard.activityImageUrl()).isEqualTo("activity/image.png");
        assertThat(targetCard.oneLineReview()).isEqualTo("오늘 인증 완료");
    }

    private ChallengeRecord saveCertifiedRecord(
            Long groupChallengeId,
            GroupChallengeParticipant participant,
            User user
    ) {
        ActivityRecord activityRecord = activityRecordRepository.save(
                ActivityRecord.create(
                        user,
                        participant,
                        "activity/image.png",
                        "오늘 인증 완료"
                )
        );

        ChallengeRecord challengeRecord = ChallengeRecord.create(
                groupChallengeId,
                participant.getId(),
                LocalDate.now()
        );

        challengeRecord.certify(
                activityRecord.getId(),
                participant.getId(),
                ChallengeRecordCertificationResult.SUCCESS
        );

        return challengeRecordRepository.save(challengeRecord);
    }

    private GroupChallengeParticipant saveParticipant(Long groupId, Long groupChallengeId, User user) {
        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(user.getId(), groupId));

        return participantRepository.save(GroupChallengeParticipant.join(groupMember.getId(), groupChallengeId));
    }


    private HomeFeedMemberCard findCard(HomeFeedResponse response, Long userId) {
        return response.members().stream()
                .filter(member -> member.userId().equals(userId))
                .findFirst()
                .orElseThrow();
    }


}
