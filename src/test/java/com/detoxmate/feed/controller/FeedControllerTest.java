package com.detoxmate.feed.controller;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.challengerecord.domain.ChallengeRecord;
import com.detoxmate.challengerecord.repository.ChallengeRecordRepository;
import com.detoxmate.challengerecordstatuscount.repository.ChallengeRecordStatusCountRepository;
import com.detoxmate.group.domain.Group;
import com.detoxmate.group.domain.GroupChallenge;
import com.detoxmate.group.domain.GroupChallengeParticipant;
import com.detoxmate.group.domain.GroupMember;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import com.detoxmate.group.repository.GroupChallengeRepository;
import com.detoxmate.group.repository.GroupMemberRepository;
import com.detoxmate.group.repository.GroupRepository;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
class FeedControllerTest {

    private static final String HOME_FEED_URL = "/group-challenges/{groupChallengeId}/home";
    private static final String FEED_DETAIL_URL = "/challenge-records/{challengeRecordId}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

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

    @Test
    @DisplayName("GET /group-challenges/{groupChallengeId}/home — 홈 피드를 조회하면 참여자 카드와 challengeRecordId를 반환한다")
    void getHomeFeed_returnsMemberCardsWithChallengeRecordId() throws Exception {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(GroupChallenge.createFirst(group.getId()));

        User currentUser = userRepository.save(User.createNew("나"));
        User targetUser = userRepository.save(User.createNew("민준"));

        saveParticipant(group.getId(), challenge.getId(), currentUser);
        saveParticipant(group.getId(), challenge.getId(), targetUser);

        // when & then
        mockMvc.perform(get(HOME_FEED_URL, challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge.groupChallengeId").value(challenge.getId()))
                .andExpect(jsonPath("$.challenge.groupChallengeName").value("수능방"))
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.members[0].challengeRecordId", notNullValue()))
                .andExpect(jsonPath("$.members[0].challengeStatus").value("BEFORE_RECORD"))
                .andExpect(jsonPath("$.members[0].activityRecordId").doesNotExist())
                .andExpect(jsonPath("$.members[0].isPoked").value(false));

        assertThat(challengeRecordRepository.findAll()).hasSize(2);
        assertThat(statusCountRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("GET /group-challenges/{groupChallengeId}/home — 인증되지 않은 요청은 401을 반환한다")
    void getHomeFeed_unauthenticated_returns401() throws Exception {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(
                GroupChallenge.createFirst(group.getId())
        );

        // when & then
        mockMvc.perform(get(HOME_FEED_URL, challenge.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /challenge-records/{challengeRecordId} — 인증 전 피드 상세를 조회한다")
    void getFeedDetail_beforeRecord_returnsBeforeRecordDetail() throws Exception {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(
                GroupChallenge.createFirst(group.getId())
        );

        User currentUser = userRepository.save(User.createNew("나"));
        User author = userRepository.save(User.createNew("민준"));

        GroupChallengeParticipant authorParticipant =
                saveParticipant(group.getId(), challenge.getId(), author);

        ChallengeRecord challengeRecord = challengeRecordRepository.save(
                ChallengeRecord.create(
                        challenge.getId(),
                        authorParticipant.getId(),
                        LocalDate.now()
                )
        );

        statusCountRepository.save(
                com.detoxmate.challengerecordstatuscount.domain.ChallengeRecordStatusCount.create(
                        challengeRecord.getId()
                )
        );

        // when & then
        mockMvc.perform(get(FEED_DETAIL_URL, challengeRecord.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(currentUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challengeRecordId").value(challengeRecord.getId()))
                .andExpect(jsonPath("$.groupChallengeId").value(challenge.getId()))
                .andExpect(jsonPath("$.activityRecordId").doesNotExist())
                .andExpect(jsonPath("$.challengeStatus").value("BEFORE_RECORD"))
                .andExpect(jsonPath("$.author.userId").value(author.getId()))
                .andExpect(jsonPath("$.author.displayName").value("민준"))
                .andExpect(jsonPath("$.activityImageUrl").doesNotExist())
                .andExpect(jsonPath("$.oneLineReview").doesNotExist())
                .andExpect(jsonPath("$.reactions.totalCount").value(0))
                .andExpect(jsonPath("$.pokeable").value(true))
                .andExpect(jsonPath("$.poked").value(false))
                .andExpect(jsonPath("$.pokedUsers.length()").value(0));
    }

    @Test
    @DisplayName("GET /challenge-records/{challengeRecordId} — 인증되지 않은 요청은 401을 반환한다")
    void getFeedDetail_unauthenticated_returns401() throws Exception {
        // given
        Group group = groupRepository.save(Group.createNew("수능방", "ABCDE"));
        GroupChallenge challenge = groupChallengeRepository.save(
                GroupChallenge.createFirst(group.getId())
        );

        User author = userRepository.save(User.createNew("민준"));
        GroupChallengeParticipant participant =
                saveParticipant(group.getId(), challenge.getId(), author);

        ChallengeRecord challengeRecord = challengeRecordRepository.save(
                ChallengeRecord.create(
                        challenge.getId(),
                        participant.getId(),
                        LocalDate.now()
                )
        );

        // when & then
        mockMvc.perform(get(FEED_DETAIL_URL, challengeRecord.getId()))
                .andExpect(status().isUnauthorized());
    }

    private GroupChallengeParticipant saveParticipant(
            Long groupId,
            Long groupChallengeId,
            User user
    ) {
        GroupMember groupMember = groupMemberRepository.save(
                GroupMember.createMember(user.getId(), groupId)
        );

        return participantRepository.save(
                GroupChallengeParticipant.join(groupMember.getId(), groupChallengeId)
        );
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

}
