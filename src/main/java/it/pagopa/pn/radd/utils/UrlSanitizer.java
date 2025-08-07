package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.exception.UrlSanitizeException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class UrlSanitizer {

    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[<>\"'(){};]");
    public static final Pattern SCHEME_SEPARATOR_REGEX = Pattern.compile("^[a-z][a-z0-9+.-]*://.*");

    public static String sanitizeUrl(String inputUrl) {
        validateInput(inputUrl);

        String url = prepareUrl(inputUrl.trim().toLowerCase());

        try {
            URI uri = new URI(url);
            return buildUrl(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getRawQuery());
        } catch (URISyntaxException e) {
            throw new UrlSanitizeException("Errore nella sanitizzazione dell'URL: " + e.getMessage());
        }
    }

    private static void validateInput(String inputUrl) {
        if (inputUrl == null || inputUrl.trim().isEmpty()) {
            throw new UrlSanitizeException("L'URL non può essere vuoto o nullo");
        }
        if (DANGEROUS_CHARS.matcher(inputUrl).find()) {
            throw new UrlSanitizeException("L'URL contiene caratteri pericolosi");
        }
    }

    private static String prepareUrl(String url) {
        if (SCHEME_SEPARATOR_REGEX.matcher(url).find()) {
            if (!url.startsWith("https://")) {
                throw new UrlSanitizeException("Protocollo non supportato");
            }
            return url;
        }
        return "https://" + url;
    }

    private static String buildUrl(String scheme, String host, int port, String path, String query) {
        StringBuilder urlBuilder = new StringBuilder(scheme).append("://").append(host);

        if (port != -1) urlBuilder.append(":").append(port);
        if (path != null && !path.isEmpty()) urlBuilder.append(path);
        if (query != null && !query.isEmpty()) urlBuilder.append("?").append(query);

        return urlBuilder.toString();
    }
}