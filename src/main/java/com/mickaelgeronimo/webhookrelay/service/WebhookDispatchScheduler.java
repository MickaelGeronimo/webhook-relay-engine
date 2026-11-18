package com.mickaelgeronimo.webhookrelay.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background poller triggering due webhook deliveries when scheduling is enabled.
 */
@Component
@ConditionalOnProperty(value = "webhook.dispatcher.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class WebhookDispatchScheduler {

    private final WebhookDispatcherService dispatcherService;

    public WebhookDispatchScheduler(WebhookDispatcherService dispatcherService) {
        this.dispatcherService = dispatcherService;
    }

    @Scheduled(fixedDelayString = "${webhook.dispatcher.fixed-delay-ms:5000}")
    public void pollAndDispatch() {
        dispatcherService.dispatchDueDeliveries();
    }
}
