package com.cloudvault.backend.controller;

import com.cloudvault.backend.dto.CreateFolderRequest;
import com.cloudvault.backend.dto.FolderContentsResponse;
import com.cloudvault.backend.dto.FolderResponse;
import com.cloudvault.backend.dto.RenameRequest;
import com.cloudvault.backend.service.FolderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for folder operations.
 * <ul>
 *   <li>POST   /api/folders            — create a folder</li>
 *   <li>GET    /api/folders             — list root-level folders</li>
 *   <li>GET    /api/folders/{id}/contents — get folder contents (subfolders + files)</li>
 *   <li>DELETE /api/folders/{id}        — delete an empty folder</li>
 *   <li>PATCH  /api/folders/{id}/rename — rename a folder</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @Valid @RequestBody CreateFolderRequest request) {

        FolderResponse response = folderService.createFolder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> listRootFolders() {
        List<FolderResponse> folders = folderService.listRootFolders();
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/{id}/contents")
    public ResponseEntity<FolderContentsResponse> getFolderContents(@PathVariable UUID id) {
        FolderContentsResponse response = folderService.getFolderContents(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable UUID id) {
        folderService.deleteFolder(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/rename")
    public ResponseEntity<FolderResponse> renameFolder(
            @PathVariable UUID id,
            @Valid @RequestBody RenameRequest request) {

        FolderResponse response = folderService.renameFolder(id, request);
        return ResponseEntity.ok(response);
    }
}
