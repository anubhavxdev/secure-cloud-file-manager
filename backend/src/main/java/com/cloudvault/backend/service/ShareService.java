package com.cloudvault.backend.service;

import com.cloudvault.backend.config.SupabaseStorageClient;
import com.cloudvault.backend.dto.CreateShareRequest;
import com.cloudvault.backend.dto.ShareResponse;
import com.cloudvault.backend.exception.ResourceNotFoundException;
import com.cloudvault.backend.exception.UnauthorizedException;
import com.cloudvault.backend.model.FileEntity;
import com.cloudvault.backend.model.Role;
import com.cloudvault.backend.model.ShareToken;
import com.cloudvault.backend.model.User;
import com.cloudvault.backend.repository.FileRepository;
import com.cloudvault.backend.repository.ShareTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling file sharing via UUID-based public tokens.
 */
@Service
public class ShareService {

    private static final Logger log = LoggerFactory.getLogger(ShareService.class);

    private final ShareTokenRepository shareTokenRepository;
    private final FileRepository fileRepository;
    private final SupabaseStorageClient storageClient;
    private final AuthService authService;

    @Value("${server.port:8080}")
    private int serverPort;

    public ShareService(ShareTokenRepository shareTokenRepository,
                        FileRepository fileRepository,
                        SupabaseStorageClient storageClient,
                        AuthService authService) {
        this.shareTokenRepository = shareTokenRepository;
        this.fileRepository = fileRepository;
        this.storageClient = storageClient;
        this.authService = authService;
    }

    /**
     * Create a share token for a file.
     *
     * @param fileId  the file to share
     * @param request optional expiry configuration
     * @return share response with the public URL
     */
    @Transactional
    public ShareResponse createShareToken(UUID fileId, CreateShareRequest request) {
        User currentUser = authService.resolveCurrentUser();
        FileEntity file = findFileOrThrow(fileId);
        verifyOwnerOrAdmin(file.getOwner(), currentUser);

        // Calculate expiry
        Instant expiresAt = null;
        if (request != null && request.expiresInHours() != null && request.expiresInHours() > 0) {
            expiresAt = Instant.now().plus(request.expiresInHours(), ChronoUnit.HOURS);
        }

        ShareToken shareToken = new ShareToken(file, expiresAt);
        shareToken = shareTokenRepository.save(shareToken);

        log.info("User {} created share token for file '{}' (token: {}, expires: {})",
                currentUser.getEmail(), file.getOriginalName(), shareToken.getToken(),
                expiresAt != null ? expiresAt : "never");

        return toResponse(shareToken);
    }

    /**
     * Resolve a share token and return the file bytes for public download.
     * This method does NOT require authentication.
     *
     * @param token the share UUID token
     * @return file download result
     * @throws ResourceNotFoundException if the token does not exist
     * @throws IllegalStateException     if the token has expired
     */
    public ShareDownloadResult downloadSharedFile(UUID token) {
        ShareToken shareToken = shareTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Share link not found or is invalid"));

        if (shareToken.isExpired()) {
            throw new IllegalStateException("This share link has expired");
        }

        FileEntity file = shareToken.getFile();
        byte[] data = storageClient.download(file.getStoragePath());

        log.info("Public download via share token {} for file '{}'",
                token, file.getOriginalName());

        return new ShareDownloadResult(data, file.getContentType(), file.getOriginalName());
    }

    /**
     * Revoke all share tokens for a file.
     */
    @Transactional
    public void revokeShareTokens(UUID fileId) {
        User currentUser = authService.resolveCurrentUser();
        FileEntity file = findFileOrThrow(fileId);
        verifyOwnerOrAdmin(file.getOwner(), currentUser);

        shareTokenRepository.deleteByFile(file);

        log.info("User {} revoked all share tokens for file '{}' (id: {})",
                currentUser.getEmail(), file.getOriginalName(), fileId);
    }

    // --- Internal helpers ---

    private FileEntity findFileOrThrow(UUID fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));
    }

    private void verifyOwnerOrAdmin(User resourceOwner, User currentUser) {
        if (!resourceOwner.getId().equals(currentUser.getId())
                && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("You do not have permission to share this file");
        }
    }

    private ShareResponse toResponse(ShareToken shareToken) {
        FileEntity file = shareToken.getFile();
        String shareUrl = "/api/share/" + shareToken.getToken();

        return new ShareResponse(
                shareToken.getId(),
                file.getId(),
                file.getOriginalName(),
                shareToken.getToken(),
                shareUrl,
                shareToken.getExpiresAt() != null ? shareToken.getExpiresAt().toString() : null,
                shareToken.getCreatedAt().toString()
        );
    }

    /**
     * Carrier record for shared file download results.
     */
    public record ShareDownloadResult(byte[] data, String contentType, String originalName) {
    }
}
