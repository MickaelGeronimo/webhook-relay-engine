package com.mickaelgeronimo.webhookrelay.api;

import com.mickaelgeronimo.webhookrelay.service.WebhookIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for idempotent domain event ingestion into the webhook delivery pipeline.
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final WebhookIngestionService ingestionService;

    public EventController(WebhookIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<IngestEventResponse> ingest(@Valid @RequestBody IngestEventRequest request) {
        WebhookIngestionService.IngestionResult result = ingestionService.ingestEvent(
                request.tenantId(),
                request.eventType(),
                request.idempotencyKey(),
                request.payload()
        );
        HttpStatus status = result.deduplicated() ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(IngestEventResponse.from(result));
    }
}
