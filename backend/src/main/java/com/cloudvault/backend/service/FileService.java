package com.cloudvault.backend.service;

import com.cloudvault.backend.config.SupabaseStorageClient;
import com.cloudvault.backend.dto.FileMetadataResponse;
import com.cloudvault.backend.dto.RenameRequest;
import com.cloudvault.backend.exception.ResourceNotFoundException;
import com.cloudvault.backend.exception.UnauthorizedException;
import com.cloudvault.backend.model.FileEntity;
import com.cloudvault.backend.model.Folder;
import com.cloudvault.backend.model.Role;
import com.cloudvault.backend.model.User;
import com.cloudvault.backend.repository.FileRepository;
import com.cloudvault.backend.repository.FolderRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service handling file CRUD operations.
 * Coordinates between the database (metadata) and Supabase Storage (binary data).
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final SupabaseStorageClient storageClient;
    private final AuthService authService;

    public FileService(FileRepository fileRepository,
                       FolderRepository folderRepository,
                       SupabaseStorageClient storageClient,
                       AuthService authService) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.storageClient = storageClient;
        this.authService = authService;
    }

    /**
     * Upload a file to Supabase Storage and persist metadata.
     *
     * @param multipartFile the uploaded file
     * @param folderId      optional folder ID (null for root)
     * @return file metadata response
     */
    @Transactional
    public FileMetadataResponse uploadFile(MultipartFile multipartFile, UUID folderId) {
        User currentUser = authService.resolveCurrentUser();

        // Resolve optional folder
        Folder folder = null;
        if (folderId != null) {
            folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", folderId));
            verifyOwnership(folder.getOwner(), currentUser);
        }

        // Build a unique storage path: {userId}/{uuid}_{originalFilename}
        String originalName = multipartFile.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "unnamed";
        }
        String storagePath = buildStoragePath(currentUser.getId(), originalName);

        try {
            // Upload binary data to Supabase Storage
            byte[] fileBytes = multipartFile.getBytes();
            String contentType = multipartFile.getContentType() != null
                    ? multipartFile.getContentType()
                    : "application/octet-stream";

            storageClient.upload(storagePath, fileBytes, contentType);

            // Persist metadata in PostgreSQL
            FileEntity fileEntity = new FileEntity(
                    originalName,
                    storagePath,
                    contentType,
                    multipartFile.getSize(),
                    currentUser,
                    folder
            );
            fileEntity = fileRepository.save(fileEntity);

            log.info("User {} uploaded file '{}' (id: {})", currentUser.getEmail(),
                    originalName, fileEntity.getId());

            return toResponse(fileEntity);

        } catch (java.io.IOException e) {
            throw new com.cloudvault.backend.exception.StorageException(
                    "Failed to read uploaded file: " + originalName, e);
        }
    }

    /**
     * Get file metadata by ID.
     * ADMINs can see any file; regular users can only see their own.
     */
    public FileMetadataResponse getFileMetadata(UUID fileId) {
        FileEntity file = findFileOrThrow(fileId);
        User currentUser = authService.resolveCurrentUser();
        verifyAccessPermission(file, currentUser);
        return toResponse(file);
    }

    /**
     * List files for the current user, optionally filtered by folder.
     * ADMINs see all files; regular users see only their own.
     *
     * @param folderId optional folder filter (null for root-level files)
     */
    public List<FileMetadataResponse> listFiles(UUID folderId) {
        User currentUser = authService.resolveCurrentUser();
        List<FileEntity> files;

        if (currentUser.getRole() == Role.ADMIN && folderId == null) {
            // ADMIN with no folder filter: see all files
            files = fileRepository.findAll();
        } else if (folderId != null) {
            Folder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", folderId));
            verifyAccessPermission(folder, currentUser);
            files = fileRepository.findByOwnerAndFolder(
                    currentUser.getRole() == Role.ADMIN ? folder.getOwner() : currentUser, folder);
        } else {
            // Regular user, root level
            files = fileRepository.findByOwnerAndFolderIsNull(currentUser);
        }

        return files.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Download file bytes from Supabase Storage.
     * Returns the raw byte array; the controller handles streaming to the response.
     */
    public FileDownloadResult downloadFile(UUID fileId) {
        FileEntity file = findFileOrThrow(fileId);
        User currentUser = authService.resolveCurrentUser();
        verifyAccessPermission(file, currentUser);

        byte[] data = storageClient.download(file.getStoragePath());
        return new FileDownloadResult(data, file.getContentType(), file.getOriginalName());
    }

    /**
     * Delete a file from both Supabase Storage and the database.
     */
    @Transactional
    public void deleteFile(UUID fileId) {
        FileEntity file = findFileOrThrow(fileId);
        User currentUser = authService.resolveCurrentUser();
        verifyOwnerOrAdmin(file.getOwner(), currentUser);

        storageClient.delete(file.getStoragePath());
        fileRepository.delete(file);

        log.info("User {} deleted file '{}' (id: {})", currentUser.getEmail(),
                file.getOriginalName(), file.getId());
    }

    /**
     * Rename a file (metadata only — storage path stays the same).
     */
    @Transactional
    public FileMetadataResponse renameFile(UUID fileId, RenameRequest request) {
        FileEntity file = findFileOrThrow(fileId);
        User currentUser = authService.resolveCurrentUser();
        verifyOwnerOrAdmin(file.getOwner(), currentUser);

        file.setOriginalName(request.newName());
        file = fileRepository.save(file);

        log.info("User {} renamed file {} to '{}'", currentUser.getEmail(),
                fileId, request.newName());

        return toResponse(file);
    }

    // --- Internal helpers ---

    private FileEntity findFileOrThrow(UUID fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File", fileId));
    }

    private void verifyOwnership(User resourceOwner, User currentUser) {
        if (!resourceOwner.getId().equals(currentUser.getId())
                && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("You do not have access to this resource");
        }
    }

    private void verifyAccessPermission(FileEntity file, User currentUser) {
        verifyOwnership(file.getOwner(), currentUser);
    }

    private void verifyAccessPermission(Folder folder, User currentUser) {
        verifyOwnership(folder.getOwner(), currentUser);
    }

    private void verifyOwnerOrAdmin(User resourceOwner, User currentUser) {
        verifyOwnership(resourceOwner, currentUser);
    }

    private String buildStoragePath(UUID userId, String originalName) {
        // Sanitize the filename: keep alphanumeric, dots, hyphens, underscores
        String sanitized = originalName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        return userId.toString() + "/" + UUID.randomUUID() + "_" + sanitized;
    }

    private FileMetadataResponse toResponse(FileEntity file) {
        UUID folderId = file.getFolder() != null ? file.getFolder().getId() : null;
        String folderName = file.getFolder() != null ? file.getFolder().getName() : null;

        return new FileMetadataResponse(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSize(),
                folderId,
                folderName,
                file.getCreatedAt().toString(),
                file.getUpdatedAt().toString()
        );
    }

    /**
     * Carrier record for file download results.
     */
    public record FileDownloadResult(byte[] data, String contentType, String originalName) {
    }
}
