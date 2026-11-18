package com.mickaelgeronimo.webhookrelay;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "webhook.dispatcher.scheduling-enabled=false")
class WebhookRelayApplicationTests {

    @Test
    void contextLoads() {
    }
}
