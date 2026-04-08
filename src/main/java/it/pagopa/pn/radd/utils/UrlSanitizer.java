package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.exception.UrlSanitizeException;
import lombok.CustomLog;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

@CustomLog
public class UrlSanitizer {

    public static final String HTTPS = "https://";

    private UrlSanitizer() {
        throw new IllegalStateException("UrlSanitizer is a utility class");
    }

    private static final Pattern SAFE_CHARS = Pattern.compile("^[a-zA-Z0-9:/?#\\[\\]@!$&'()*+,;=_\\-.~%]*$");
    private static final Pattern SCHEME_REGEX = Pattern.compile("^[a-z][a-z0-9+.-]*://.*");
    private static final Pattern VALID_TLD_PATTERN = Pattern.compile(".*\\.[a-zA-Z]{2,}$");

    public static void validateUrl(String inputUrl) {
        if (inputUrl == null || inputUrl.trim().isEmpty()) {
            throw new UrlSanitizeException("L'URL non può essere vuoto o nullo.");
        }

        if (!SAFE_CHARS.matcher(inputUrl).matches()) {
            throw new UrlSanitizeException("URL contiene caratteri non validi: " + inputUrl);
        }

        String urlLower = inputUrl.trim().toLowerCase();
        if (SCHEME_REGEX.matcher(urlLower).matches() && !urlLower.startsWith(HTTPS)) {
            throw new UrlSanitizeException("Protocollo non supportato per l'URL: " + inputUrl);
        }
    }

    public static String sanitizeUrl(String inputUrl) {
        if (inputUrl == null) return null;

        log.debug("Sanitizing URL: {}", inputUrl);
        String url = inputUrl.trim().toLowerCase();

        if (!url.startsWith(HTTPS)) {
            url = HTTPS + url;
        }

        try {
            URI uri = new URI(url).normalize();
            String sanitizedUrl = uri.toString();

            String domainName = sanitizedUrl.replaceAll("http(s)?://|www\\.|/.*", "");

            if (!VALID_TLD_PATTERN.matcher(domainName).matches() && domainName.lastIndexOf(":")<0) {
                throw new UrlSanitizeException("URL contiene un TLD non valido: " + url);
            }

            log.debug("URL sanitizzato: {}", sanitizedUrl);
            return sanitizedUrl;
        } catch (URISyntaxException e) {
            throw new UrlSanitizeException("Errore nella sanitizzazione dell'URL: " + e.getMessage());
        }
    }
}