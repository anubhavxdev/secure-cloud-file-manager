package com.cloudvault.backend.service;

import com.cloudvault.backend.dto.CreateFolderRequest;
import com.cloudvault.backend.dto.FileMetadataResponse;
import com.cloudvault.backend.dto.FolderContentsResponse;
import com.cloudvault.backend.dto.FolderResponse;
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

/**
 * Service handling folder CRUD operations and content listing.
 */
@Service
public class FolderService {

    private static final Logger log = LoggerFactory.getLogger(FolderService.class);

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final AuthService authService;

    public FolderService(FolderRepository folderRepository,
                         FileRepository fileRepository,
                         AuthService authService) {
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.authService = authService;
    }

    /**
     * Create a new folder, optionally nested under a parent folder.
     */
    @Transactional
    public FolderResponse createFolder(CreateFolderRequest request) {
        User currentUser = authService.resolveCurrentUser();

        Folder parent = null;
        if (request.parentId() != null) {
            parent = folderRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", request.parentId()));
            verifyOwnership(parent.getOwner(), currentUser);
        }

        Folder folder = new Folder(request.name(), currentUser, parent);
        folder = folderRepository.save(folder);

        log.info("User {} created folder '{}' (id: {}, parent: {})",
                currentUser.getEmail(), folder.getName(), folder.getId(),
                parent != null ? parent.getId() : "root");

        return toResponse(folder);
    }

    /**
     * Get the contents of a folder (subfolders + files).
     */
    public FolderContentsResponse getFolderContents(UUID folderId) {
        User currentUser = authService.resolveCurrentUser();

        Folder folder = findFolderOrThrow(folderId);
        verifyAccess(folder, currentUser);

        // Subfolders within this folder
        List<Folder> subfolders = folderRepository.findByOwnerAndParent(folder.getOwner(), folder);
        List<FolderResponse> subfolderResponses = subfolders.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // Files within this folder
        List<FileEntity> files = fileRepository.findByOwnerAndFolder(folder.getOwner(), folder);
        List<FileMetadataResponse> fileResponses = files.stream()
                .map(this::toFileResponse)
                .collect(Collectors.toList());

        return new FolderContentsResponse(toResponse(folder), subfolderResponses, fileResponses);
    }

    /**
     * List root-level folders for the current user.
     */
    public List<FolderResponse> listRootFolders() {
        User currentUser = authService.resolveCurrentUser();

        List<Folder> folders;
        if (currentUser.getRole() == Role.ADMIN) {
            folders = folderRepository.findAll();
        } else {
            folders = folderRepository.findByOwnerAndParentIsNull(currentUser);
        }

        return folders.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Rename a folder.
     */
    @Transactional
    public FolderResponse renameFolder(UUID folderId, RenameRequest request) {
        User currentUser = authService.resolveCurrentUser();
        Folder folder = findFolderOrThrow(folderId);
        verifyOwnerOrAdmin(folder.getOwner(), currentUser);

        folder.setName(request.newName());
        folder = folderRepository.save(folder);

        log.info("User {} renamed folder {} to '{}'",
                currentUser.getEmail(), folderId, request.newName());

        return toResponse(folder);
    }

    /**
     * Delete a folder.
     * Only allowed if the folder is empty (no subfolders and no files).
     */
    @Transactional
    public void deleteFolder(UUID folderId) {
        User currentUser = authService.resolveCurrentUser();
        Folder folder = findFolderOrThrow(folderId);
        verifyOwnerOrAdmin(folder.getOwner(), currentUser);

        // Check for child subfolders
        List<Folder> children = folderRepository.findByOwnerAndParent(folder.getOwner(), folder);
        if (!children.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete folder '" + folder.getName() + "': it contains subfolders. "
                            + "Delete or move them first.");
        }

        // Check for files in this folder
        List<FileEntity> files = fileRepository.findByOwnerAndFolder(folder.getOwner(), folder);
        if (!files.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete folder '" + folder.getName() + "': it contains files. "
                            + "Delete or move them first.");
        }

        folderRepository.delete(folder);

        log.info("User {} deleted folder '{}' (id: {})",
                currentUser.getEmail(), folder.getName(), folderId);
    }

    // --- Internal helpers ---

    private Folder findFolderOrThrow(UUID folderId) {
        return folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", folderId));
    }

    private void verifyOwnership(User resourceOwner, User currentUser) {
        if (!resourceOwner.getId().equals(currentUser.getId())
                && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("You do not have access to this folder");
        }
    }

    private void verifyAccess(Folder folder, User currentUser) {
        verifyOwnership(folder.getOwner(), currentUser);
    }

    private void verifyOwnerOrAdmin(User resourceOwner, User currentUser) {
        verifyOwnership(resourceOwner, currentUser);
    }

    private FolderResponse toResponse(Folder folder) {
        UUID parentId = folder.getParent() != null ? folder.getParent().getId() : null;
        String parentName = folder.getParent() != null ? folder.getParent().getName() : null;

        return new FolderResponse(
                folder.getId(),
                folder.getName(),
                parentId,
                parentName,
                folder.getCreatedAt().toString()
        );
    }

    private FileMetadataResponse toFileResponse(FileEntity file) {
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
}
