package com.detoxmate.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "firebase.enabled = false")
@ActiveProfiles("test")
class FirebaseConfigTest {

    @Test
    @DisplayName("Firebase 사용 불가여도 기본 실행은 보장한다.")
    void contextLoads_whenFirebaseDisabled(){

    }

}