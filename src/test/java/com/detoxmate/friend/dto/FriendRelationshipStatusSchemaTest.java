package com.detoxmate.friend.dto;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class FriendRelationshipStatusSchemaTest {

    @ParameterizedTest
    @ValueSource(classes = {FriendSearchResponse.class, FriendUserResponse.class,
            FriendListUserResponse.class, FriendInviteeResponse.class, FriendProfileResponse.class})
    void relationshipStatusUsesTheSharedEnumSchema(Class<?> responseType) {
        var schemas = ModelConverters.getInstance().readAll(responseType);
        Schema<?> responseSchema = schemas.get(responseType.getSimpleName());
        Schema<?> relationshipStatus = responseSchema.getProperties().get("relationshipStatus");

        assertThat(relationshipStatus.get$ref())
                .isEqualTo("#/components/schemas/FriendRelationshipStatus");
        assertThat(relationshipStatus.getType()).isNull();
        assertThat(relationshipStatus.getEnum()).isNull();

        var expectedValues = Arrays.stream(FriendRelationshipStatus.values()).map(Enum::name).toList();
        Schema<?> commonStatus = schemas.get("FriendRelationshipStatus");
        assertThat(commonStatus.getType()).isEqualTo("string");
        assertThat(commonStatus.getEnum()).isEqualTo(expectedValues);
        assertThat(schemas.values().stream()
                .filter(schema -> expectedValues.equals(schema.getEnum())).count()).isEqualTo(1);
    }
}
