package com.mickaelgeronimo.webhookrelay.api;

import com.mickaelgeronimo.webhookrelay.domain.WebhookEndpointEntity;
import com.mickaelgeronimo.webhookrelay.service.WebhookIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for managing webhook subscribers and rotating signing secrets.
 */
@RestController
@RequestMapping("/api/v1/endpoints")
public class EndpointController {

    private final WebhookIngestionService ingestionService;

    public EndpointController(WebhookIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<EndpointResponse> register(@Valid @RequestBody RegisterEndpointRequest request) {
        WebhookEndpointEntity created = ingestionService.registerEndpoint(
                request.tenantId(),
                request.targetUrl(),
                request.eventTypes(),
                request.maxRetriesOrDefault()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(EndpointResponse.from(created, true));
    }

    @GetMapping
    public List<EndpointResponse> list(@RequestParam(required = false) String tenantId) {
        return ingestionService.listEndpoints(tenantId).stream()
                .map(ep -> EndpointResponse.from(ep, false))
                .toList();
    }

    @GetMapping("/{id}")
    public EndpointResponse getById(@PathVariable String id) {
        return EndpointResponse.from(ingestionService.getEndpoint(id), false);
    }

    @PatchMapping("/{id}/status")
    public EndpointResponse updateStatus(
            @PathVariable String id,
            @RequestBody UpdateEndpointStatusRequest request
    ) {
        return EndpointResponse.from(ingestionService.updateEndpointStatus(id, request.active()), false);
    }

    @PostMapping("/{id}/rotate-secret")
    public EndpointResponse rotateSecret(@PathVariable String id) {
        return EndpointResponse.from(ingestionService.rotateEndpointSecret(id), true);
    }
}
