package it.pagopa.pn.radd.utils;

public final class SafeStorageTestUtils {

    private static final String SAFESTORAGE_KEY_PREFIX = "safestorage://";
    private static final String PRESIGNED_URL_BASE = "https://pn-safestorage-eu-south-1-000000000000.s3.eu-south-1.amazonaws.com/";
    private static final String PRESIGNED_URL_QUERY = "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260101T000000Z&X-Amz-Expires=3600&X-Amz-Signature=test";

    private SafeStorageTestUtils() {
    }

    public static String safeStorageKey(String fileKey) {
        return SAFESTORAGE_KEY_PREFIX + fileKey;
    }

    public static String presignedUrl(String fileKey) {
        return PRESIGNED_URL_BASE + fileKey + PRESIGNED_URL_QUERY;
    }
}
