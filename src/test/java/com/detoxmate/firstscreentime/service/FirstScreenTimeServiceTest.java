package com.detoxmate.firstscreentime.service;

import com.detoxmate.common.exception.CustomException;
import com.detoxmate.common.exception.firstscreentime.FirstScreenTimeErrorCode;
import com.detoxmate.firstscreentime.domain.FirstScreenTime;
import com.detoxmate.firstscreentime.dto.FirstScreenTimeCreateRequest;
import com.detoxmate.firstscreentime.dto.FirstScreenTimeResponse;
import com.detoxmate.firstscreentime.repository.FirstScreenTimeRepository;
import com.detoxmate.group.repository.GroupChallengeParticipantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FirstScreenTimeServiceTest {

    private final FirstScreenTimeRepository firstScreenTimeRepository = mock(FirstScreenTimeRepository.class);
    private final GroupChallengeParticipantRepository participantRepository = mock(GroupChallengeParticipantRepository.class);
    private final FirstScreenTimeService firstScreenTimeService =
            new FirstScreenTimeService(firstScreenTimeRepository, participantRepository);

    @Test
    void 첫_스크린타임을_저장한다() {
        Long userId = 1L;
        Long participantId = 10L;
        FirstScreenTimeCreateRequest request = new FirstScreenTimeCreateRequest(
                participantId,
                210,
                LocalDate.of(2026, 6, 11)
        );

        when(participantRepository.existsActiveByIdAndUserId(participantId, userId)).thenReturn(true);
        when(firstScreenTimeRepository.existsByGroupChallengeParticipantId(participantId)).thenReturn(false);
        when(firstScreenTimeRepository.save(any(FirstScreenTime.class))).thenAnswer(invocation -> {
            FirstScreenTime firstScreenTime = invocation.getArgument(0);
            ReflectionTestUtils.setField(firstScreenTime, "id", 1L);
            ReflectionTestUtils.setField(firstScreenTime, "createdAt", LocalDateTime.of(2026, 6, 11, 10, 0));
            return firstScreenTime;
        });

        FirstScreenTimeResponse response = firstScreenTimeService.create(userId, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.groupChallengeParticipantId()).isEqualTo(participantId);
        assertThat(response.screenTimeMinutes()).isEqualTo(210);
        assertThat(response.recordDate()).isEqualTo(LocalDate.of(2026, 6, 11));
    }

    @Test
    void 첫_스크린타임이_이미_있으면_중복_저장을_거부한다() {
        Long userId = 1L;
        Long participantId = 10L;
        FirstScreenTimeCreateRequest request = new FirstScreenTimeCreateRequest(
                participantId,
                210,
                LocalDate.of(2026, 6, 11)
        );

        when(participantRepository.existsActiveByIdAndUserId(participantId, userId)).thenReturn(true);
        when(firstScreenTimeRepository.existsByGroupChallengeParticipantId(participantId)).thenReturn(true);

        assertThatThrownBy(() -> firstScreenTimeService.create(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(FirstScreenTimeErrorCode.FIRST_SCREEN_TIME_ALREADY_REGISTER);
        verify(firstScreenTimeRepository, never()).save(any());
    }
}
