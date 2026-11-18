package com.mickaelgeronimo.webhookrelay.service;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Spring RestClient-based outbound transport emitting Standard Webhooks headers.
 */
@Component
public class HttpWebhookTransport implements WebhookTransport {

    private final RestClient restClient;

    public HttpWebhookTransport(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public TransportResult deliver(
            String targetUrl,
            String webhookId,
            String timestampHeader,
            String signatureHeader,
            String payload
    ) {
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("webhook-id", webhookId)
                    .header("webhook-timestamp", timestampHeader)
                    .header("webhook-signature", signatureHeader)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            int status = response.getStatusCode().value();
            return TransportResult.delivered(status);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            boolean retryable = status >= 500 || status == 429 || status == 408;
            return TransportResult.httpFailure(status, "HTTP " + status + ": " + ex.getStatusText(), retryable);
        } catch (RestClientException ex) {
            return TransportResult.networkFailure("Network error: " + ex.getMessage());
        }
    }
}
