package com.mickaelgeronimo.webhookrelay.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Validates outbound webhook URLs and defends against Server-Side Request Forgery (SSRF)
 * by rejecting loopback, link-local (cloud metadata), site-local and multicast IP addresses.
 */
public class WebhookUrlValidator {

    @FunctionalInterface
    public interface HostResolver {
        List<InetAddress> resolveAll(String host) throws UnknownHostException;
    }

    private final boolean allowPrivateTargets;
    private final HostResolver hostResolver;

    public WebhookUrlValidator(boolean allowPrivateTargets) {
        this(allowPrivateTargets, host -> Arrays.asList(InetAddress.getAllByName(host)));
    }

    public WebhookUrlValidator(boolean allowPrivateTargets, HostResolver hostResolver) {
        this.allowPrivateTargets = allowPrivateTargets;
        this.hostResolver = Objects.requireNonNull(hostResolver, "hostResolver must not be null");
    }

    public URI validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new UnsafeWebhookTargetException("Webhook target URL must not be blank");
        }

        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException ex) {
            throw new UnsafeWebhookTargetException("Webhook target URL is malformed: " + ex.getMessage());
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme) && !"http".equals(scheme)) {
            throw new UnsafeWebhookTargetException("Only http and https webhook schemes are allowed");
        }

        if (uri.getRawUserInfo() != null) {
            throw new UnsafeWebhookTargetException("Embedded URL credentials are not permitted");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UnsafeWebhookTargetException("Webhook target URL must contain a valid host");
        }

        if (!allowPrivateTargets) {
            checkPublicHost(host);
        }

        return uri;
    }

    private void checkPublicHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalized) || normalized.endsWith(".localhost") || normalized.endsWith(".local")) {
            throw new UnsafeWebhookTargetException("Private or loopback hostname is blocked by SSRF policy: " + host);
        }

        List<InetAddress> addresses;
        try {
            addresses = hostResolver.resolveAll(host);
        } catch (UnknownHostException ex) {
            throw new UnsafeWebhookTargetException("Unable to resolve webhook target host: " + host);
        }

        if (addresses.isEmpty()) {
            throw new UnsafeWebhookTargetException("Webhook target host resolved to no IP addresses: " + host);
        }

        for (InetAddress address : addresses) {
            if (isInternalAddress(address)) {
                throw new UnsafeWebhookTargetException(
                        "Webhook target resolves to non-public IP address (" + address.getHostAddress() + ")");
            }
        }
    }

    static boolean isInternalAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] raw = address.getAddress();
        // Block IPv6 Unique Local Addresses (fc00::/7) and Carrier-Grade NAT IPv4 (100.64.0.0/10).
        if (raw.length == 16 && (raw[0] & 0xfe) == 0xfc) {
            return true;
        }
        return raw.length == 4 && (raw[0] & 0xff) == 100 && (raw[1] & 0xc0) == 64;
    }
}
