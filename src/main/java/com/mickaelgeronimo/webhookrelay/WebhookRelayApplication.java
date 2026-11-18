package com.mickaelgeronimo.webhookrelay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Standard Webhooks compatible event delivery and retry engine.
 */
@SpringBootApplication
@EnableScheduling
public class WebhookRelayApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookRelayApplication.class, args);
    }
}
