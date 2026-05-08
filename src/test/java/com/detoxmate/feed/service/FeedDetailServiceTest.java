package com.detoxmate.feed.service;

import com.detoxmate.activityrecord.domain.ActivityRecord;
import com.detoxmate.activityrecord.repository.ActivityRecordRepository;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.domain.ChallengeRecordCertificationResult;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.feed.dto.response.FeedDetailResponse;
import com.detoxmate.feed.dto.response.FeedGoalStatus;
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
import com.detoxmate.reaction.domain.Reaction;
import com.detoxmate.reaction.domain.ReactionBody;
import com.detoxmate.reaction.repository.ReactionRepository;
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
class FeedDetailServiceTest {

    @Autowired
    FeedDetailService feedDetailService;

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

    @Autowired
    ReactionRepository reactionRepository;

    @Test
    @DisplayName("인증 전 오늘 상세는 인증 정보 없이 콕 가능 상태와 찌른 사람 목록을 반환한다")
    void getFeedDetail_beforeRecordToday_returnsBeforeRecordDetail() {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User author = userRepository.save(User.createNew("민준"));
        User poker1 = userRepository.save(User.createNew("Alice"));
        User poker2 = userRepository.save(User.createNew("Bob"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupChallengeParticipant authorParticipant = saveParticipant(group.getId(), challenge.getId(), author);

        ChallengeRecord challengeRecord = challengeRecordRepository.save(
                ChallengeRecord.create(challenge.getId(), authorParticipant.getId(), LocalDate.now())
        );

        statusCountRepository.saveAndFlush(ChallengeRecordStatusCount.create(challengeRecord.getId()));
        statusCountRepository.increaseBeforeCommentCount(challengeRecord.getId());
        statusCountRepository.increasePokeCount(challengeRecord.getId());
        statusCountRepository.increasePokeCount(challengeRecord.getId());

        pokeRepository.save(Poke.create(challengeRecord.getId(), poker1.getId(), author.getId(), LocalDate.now()));
        pokeRepository.save(Poke.create(challengeRecord.getId(), poker2.getId(), author.getId(), LocalDate.now()));

        // when
        FeedDetailResponse response = feedDetailService.getFeedDetail(
                challengeRecord.getId(),
                currentUser.getId()
        );

        // then
        assertThat(response.challengeRecordId()).isEqualTo(challengeRecord.getId());
        assertThat(response.groupChallengeId()).isEqualTo(challenge.getId());
        assertThat(response.activityRecordId()).isNull();
        assertThat(response.challengeStatus()).isEqualTo("BEFORE_RECORD");
        assertThat(response.recordDate()).isEqualTo(LocalDate.now());

        assertThat(response.author().userId()).isEqualTo(author.getId());
        assertThat(response.author().displayName()).isEqualTo("민준");

        assertThat(response.activityCreatedAt()).isNull();
        assertThat(response.activityImageUrl()).isNull();
        assertThat(response.oneLineReview()).isNull();
        assertThat(response.goalStatus()).isNull();
        assertThat(response.snapshotGoalMinutes()).isNull();
        assertThat(response.details()).isEmpty();

        assertThat(response.reactions().totalCount()).isZero();
        assertThat(response.reactions().summary()).isEmpty();

        assertThat(response.commentCount()).isEqualTo(1);
        assertThat(response.pokeCount()).isEqualTo(2);
        assertThat(response.pokeable()).isTrue();
        assertThat(response.poked()).isFalse();

        assertThat(response.pokedUsers())
                .extracting(user -> user.displayName())
                .containsExactly("Bob", "Alice");
    }

    @Test
    @DisplayName("인증 전 오늘 상세에서 현재 사용자가 이미 찔렀으면 poked가 true다")
    void getFeedDetail_beforeRecordToday_returnsPokedTrueWhenCurrentUserAlreadyPoked() {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User author = userRepository.save(User.createNew("민준"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupChallengeParticipant authorParticipant = saveParticipant(group.getId(), challenge.getId(), author);

        ChallengeRecord challengeRecord = challengeRecordRepository.save(
                ChallengeRecord.create(challenge.getId(), authorParticipant.getId(), LocalDate.now())
        );

        pokeRepository.save(
                Poke.create(challengeRecord.getId(), currentUser.getId(), author.getId(), LocalDate.now())
        );

        // when
        FeedDetailResponse response = feedDetailService.getFeedDetail(
                challengeRecord.getId(),
                currentUser.getId()
        );

        // then
        assertThat(response.pokeable()).isTrue();
        assertThat(response.poked()).isTrue();
    }

    @Test
    @DisplayName("인증 전 과거 상세는 콕을 할 수 없지만 찌른 사람 목록은 반환한다")
    void getFeedDetail_beforeRecordPast_returnsNotPokeable() {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User author = userRepository.save(User.createNew("민준"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupChallengeParticipant authorParticipant = saveParticipant(group.getId(), challenge.getId(), author);

        ChallengeRecord challengeRecord = challengeRecordRepository.save(
                ChallengeRecord.create(challenge.getId(), authorParticipant.getId(), LocalDate.now().minusDays(1))
        );

        // when
        FeedDetailResponse response = feedDetailService.getFeedDetail(
                challengeRecord.getId(),
                currentUser.getId()
        );

        // then
        assertThat(response.challengeStatus()).isEqualTo("BEFORE_RECORD");
        assertThat(response.pokeable()).isFalse();
        assertThat(response.poked()).isFalse();
    }

    @Test
    @DisplayName("인증 후 상세는 인증 정보와 리액션 목록을 반환하고 콕은 불가능하다")
    void getFeedDetail_afterRecord_returnsAfterRecordDetail() {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User author = userRepository.save(User.createNew("민준"));
        User reactor1 = userRepository.save(User.createNew("Alice"));
        User reactor2 = userRepository.save(User.createNew("Bob"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupChallengeParticipant authorParticipant = saveParticipant(group.getId(), challenge.getId(), author);

        ActivityRecord activityRecord = activityRecordRepository.save(
                ActivityRecord.create(
                        author,
                        authorParticipant,
                        "activity/image.png",
                        "오늘 인증 완료"
                )
        );

        ChallengeRecord challengeRecord = ChallengeRecord.create(
                challenge.getId(),
                authorParticipant.getId(),
                LocalDate.now()
        );
        challengeRecord.certify(
                activityRecord.getId(),
                authorParticipant.getId(),
                ChallengeRecordCertificationResult.SUCCESS
        );
        challengeRecordRepository.save(challengeRecord);

        statusCountRepository.saveAndFlush(ChallengeRecordStatusCount.create(challengeRecord.getId()));
        statusCountRepository.increaseAfterCommentCount(challengeRecord.getId());
        statusCountRepository.increaseReactionCount(challengeRecord.getId());
        statusCountRepository.increaseReactionCount(challengeRecord.getId());

        reactionRepository.save(Reaction.create(challengeRecord.getId(), reactor1.getId(), ReactionBody.CLAP));
        reactionRepository.save(Reaction.create(challengeRecord.getId(), reactor2.getId(), ReactionBody.MUSCLE));

        // when
        FeedDetailResponse response = feedDetailService.getFeedDetail(
                challengeRecord.getId(),
                currentUser.getId()
        );

        // then
        assertThat(response.challengeRecordId()).isEqualTo(challengeRecord.getId());
        assertThat(response.activityRecordId()).isEqualTo(activityRecord.getId());
        assertThat(response.challengeStatus()).isEqualTo("AFTER_RECORD_SUCCESS");

        assertThat(response.activityCreatedAt()).isNotNull();
        assertThat(response.activityImageUrl()).isEqualTo("activity/image.png");
        assertThat(response.oneLineReview()).isEqualTo("오늘 인증 완료");
        assertThat(response.goalStatus()).isEqualTo(FeedGoalStatus.SUCCESS);

        assertThat(response.commentCount()).isEqualTo(1);
        assertThat(response.reactions().totalCount()).isEqualTo(2);
        assertThat(response.reactions().summary())
                .extracting(reaction -> reaction.displayName())
                .containsExactly("Bob", "Alice");

        assertThat(response.pokeable()).isFalse();
        assertThat(response.poked()).isFalse();
        assertThat(response.pokedUsers()).isEmpty();
    }

    @Test
    @DisplayName("탈퇴한 작성자의 과거 피드 상세는 익명 작성자 정보와 탈퇴 여부를 반환한다")
    void getFeedDetail_withWithdrawnAuthor_returnsAnonymizedAuthor() {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User author = userRepository.save(User.createNew("민준", "profile-images/2/profile.png"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        GroupChallengeParticipant authorParticipant = saveParticipant(group.getId(), challenge.getId(), author);

        ActivityRecord activityRecord = activityRecordRepository.save(
                ActivityRecord.create(
                        author,
                        authorParticipant,
                        "activity/image.png",
                        "오늘 인증 완료"
                )
        );
        ChallengeRecord challengeRecord = ChallengeRecord.create(
                challenge.getId(),
                authorParticipant.getId(),
                LocalDate.now()
        );
        challengeRecord.certify(
                activityRecord.getId(),
                authorParticipant.getId(),
                ChallengeRecordCertificationResult.SUCCESS
        );
        challengeRecordRepository.save(challengeRecord);

        GroupMember authorGroupMember = groupMemberRepository
                .findByUserIdAndGroupIdAndStatus(author.getId(), group.getId(), "ACTIVE")
                .orElseThrow();
        authorGroupMember.leave();
        authorParticipant.withdraw();
        author.withdraw();

        // when
        FeedDetailResponse response = feedDetailService.getFeedDetail(
                challengeRecord.getId(),
                currentUser.getId()
        );

        // then
        assertThat(response.author().userId()).isEqualTo(author.getId());
        assertThat(response.author().displayName()).isEqualTo(User.WITHDRAWN_DISPLAY_NAME);
        assertThat(response.author().profileImageUrl()).isNull();
        assertThat(response.author().isWithdrawn()).isTrue();
    }

    private GroupChallengeParticipant saveParticipant(Long groupId,
                                                      Long groupChallengeId,
                                                      User user) {

        GroupMember groupMember = groupMemberRepository.save(GroupMember.createMember(user.getId(),groupId));

        return participantRepository.save(
                GroupChallengeParticipant.join(groupMember.getId(), groupChallengeId)
        );
    }
}
