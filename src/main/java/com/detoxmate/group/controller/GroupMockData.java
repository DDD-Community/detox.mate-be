package com.detoxmate.group.controller;

import com.detoxmate.group.dto.GroupChallengeParticipantResponse;
import com.detoxmate.group.dto.GroupChallengeResponse;
import com.detoxmate.group.dto.GroupChallengeSummaryResponse;
import com.detoxmate.group.dto.GroupMemberResponse;
import com.detoxmate.group.dto.GroupResponse;

import java.time.LocalDateTime;
import java.util.List;

public final class GroupMockData {

    private static final String INVITE_CODE = "AB123";
    private static final LocalDateTime GROUP_CREATED_AT = LocalDateTime.parse("2026-04-19T10:00:00");
    private static final LocalDateTime SECOND_MEMBER_JOINED_AT = LocalDateTime.parse("2026-04-19T10:30:00");
    private static final LocalDateTime ACTIVE_CHALLENGE_START_AT = LocalDateTime.parse("2026-04-20T09:00:00");
    private static final LocalDateTime ACTIVE_CHALLENGE_END_AT = LocalDateTime.parse("2026-04-27T09:00:00");

    private GroupMockData() {
    }

    public static GroupResponse createGroupResponse(String name) {
        return new GroupResponse(
                1L,
                INVITE_CODE,
                name,
                "OWNER",
                List.of(ownerMember()),
                recruitingChallengeSummary(10L),
                GROUP_CREATED_AT,
                GROUP_CREATED_AT
        );
    }

    static GroupResponse joinGroupResponse(String inviteCode) {
        return new GroupResponse(
                1L,
                inviteCode,
                "주말 디톡스",
                "MEMBER",
                List.of(ownerMember(), memberParticipant()),
                recruitingChallengeSummary(10L),
                GROUP_CREATED_AT,
                SECOND_MEMBER_JOINED_AT
        );
    }

    static List<GroupResponse> myGroupsResponse() {
        return List.of(new GroupResponse(
                1L,
                INVITE_CODE,
                "주말 디톡스",
                "OWNER",
                List.of(ownerMember()),
                activeChallengeSummary(10L),
                GROUP_CREATED_AT,
                ACTIVE_CHALLENGE_START_AT
        ));
    }

    static GroupResponse groupDetailResponse(long id) {
        return new GroupResponse(
            id,
            INVITE_CODE,
            "주말 디톡스",
                "OWNER",
                List.of(ownerMember(), memberParticipant()),
                activeChallengeSummary(10L),
                GROUP_CREATED_AT,
                ACTIVE_CHALLENGE_START_AT
        );
    }

    static List<GroupChallengeResponse> myGroupChallengesResponse(String status) {
        String challengeStatus = status == null || status.isBlank() ? "ACTIVE" : status;

        return List.of(new GroupChallengeResponse(
                10L,
                1L,
                "주말 디톡스",
                1,
                challengeStatus,
                List.of(ownerChallengeParticipant(), memberChallengeParticipant()),
                ACTIVE_CHALLENGE_START_AT,
                ACTIVE_CHALLENGE_END_AT,
                GROUP_CREATED_AT,
                ACTIVE_CHALLENGE_START_AT
        ));
    }

    static GroupChallengeResponse groupChallengeDetailResponse(long id) {
        return new GroupChallengeResponse(
                id,
                1L,
                "주말 디톡스",
                1,
                "ACTIVE",
                List.of(ownerChallengeParticipant(), memberChallengeParticipant()),
                ACTIVE_CHALLENGE_START_AT,
                ACTIVE_CHALLENGE_END_AT,
                GROUP_CREATED_AT,
                ACTIVE_CHALLENGE_START_AT
        );
    }

    private static GroupMemberResponse ownerMember() {
        return new GroupMemberResponse(
                100L,
                1L,
                "지민",
                "https://...",
                "OWNER",
                "ACTIVE",
                GROUP_CREATED_AT,
                null
        );
    }

    private static GroupMemberResponse memberParticipant() {
        return new GroupMemberResponse(
                101L,
                2L,
                "민수",
                null,
                "MEMBER",
                "ACTIVE",
                SECOND_MEMBER_JOINED_AT,
                null
        );
    }

    private static GroupChallengeSummaryResponse recruitingChallengeSummary(long id) {
        return new GroupChallengeSummaryResponse(
                id,
                1,
                "RECRUITING",
                null,
                null
        );
    }

    private static GroupChallengeSummaryResponse activeChallengeSummary(long id) {
        return new GroupChallengeSummaryResponse(
                id,
                1,
                "ACTIVE",
                ACTIVE_CHALLENGE_START_AT,
                ACTIVE_CHALLENGE_END_AT
        );
    }

    private static GroupChallengeParticipantResponse ownerChallengeParticipant() {
        return new GroupChallengeParticipantResponse(
                1000L,
                100L,
                1L,
                "지민",
                "https://...",
                "JOINED",
                GROUP_CREATED_AT,
                null,
                List.of()
        );
    }

    private static GroupChallengeParticipantResponse memberChallengeParticipant() {
        return new GroupChallengeParticipantResponse(
                1001L,
                101L,
                2L,
                "민수",
                null,
                "JOINED",
                SECOND_MEMBER_JOINED_AT,
                null,
                List.of()
        );
    }
}
