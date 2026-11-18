package com.mickaelgeronimo.webhookrelay.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mickaelgeronimo.webhookrelay.domain.DeliveryAttemptRepository;
import com.mickaelgeronimo.webhookrelay.domain.WebhookDeliveryRepository;
import com.mickaelgeronimo.webhookrelay.domain.WebhookEndpointRepository;
import com.mickaelgeronimo.webhookrelay.domain.WebhookEventRepository;
import com.mickaelgeronimo.webhookrelay.security.WebhookUrlValidator;
import com.mickaelgeronimo.webhookrelay.service.TransportResult;
import com.mickaelgeronimo.webhookrelay.service.WebhookTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "webhook.dispatcher.scheduling-enabled=false",
        "webhook.dispatcher.base-retry-delay-seconds=2",
        "webhook.dispatcher.max-retry-delay-seconds=10"
})
@AutoConfigureMockMvc
class WebhookRelayIntegrationTest {

    @TestConfiguration
    static class DeterministicTestBeans {

        @Bean
        @Primary
        MutableTestClock testClock() {
            return new MutableTestClock(Instant.parse("2026-11-18T12:00:00Z"));
        }

        @Bean
        @Primary
        WebhookUrlValidator.HostResolver testHostResolver() {
            return host -> switch (host) {
                case "hooks.partner.test" -> List.of(
                        InetAddress.getByAddress(host, new byte[]{(byte) 93, (byte) 184, (byte) 216, 34}));
                case "metadata.internal" -> List.of(
                        InetAddress.getByAddress(host, new byte[]{(byte) 169, (byte) 254, (byte) 169, (byte) 254}));
                default -> List.of(InetAddress.getLoopbackAddress());
            };
        }
    }

    static final class MutableTestClock extends Clock {
        private final AtomicReference<Instant> current;

        MutableTestClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }

        void advance(Duration duration) {
            current.updateAndGet(t -> t.plus(duration));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MutableTestClock testClock;

    @Autowired
    private DeliveryAttemptRepository attemptRepository;

    @Autowired
    private WebhookDeliveryRepository deliveryRepository;

    @Autowired
    private WebhookEventRepository eventRepository;

    @Autowired
    private WebhookEndpointRepository endpointRepository;

    @MockBean
    private WebhookTransport mockTransport;

    @BeforeEach
    void cleanup() {
        attemptRepository.deleteAll();
        deliveryRepository.deleteAll();
        eventRepository.deleteAll();
        endpointRepository.deleteAll();
        reset(mockTransport);
    }

    @Test
    void enforcesSsrfProtectionAndIdempotencyAndRetryToDlqReplayWithDualSecretSigning() throws Exception {
        // 1. SSRF protection blocks loopback and cloud metadata URLs
        mockMvc.perform(post("/api/v1/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "pay-core",
                                  "targetUrl": "http://metadata.internal/latest/meta-data",
                                  "eventTypes": ["payment.settled"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Unsafe webhook target URL"));

        // 2. Register valid public subscriber endpoint (maxRetries = 2)
        MvcResult epResult = mockMvc.perform(post("/api/v1/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "pay-core",
                                  "targetUrl": "https://hooks.partner.test/webhooks/payments",
                                  "eventTypes": ["payment.settled"],
                                  "maxRetries": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        JsonNode epJson = objectMapper.readTree(epResult.getResponse().getContentAsString());
        String endpointId = epJson.get("id").asText();
        String originalSecret = epJson.get("signingSecret").asText();
        assertThat(originalSecret).startsWith("whsec_");

        // Rotate secret so deliveries carry dual signatures (new + previous)
        MvcResult rotateResult = mockMvc.perform(post("/api/v1/endpoints/{id}/rotate-secret", endpointId))
                .andExpect(status().isOk())
                .andReturn();
        String rotatedSecret = objectMapper.readTree(rotateResult.getResponse().getContentAsString())
                .get("signingSecret").asText();
        assertThat(rotatedSecret).isNotEqualTo(originalSecret);

        // 3. Ingest event idempotently
        String eventBody = """
                {
                  "tenantId": "pay-core",
                  "eventType": "payment.settled",
                  "idempotencyKey": "pix-tx-9901",
                  "payload": "{\\"transactionId\\":\\"tx_9901\\",\\"amountCents\\":12500}"
                }
                """;

        MvcResult firstIngest = mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.deduplicated").value(false))
                .andExpect(jsonPath("$.deliveryIds.length()").value(1))
                .andReturn();

        JsonNode ingestJson = objectMapper.readTree(firstIngest.getResponse().getContentAsString());
        String eventId = ingestJson.get("eventId").asText();
        String deliveryId = ingestJson.get("deliveryIds").get(0).asText();

        // Duplicate idempotency key returns 200 OK with deduplicated=true and same deliveryId
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deduplicated").value(true))
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.deliveryIds[0]").value(deliveryId));

        // 4. First delivery fails with 503 -> transitions to RETRYING
        when(mockTransport.deliver(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(TransportResult.httpFailure(503, "HTTP 503: Service Unavailable", true));

        mockMvc.perform(post("/api/v1/deliveries/dispatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(1));

        mockMvc.perform(get("/api/v1/deliveries/{id}", deliveryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETRYING"))
                .andExpect(jsonPath("$.attemptCount").value(1))
                .andExpect(jsonPath("$.lastHttpStatus").value(503));

        // 5. Advance clock past backoff window, second 502 failure exhausts maxRetries -> DEAD_LETTER
        testClock.advance(Duration.ofSeconds(5));
        when(mockTransport.deliver(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(TransportResult.httpFailure(502, "HTTP 502: Bad Gateway", true));

        mockMvc.perform(post("/api/v1/deliveries/dispatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(1));

        mockMvc.perform(get("/api/v1/deliveries/{id}", deliveryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEAD_LETTER"))
                .andExpect(jsonPath("$.attemptCount").value(2));

        // 6. Replay DEAD_LETTER once downstream recovers -> DELIVERED
        when(mockTransport.deliver(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(TransportResult.delivered(200));

        mockMvc.perform(post("/api/v1/deliveries/{id}/replay", deliveryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.lastHttpStatus").value(200))
                .andExpect(jsonPath("$.attemptCount").value(1));

        // Replaying an already DELIVERED item returns 409 Conflict
        mockMvc.perform(post("/api/v1/deliveries/{id}/replay", deliveryId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid delivery state transition"));

        // 7. Capture outbound headers and verify HMAC-SHA256 signatures against both rotated and previous secrets
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sigCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockTransport, times(3)).deliver(
                anyString(),
                idCaptor.capture(),
                tsCaptor.capture(),
                sigCaptor.capture(),
                payloadCaptor.capture()
        );

        String sentWebhookId = idCaptor.getValue();
        String sentTimestamp = tsCaptor.getValue();
        String sentSignature = sigCaptor.getValue();
        String sentPayload = payloadCaptor.getValue();

        assertThat(sentSignature.split("\\s+")).hasSize(2);

        String verifyPayloadNewSecret = objectMapper.writeValueAsString(new VerifySignatureRequest(
                rotatedSecret, sentWebhookId, sentTimestamp, sentSignature, sentPayload));
        String verifyPayloadOldSecret = objectMapper.writeValueAsString(new VerifySignatureRequest(
                originalSecret, sentWebhookId, sentTimestamp, sentSignature, sentPayload));

        mockMvc.perform(post("/api/v1/signatures/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyPayloadNewSecret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        mockMvc.perform(post("/api/v1/signatures/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyPayloadOldSecret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        mockMvc.perform(get("/api/v1/deliveries/{id}/attempts", deliveryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
