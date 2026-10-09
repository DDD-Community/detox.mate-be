package com.detoxmate.friend.dto;

import com.detoxmate.user.dto.MyPageResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FriendInviteContractSchemaTest {

    record Contract(Class<?> responseType, Set<String> fields, Set<String> nullableFields) {
    }

    static Stream<Contract> responseContracts() {
        return Stream.of(
                new Contract(MyPageResponse.class,
                        Set.of("id", "displayName", "userCode", "profileImageUrl", "pushNotificationEnabled"),
                        Set.of("profileImageUrl")),
                new Contract(FriendListUserResponse.class,
                        Set.of("userId", "displayName", "userCode", "profileImageUrl", "relationshipStatus", "requestId"),
                        Set.of("profileImageUrl", "requestId")),
                new Contract(FriendInviteResponse.class, Set.of("code", "userCode"), Set.of()),
                new Contract(FriendResponse.class, Set.of("friendshipId", "user", "acceptedAt"), Set.of()),
                new Contract(FriendReceivedRequestResponse.class, Set.of("requestId", "user", "createdAt"), Set.of()));
    }

    @ParameterizedTest
    @MethodSource("responseContracts")
    @DisplayName("응답 스키마의 모든 필드는 필수이며 실제 null 허용 필드만 nullable이다")
    void responseSchema_matchesRequiredAndNullableContract(Contract contract) {
        // when
        Schema<?> schema = ModelConverters.getInstance().readAll(contract.responseType())
                .get(contract.responseType().getSimpleName());

        // then
        assertThat(schema.getProperties().keySet()).containsExactlyInAnyOrderElementsOf(contract.fields());
        assertThat(schema.getRequired()).containsExactlyInAnyOrderElementsOf(contract.fields());
        contract.fields().forEach(field -> assertThat(Boolean.TRUE.equals(schema.getProperties().get(field).getNullable()))
                .as("%s.%s nullable", contract.responseType().getSimpleName(), field)
                .isEqualTo(contract.nullableFields().contains(field)));
    }
}
