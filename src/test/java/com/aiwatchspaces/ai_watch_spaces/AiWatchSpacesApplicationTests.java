package com.aiwatchspaces.ai_watch_spaces;

import com.aiwatchspaces.ai_watch_spaces.ai.AiProvider;
import com.aiwatchspaces.ai_watch_spaces.ai.TestAiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
class AiWatchSpacesApplicationTests {

	@TestConfiguration
	static class TestAiConfig {

		@Bean
		@Primary
		AiProvider aiProvider() {
			return new TestAiProvider();
		}
	}

	@Test
	void contextLoads() {
	}
}