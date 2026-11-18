package com.mickaelgeronimo.webhookrelay.api;

import com.mickaelgeronimo.webhookrelay.security.StandardWebhookSigner;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Utility endpoint enabling receivers to validate Standard Webhooks HMAC-SHA256 signatures.
 */
@RestController
@RequestMapping("/api/v1/signatures")
public class SignatureVerificationController {

    private final StandardWebhookSigner signer;

    public SignatureVerificationController(StandardWebhookSigner signer) {
        this.signer = signer;
    }

    @PostMapping("/verify")
    public VerifySignatureResponse verify(@Valid @RequestBody VerifySignatureRequest request) {
        boolean valid = signer.verify(
                request.secret(),
                request.webhookId(),
                request.timestampHeader(),
                request.signatureHeader(),
                request.payload()
        );
        return new VerifySignatureResponse(valid);
    }
}
