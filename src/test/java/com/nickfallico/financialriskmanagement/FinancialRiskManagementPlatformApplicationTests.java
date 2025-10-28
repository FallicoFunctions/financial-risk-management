package com.nickfallico.financialriskmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
        // 1. Don't try to spin up Redis-backed cache
        "spring.cache.type=NONE",
        // 2. Don't auto-configure Redis at all
        "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    }
)
class FinancialRiskManagementPlatformApplicationTests {

	@Test
	void contextLoads() {
	}

}
