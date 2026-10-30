package io.github.jhipster.sample.fde;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnterpriseClock {
    @Bean public Clock fdeClock() { return Clock.systemUTC(); }
}
