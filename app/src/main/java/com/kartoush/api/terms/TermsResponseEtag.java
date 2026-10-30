package com.kartoush.api.terms;

import com.kartoush.customer.facade.model.TermsOfServiceMetadataView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class TermsResponseEtag {
    private TermsResponseEtag() { }

    static String from(final TermsOfServiceMetadataView terms) {
        // Version identifies immutable published content. Include every mutable public field.
        final String state = String.join("\n", terms.version(), terms.contentType().name(), terms.status().name(),
            String.valueOf(terms.effectiveAt()), String.valueOf(terms.supersededAt()));
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(state.getBytes(StandardCharsets.UTF_8));
            // Weak: semantic state, not serialized JSON bytes. Bump the prefix if representation semantics change.
            return "W/\"terms-json-v1-" + HexFormat.of().formatHex(digest) + "\"";
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
