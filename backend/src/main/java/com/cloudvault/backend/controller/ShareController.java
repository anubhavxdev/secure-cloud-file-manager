package com.cloudvault.backend.controller;

import com.cloudvault.backend.dto.CreateShareRequest;
import com.cloudvault.backend.dto.ShareResponse;
import com.cloudvault.backend.service.ShareService;
import com.cloudvault.backend.service.ShareService.ShareDownloadResult;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for file sharing operations.
 * <ul>
 *   <li>POST   /api/files/{id}/share   — create a share link (auth required)</li>
 *   <li>DELETE  /api/files/{id}/share   — revoke all share links (auth required)</li>
 *   <li>GET    /api/share/{token}       — public file download (no auth)</li>
 * </ul>
 */
@RestController
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    /**
     * Create a share link for a file.
     * Requires authentication. The owner (or ADMIN) can create share tokens.
     */
    @PostMapping("/api/files/{fileId}/share")
    public ResponseEntity<ShareResponse> createShareLink(
            @PathVariable UUID fileId,
            @RequestBody(required = false) CreateShareRequest request) {

        ShareResponse response = shareService.createShareToken(fileId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Revoke all share links for a file.
     * Requires authentication.
     */
    @DeleteMapping("/api/files/{fileId}/share")
    public ResponseEntity<Void> revokeShareLinks(@PathVariable UUID fileId) {
        shareService.revokeShareTokens(fileId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Public download via share token.
     * No authentication required — the token itself is the authorization.
     */
    @GetMapping("/api/share/{token}")
    public ResponseEntity<byte[]> downloadSharedFile(@PathVariable UUID token) {
        ShareDownloadResult result = shareService.downloadSharedFile(token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.contentType()));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(result.originalName())
                        .build()
        );
        headers.setContentLength(result.data().length);

        return new ResponseEntity<>(result.data(), headers, HttpStatus.OK);
    }
}
