package com.cloudvault.backend.config;

import com.cloudvault.backend.exception.StorageException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for Supabase Storage REST API.
 * <p>
 * All operations use the service role key for server-side access,
 * bypassing Supabase RLS policies. Access control is enforced at
 * our application layer instead.
 * </p>
 *
 * <h3>API Reference:</h3>
 * <ul>
 *   <li>Upload:   POST   /storage/v1/object/{bucket}/{path}</li>
 *   <li>Download: GET    /storage/v1/object/{bucket}/{path}</li>
 *   <li>Delete:   DELETE /storage/v1/object/{bucket}  (body: {"prefixes": [...]})</li>
 * </ul>
 */
@Component
public class SupabaseStorageClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageClient.class);

    private final RestClient restClient;
    private final String bucket;

    public SupabaseStorageClient(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey,
            @Value("${supabase.storage.bucket}") String bucket) {
        this.bucket = bucket;
        this.restClient = RestClient.builder()
                .baseUrl(supabaseUrl + "/storage/v1")
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .defaultHeader("apikey", serviceRoleKey)
                .build();
    }

    /**
     * Upload a file to Supabase Storage.
     *
     * @param storagePath path within the bucket (e.g. "userId/uuid_filename.pdf")
     * @param data        raw file bytes
     * @param contentType MIME type of the file
     * @throws StorageException if the upload fails
     */
    public void upload(String storagePath, byte[] data, String contentType) {
        try {
            restClient.post()
                    .uri("/object/{bucket}/{path}", bucket, storagePath)
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("x-upsert", "true")
                    .body(data)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new StorageException(
                                "Supabase upload failed with status " + response.getStatusCode()
                                        + " for path: " + storagePath);
                    })
                    .toBodilessEntity();

            log.info("Uploaded file to Supabase Storage: {}/{}", bucket, storagePath);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Failed to upload file to Supabase Storage: " + storagePath, e);
        }
    }

    /**
     * Download a file from Supabase Storage.
     *
     * @param storagePath path within the bucket
     * @return raw file bytes
     * @throws StorageException if the download fails
     */
    public byte[] download(String storagePath) {
        try {
            byte[] data = restClient.get()
                    .uri("/object/{bucket}/{path}", bucket, storagePath)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new StorageException(
                                "Supabase download failed with status " + response.getStatusCode()
                                        + " for path: " + storagePath);
                    })
                    .body(byte[].class);

            if (data == null) {
                throw new StorageException("Supabase returned empty body for path: " + storagePath);
            }

            log.info("Downloaded file from Supabase Storage: {}/{} ({} bytes)",
                    bucket, storagePath, data.length);
            return data;
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Failed to download file from Supabase Storage: " + storagePath, e);
        }
    }

    /**
     * Delete a file from Supabase Storage.
     *
     * @param storagePath path within the bucket
     * @throws StorageException if the deletion fails
     */
    public void delete(String storagePath) {
        try {
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/object/{bucket}", bucket)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(storagePath)))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new StorageException(
                                "Supabase delete failed with status " + response.getStatusCode()
                                        + " for path: " + storagePath);
                    })
                    .toBodilessEntity();

            log.info("Deleted file from Supabase Storage: {}/{}", bucket, storagePath);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Failed to delete file from Supabase Storage: " + storagePath, e);
        }
    }
}
