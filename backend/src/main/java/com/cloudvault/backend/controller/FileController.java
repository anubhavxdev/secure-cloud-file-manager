package com.cloudvault.backend.controller;

import com.cloudvault.backend.dto.FileMetadataResponse;
import com.cloudvault.backend.dto.RenameRequest;
import com.cloudvault.backend.service.FileService;
import com.cloudvault.backend.service.FileService.FileDownloadResult;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for file operations.
 * <ul>
 *   <li>POST   /api/files/upload        — upload a file</li>
 *   <li>GET    /api/files               — list files (optional folderId filter)</li>
 *   <li>GET    /api/files/{id}          — get file metadata</li>
 *   <li>GET    /api/files/{id}/download — download file</li>
 *   <li>DELETE /api/files/{id}          — delete file</li>
 *   <li>PATCH  /api/files/{id}/rename   — rename file</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileMetadataResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) UUID folderId) {

        FileMetadataResponse response = fileService.uploadFile(file, folderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FileMetadataResponse>> listFiles(
            @RequestParam(value = "folderId", required = false) UUID folderId) {

        List<FileMetadataResponse> files = fileService.listFiles(folderId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileMetadataResponse> getFileMetadata(@PathVariable UUID id) {
        FileMetadataResponse response = fileService.getFileMetadata(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable UUID id) {
        FileDownloadResult result = fileService.downloadFile(id);

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<FileMetadataResponse> renameFile(
            @PathVariable UUID id,
            @Valid @RequestBody RenameRequest request) {

        FileMetadataResponse response = fileService.renameFile(id, request);
        return ResponseEntity.ok(response);
    }
}
