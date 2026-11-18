package com.mickaelgeronimo.webhookrelay.service;

import com.mickaelgeronimo.webhookrelay.domain.BackoffPolicy;
import com.mickaelgeronimo.webhookrelay.security.StandardWebhookSigner;
import com.mickaelgeronimo.webhookrelay.security.WebhookUrlValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;

/**
 * Spring configuration wiring core cryptographic, SSRF, clock and HTTP client beans.
 */
@Configuration
@EnableConfigurationProperties(WebhookProperties.class)
public class WebhookBeansConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock webhookClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public WebhookUrlValidator.HostResolver webhookHostResolver() {
        return host -> Arrays.asList(InetAddress.getAllByName(host));
    }

    @Bean
    public StandardWebhookSigner standardWebhookSigner(Clock webhookClock, WebhookProperties properties) {
        return new StandardWebhookSigner(
                webhookClock,
                Duration.ofSeconds(properties.getSecurity().getSignatureToleranceSeconds())
        );
    }

    @Bean
    public WebhookUrlValidator webhookUrlValidator(
            WebhookProperties properties,
            WebhookUrlValidator.HostResolver webhookHostResolver
    ) {
        return new WebhookUrlValidator(properties.getSecurity().isAllowPrivateTargets(), webhookHostResolver);
    }

    @Bean
    public BackoffPolicy backoffPolicy(WebhookProperties properties) {
        return new BackoffPolicy(
                Duration.ofSeconds(properties.getDispatcher().getBaseRetryDelaySeconds()),
                Duration.ofSeconds(properties.getDispatcher().getMaxRetryDelaySeconds())
        );
    }

    @Bean
    public RestClient webhookRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
