package com.mickaelgeronimo.webhookrelay.api;

import com.mickaelgeronimo.webhookrelay.domain.DeliveryStatus;
import com.mickaelgeronimo.webhookrelay.service.WebhookDispatcherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for inspecting deliveries, attempt history, triggering dispatch and replaying DLQ items.
 */
@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final WebhookDispatcherService dispatcherService;

    public DeliveryController(WebhookDispatcherService dispatcherService) {
        this.dispatcherService = dispatcherService;
    }

    @GetMapping
    public List<DeliveryResponse> list(@RequestParam(required = false) DeliveryStatus status) {
        return dispatcherService.listDeliveries(status).stream()
                .map(DeliveryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public DeliveryResponse getById(@PathVariable String id) {
        return DeliveryResponse.from(dispatcherService.getDelivery(id));
    }

    @GetMapping("/{id}/attempts")
    public List<DeliveryAttemptResponse> getAttempts(@PathVariable String id) {
        return dispatcherService.getAttempts(id).stream()
                .map(DeliveryAttemptResponse::from)
                .toList();
    }

    @PostMapping("/dispatch")
    public DispatchSummaryResponse dispatchNow() {
        return new DispatchSummaryResponse(dispatcherService.dispatchDueDeliveries());
    }

    @PostMapping("/{id}/replay")
    public DeliveryResponse replayDeadLetter(@PathVariable String id) {
        return DeliveryResponse.from(dispatcherService.replayDeadLetter(id));
    }
}
