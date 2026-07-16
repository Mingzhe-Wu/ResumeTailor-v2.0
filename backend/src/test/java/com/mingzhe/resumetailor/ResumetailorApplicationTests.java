package com.mingzhe.resumetailor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mingzhe.resumetailor.redis.RedisCacheService;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ResumetailorApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RedisCacheService redisCacheService;

	@Test
	void contextLoads() {
	}

	@Test
	void flywayMigrationsInstallPgvector() {
		Integer successfulMigrations = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE",
				Integer.class
		);
		Boolean vectorInstalled = jdbcTemplate.queryForObject(
				"SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')",
				Boolean.class
		);

		assertTrue(successfulMigrations != null && successfulMigrations > 0);
		assertEquals(Boolean.TRUE, vectorInstalled);
	}

	@Test
	void redisStoresAndRetrievesCachedValue() {
		String key = "ci:test:context";
		redisCacheService.set(key, "available", Duration.ofMinutes(1));

		try {
			assertEquals("available", redisCacheService.get(key));
		} finally {
			redisCacheService.delete(key);
		}
	}

}
