package com.detoxmate.config;

import com.detoxmate.common.error.GlobalExceptionHandler;
import com.detoxmate.user.controller.AuthController;
import com.detoxmate.user.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DevCorsConfigTest {

    @Test
    void dev_cors_설정은_localhost_3000_preflight를_허용한다() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(options("/auth/social/kakao")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }

    @Test
    void dev_cors_설정은_example_com_preflight를_거부한다() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(options("/auth/social/kakao")
                        .header(HttpHeaders.ORIGIN, "http://example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    private MockMvc buildMockMvc() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestWebConfig.class);
        context.refresh();
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Configuration
    @EnableWebMvc
    static class TestWebConfig {

        @Bean
        DevCorsConfig devCorsConfig() {
            return new DevCorsConfig();
        }

        @Bean
        AuthService authService() {
            return mock(AuthService.class);
        }

        @Bean
        AuthController authController(AuthService authService) {
            return new AuthController(authService);
        }

        @Bean
        GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }
    }
}
