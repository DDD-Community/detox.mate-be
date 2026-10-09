package com.detoxmate.notification.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class FriendUnlockRecipientSelectorTest {

    private final FriendUnlockRecipientSelector selector = new FriendUnlockRecipientSelector();

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 5})
    @DisplayName("친구 후보가 세 명 이하면 전원, 많으면 서로 다른 세 명을 선택한다")
    void select_returnsUpToThreeDistinctCandidates(int count) {
        // given
        List<Long> candidates = LongStream.rangeClosed(1, count).boxed().toList();

        // when
        List<Long> selected = selector.select(candidates);

        // then
        assertThat(selected).hasSize(Math.min(3, count)).doesNotHaveDuplicates().isSubsetOf(candidates);
        if (count <= 3) {
            assertThat(selected).containsExactlyInAnyOrderElementsOf(candidates);
        }
    }

    @Test
    @DisplayName("중복 후보는 한 명으로 계산하고 전달받은 목록을 변경하지 않는다")
    void select_deduplicatesRecipientsWithoutChangingInput() {
        // given
        List<Long> candidates = List.of(1L, 2L, 1L, 3L, 2L);

        // when
        List<Long> selected = selector.select(candidates);

        // then
        assertThat(selected).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(candidates).containsExactly(1L, 2L, 1L, 3L, 2L);
    }
}
