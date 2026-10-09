package com.detoxmate.transferminute.controller;

import com.detoxmate.support.UserFixtures;
import com.detoxmate.applock.domain.TimeLimit;
import com.detoxmate.auth.JwtTokenProvider;
import com.detoxmate.user.domain.User;
import com.detoxmate.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static com.epages.restdocs.apispec.Schema.schema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ExtendWith(RestDocumentationExtension.class)
class TransferPhraseControllerTest {

    private static final String URL = "/transfer-phrases/random";

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation)).build();
        jdbcTemplate.update("DELETE FROM transfer_phrase");
        user = userRepository.saveAndFlush(UserFixtures.createUser("transfer-phrase-reader"));
    }

    @Test
    @DisplayName("명언 원문 하나를 반환하고 문구 목록과 사용자의 시간 설정을 보존한다")
    void get_returnsStoredPhraseWithoutChangingCatalogOrTimeLimit() throws Exception {
        // given
        String phrase = "  우리가 반복해서 하는 행동이 곧 우리 자신이다.\n-윌 듀랜트  ";
        insertPhrase(101L, phrase);
        entityManager.persist(TimeLimit.create(user, 90));
        entityManager.flush();
        entityManager.clear();
        List<Map<String, Object>> catalogBefore = catalogRows();
        List<Map<String, Object>> timeLimitsBefore = timeLimitRows();

        // when
        ResultActions result = authenticatedGet();

        // then
        expectPhrase(result, phrase)
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andDo(document("transfer-phrases/random",
                        requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token")),
                        responseHeaders(headerWithName(HttpHeaders.CACHE_CONTROL).description("no-store: 응답 캐시 금지")),
                        responseFields(fieldWithPath("phrase").type(STRING).description("DB에 저장된 다짐 문구 원문")),
                        resource(builder().tag("Transfer Phrase").summary("랜덤 다짐 문구 조회")
                                .description("DB에 수동 등록된 문구 중 하나를 무작위로 반환한다. 연속 중복이 가능하며, 문구가 없으면 404를 반환한다. 성공 응답은 캐시하지 않는다.")
                                .requestHeaders(headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer access token"))
                                .responseSchema(schema("TransferPhraseResponse"))
                                .responseFields(fieldWithPath("phrase").type(STRING).description("DB에 저장된 다짐 문구 원문"))
                                .build())));
        entityManager.flush();
        entityManager.clear();
        assertThat(catalogRows()).isEqualTo(catalogBefore);
        assertThat(timeLimitRows()).isEqualTo(timeLimitsBefore);
    }

    @Test
    @DisplayName("문구가 하나이면 반복 조회해도 같은 문구를 반환하며 소모하지 않는다")
    void get_returnsSamePhraseOnRepeatedRequestsWhenOnlyOneExists() throws Exception {
        // given
        String phrase = "오늘 할 수 있는 일을 내일로 미루지 마라. -벤저민 프랭클린";
        insertPhrase(307L, phrase);
        List<Map<String, Object>> catalogBefore = catalogRows();

        // when & then
        expectPhrase(authenticatedGet(), phrase);
        expectPhrase(authenticatedGet(), phrase);
        entityManager.flush();
        entityManager.clear();
        assertThat(catalogRows()).isEqualTo(catalogBefore);
        assertThat(timeLimitRows()).isEmpty();
    }

    @Test
    @DisplayName("문구 ID가 연속되지 않아도 저장된 여러 문구 중 하나만 반환한다")
    void get_returnsOneStoredPhraseWhenIdsAreSparse() throws Exception {
        // given
        List<String> phrases = List.of("적어도 시도해 보자. 노력하면 잃을 게 없다. -손흥민",
                "속도가 느려도 멈추지만 않으면 괜찮다. -공자",
                "훈련의 고통은 잠시지만, 후회의 고통은 오래간다. -짐 론");
        insertPhrase(101L, phrases.get(0));
        insertPhrase(207L, phrases.get(1));
        insertPhrase(9001L, phrases.get(2));
        List<Map<String, Object>> catalogBefore = catalogRows();

        // when
        String body = authenticatedGet().andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // then
        JsonNode response = objectMapper.readTree(body);
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.has("phrase")).isTrue();
        assertThat(response.get("phrase").isTextual()).isTrue();
        assertThat(response.get("phrase").textValue()).isIn(phrases);
        entityManager.flush();
        entityManager.clear();
        assertThat(catalogRows()).isEqualTo(catalogBefore);
    }

    @Test
    @DisplayName("등록된 문구가 없으면 공통 404 오류를 반환한다")
    void get_returnsNotFoundWhenCatalogIsEmpty() throws Exception {
        // when & then
        String body = authenticatedGet().andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(body)).isEqualTo(objectMapper.readTree(
                "{\"code\":\"NOT_FOUND\",\"message\":\"Not Found\",\"status\":404}"));
        assertThat(catalogRows()).isEmpty();
    }

    @Test
    @DisplayName("운영자가 문구 목록을 교체하면 다음 조회에 현재 저장된 문구를 반환한다")
    void get_readsCurrentCatalogOnEachRequest() throws Exception {
        // given
        insertPhrase(101L, "우리는 시간이 짧은 게 아니라, 시간을 많이 낭비하는 것이다. -세네카");
        expectPhrase(authenticatedGet(), "우리는 시간이 짧은 게 아니라, 시간을 많이 낭비하는 것이다. -세네카");
        jdbcTemplate.update("DELETE FROM transfer_phrase");
        String replacement = "시작하는 방법은 말을 멈추고 행동하는 것이다. -월트 디즈니";
        insertPhrase(207L, replacement);
        entityManager.clear();

        // when & then
        expectPhrase(authenticatedGet(), replacement);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "malformed", "unknown-user", "withdrawn-user"})
    @DisplayName("유효한 인증 사용자가 없으면 401을 반환하고 문구를 보존한다")
    void get_requiresExistingActiveAuthenticatedUser(String credential) throws Exception {
        // given
        insertPhrase(101L, "자기 자신을 다스리지 못하는 사람은 자유롭지 않다. -에픽테토스");
        List<Map<String, Object>> catalogBefore = catalogRows();
        MockHttpServletRequestBuilder request = get(URL);
        if (credential.equals("malformed")) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer malformed-token");
        } else if (credential.equals("unknown-user")) {
            request.header(HttpHeaders.AUTHORIZATION, bearer(Long.MAX_VALUE));
        } else if (credential.equals("withdrawn-user")) {
            User withdrawn = UserFixtures.createUser("withdrawn-phrase-reader");
            withdrawn.withdraw();
            request.header(HttpHeaders.AUTHORIZATION, bearer(userRepository.saveAndFlush(withdrawn).getId()));
        }

        // when & then
        String body = mockMvc.perform(request).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(body)).isEqualTo(objectMapper.readTree(
                "{\"code\":\"UNAUTHORIZED\",\"message\":\"Unauthorized\",\"status\":401}"));
        entityManager.flush();
        entityManager.clear();
        assertThat(catalogRows()).isEqualTo(catalogBefore);
    }

    private ResultActions authenticatedGet() throws Exception {
        return mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(user.getId())));
    }

    private ResultActions expectPhrase(ResultActions result, String phrase) throws Exception {
        return result.andExpect(status().isOk()).andExpect(response -> assertThat(
                        objectMapper.readTree(response.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(objectMapper.valueToTree(Map.of("phrase", phrase))));
    }

    private void insertPhrase(long id, String phrase) {
        jdbcTemplate.update("INSERT INTO transfer_phrase (id, transfer_phrase) VALUES (?, ?)", id, phrase);
    }

    private List<Map<String, Object>> catalogRows() {
        return jdbcTemplate.queryForList("SELECT id, transfer_phrase FROM transfer_phrase ORDER BY id");
    }

    private List<Map<String, Object>> timeLimitRows() {
        return jdbcTemplate.queryForList("SELECT * FROM time_limits ORDER BY time_limit_id");
    }

    private String bearer(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }
}
