package com.detoxmate.applock.controller;

import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ExtendWith(RestDocumentationExtension.class)
class TimeLimitControllerTest {

    private static final String URL = "/me/time-limit";

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private Long ownerId;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation)).build();
        ownerId = userRepository.save(User.createNew("time-limit-owner")).getId();
    }

    @Test
    @DisplayName("사용자의 시간을 수정하고 재전송해도 현재값을 가진 한 행만 저장한다")
    void put_replacesCurrentUsersLimitWithoutCreatingAnotherRow() throws Exception {
        // given
        expectMinutes(perform("PUT", ownerId, minutes(120)), 120)
                .andDo(document("time-limits/set",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")),
                        requestFields(fieldWithPath("totalLockMinutes").type(NUMBER).description("총 잠금 설정 시간(분), 0~1440")),
                        responseFields(fieldWithPath("totalLockMinutes").type(NUMBER).description("저장된 총 잠금 설정 시간(분)")),
                        resource(builder().tag("Time Limit").summary("내 총 잠금 시간 저장")
                                .description("인증 사용자의 현재 시간을 생성하거나 교체한다. 같은 값을 재전송해도 누적하지 않는다.")
                                .requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token"))
                                .requestSchema(schema("TimeLimitRequest"))
                                .responseSchema(schema("TimeLimitResponse"))
                                .requestFields(fieldWithPath("totalLockMinutes").type(NUMBER).description("총 잠금 설정 시간(분), 0~1440"))
                                .responseFields(fieldWithPath("totalLockMinutes").type(NUMBER).description("저장된 총 잠금 설정 시간(분)"))
                                .build())));

        // when
        expectMinutes(perform("PUT", ownerId, minutes(60)), 60);
        expectMinutes(perform("PUT", ownerId, minutes(60)), 60);
        entityManager.flush();
        entityManager.clear();

        // then
        expectMinutes(perform("GET", ownerId, null), 60)
                .andDo(document("time-limits/get",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")),
                        responseFields(fieldWithPath("totalLockMinutes").type(NUMBER).description("저장된 총 잠금 설정 시간(분)")),
                        resource(builder().tag("Time Limit").summary("내 총 잠금 시간 조회")
                                .description("인증 사용자의 현재 설정을 조회한다. 미설정 시 404를 반환한다.")
                                .requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token"))
                                .responseSchema(schema("TimeLimitResponse"))
                                .responseFields(fieldWithPath("totalLockMinutes").type(NUMBER).description("저장된 총 잠금 설정 시간(분)"))
                                .build())));
        assertThat(countLimits(ownerId)).isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1440})
    @DisplayName("0분과 1440분을 저장하고 조회할 수 있다")
    void put_acceptsMinuteBoundaries(int value) throws Exception {
        expectMinutes(perform("PUT", ownerId, minutes(value)), value);
        expectMinutes(perform("GET", ownerId, null), value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"totalLockMinutes\":null}", "{\"totalLockMinutes\":-1}",
            "{\"totalLockMinutes\":1441}"})
    @DisplayName("시간이 누락되거나 허용 범위 밖이면 400이고 기존 설정을 보존한다")
    void put_rejectsInvalidMinutesAndPreservesStoredValue(String body) throws Exception {
        // given
        expectMinutes(perform("PUT", ownerId, minutes(60)), 60);

        // when & then
        perform("PUT", ownerId, body).andExpect(status().isBadRequest());
        expectMinutes(perform("GET", ownerId, null), 60);
        assertThat(countLimits(ownerId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("시간을 설정하지 않은 사용자는 조회 시 404를 받는다")
    void get_returnsNotFoundWhenUnset() throws Exception {
        perform("GET", ownerId, null).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("시간 삭제는 본인의 설정만 제거하며 사용자와 다른 사용자의 설정을 보존한다")
    void delete_removesOnlyCurrentUsersLimit() throws Exception {
        // given
        Long otherId = userRepository.save(User.createNew("time-limit-other")).getId();
        expectMinutes(perform("PUT", ownerId, minutes(60)), 60);
        perform("GET", otherId, null).andExpect(status().isNotFound());
        expectMinutes(perform("PUT", otherId, minutes(120)), 120);
        expectMinutes(perform("GET", ownerId, null), 60);

        // when
        perform("DELETE", ownerId, null).andExpect(status().isNoContent())
                .andDo(document("time-limits/delete",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")),
                        resource(builder().tag("Time Limit").summary("내 총 잠금 시간 삭제")
                                .description("인증 사용자의 총 잠금 시간 설정을 삭제한다.")
                                .requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token"))
                                .build())));
        entityManager.flush();
        entityManager.clear();

        // then
        perform("GET", ownerId, null).andExpect(status().isNotFound());
        expectMinutes(perform("GET", otherId, null), 120);
        assertThat(countLimits(ownerId)).isZero();
        assertThat(countLimits(otherId)).isEqualTo(1L);
        assertThat(userRepository.findById(ownerId)).isPresent();
    }

    @Test
    @DisplayName("미설정 상태에서 반복 삭제해도 204이며 설정을 생성하지 않는다")
    void delete_isIdempotentWhenUnset() throws Exception {
        perform("DELETE", ownerId, null).andExpect(status().isNoContent());
        perform("DELETE", ownerId, null).andExpect(status().isNoContent());
        perform("GET", ownerId, null).andExpect(status().isNotFound());
        assertThat(countLimits(ownerId)).isZero();
    }

    @ParameterizedTest
    @CsvSource({"GET, missing", "PUT, missing", "DELETE, missing", "GET, malformed", "PUT, malformed",
            "DELETE, malformed", "GET, unknown-user", "PUT, unknown-user", "DELETE, unknown-user",
            "GET, withdrawn-user", "PUT, withdrawn-user", "DELETE, withdrawn-user"})
    @DisplayName("유효한 인증 사용자가 없으면 401이고 기존 설정을 보존한다")
    void endpoints_requireExistingAuthenticatedUser(String method, String credential) throws Exception {
        // given
        expectMinutes(perform("PUT", ownerId, minutes(60)), 60);
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), URL)
                .contentType(MediaType.APPLICATION_JSON).content(minutes(120));
        if (credential.equals("malformed")) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer malformed-token");
        } else if (credential.equals("unknown-user")) {
            builder.header(HttpHeaders.AUTHORIZATION, bearer(Long.MAX_VALUE));
        } else if (credential.equals("withdrawn-user")) {
            User withdrawn = User.createNew("withdrawn-limit-user");
            withdrawn.withdraw();
            builder.header(HttpHeaders.AUTHORIZATION, bearer(userRepository.saveAndFlush(withdrawn).getId()));
        }

        // when & then
        mockMvc.perform(builder).andExpect(status().isUnauthorized());
        expectMinutes(perform("GET", ownerId, null), 60);
        assertThat(countLimits(ownerId)).isEqualTo(1L);
    }

    @ParameterizedTest
    @CsvSource({"POST, /me/apps", "GET, /me/apps", "GET, /me/apps/1", "PUT, /me/apps/1", "DELETE, /me/apps/1"})
    @DisplayName("제거된 앱별 등록 및 관리 API는 404를 반환한다")
    void legacyAppEndpoints_areUnavailable(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + ownerId + ",\"appDisplayName\":\"Instagram\",\"dailyLimitMinutes\":60}"))
                .andExpect(status().isNotFound());
    }

    private ResultActions perform(String method, Long userId, String body) throws Exception {
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), URL)
                .header(HttpHeaders.AUTHORIZATION, bearer(userId));
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder);
    }

    private ResultActions expectMinutes(ResultActions result, int value) throws Exception {
        return result.andExpect(status().isOk()).andExpect(response -> assertThat(
                objectMapper.readTree(response.getResponse().getContentAsString()))
                .isEqualTo(objectMapper.readTree(minutes(value))));
    }

    private long countLimits(Long userId) {
        entityManager.flush();
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM time_limits WHERE user_id = :userId")
                .setParameter("userId", userId).getSingleResult()).longValue();
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    private String minutes(int value) {
        return "{\"totalLockMinutes\":" + value + "}";
    }
}
