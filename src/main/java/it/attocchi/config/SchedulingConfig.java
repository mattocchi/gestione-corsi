package it.attocchi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Attività pianificate ({@code @Scheduled}), es. la sospensione notturna degli account inattivi. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
