package com.kartoush.api.support;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class UrlQueryParams {

    private UrlQueryParams() {
    }

    public static String queryParam(final String url, final String name) {
        return queryParams(url).get(name);
    }

    public static Map<String, String> queryParams(final String url) {
        final String query = URI.create(url).getQuery();
        if (query == null || query.isBlank()) {
            return Map.of();
        }

        return List.of(query.split("&")).stream()
            .map(part -> part.split("=", 2))
            .collect(Collectors.toMap(
                pair -> decode(pair[0]),
                pair -> pair.length > 1 ? decode(pair[1]) : ""
            ));
    }

    private static String decode(final String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
