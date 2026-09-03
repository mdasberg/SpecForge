package com.specforge.platform;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Shared beans every capability module may rely on. */
@Configuration
public class PlatformConfiguration {

    /**
     * Injected rather than called statically so a test can pin the clock instead of asserting
     * against wall time.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
