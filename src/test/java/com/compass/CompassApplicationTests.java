package com.compass;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "jwt.access-secret=test-access-secret-key-for-compass-application-tests-1234567890",
        "jwt.refresh-secret=test-refresh-secret-key-for-compass-application-tests-1234567890",
        "jwt.access-expiration=3600000",
        "jwt.refresh-expiration=604800000"
})
class CompassApplicationTests {

    @Test
    void contextLoads() {
    }
}