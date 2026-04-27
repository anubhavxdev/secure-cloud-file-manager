package com.cloudvault.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "size", nullable = false)
    private long size;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FileEntity() {
    }

    public FileEntity(String originalName, String storagePath, String contentType,
                      long size, User owner, Folder folder) {
        this.originalName = originalName;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.size = size;
        this.owner = owner;
        this.folder = folder;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public User getOwner() {
        return owner;
    }

    public Folder getFolder() {
        return folder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // --- Setters ---

    public void setId(UUID id) {
        this.id = id;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // --- equals / hashCode / toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileEntity that = (FileEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "FileEntity{" +
                "id=" + id +
                ", originalName='" + originalName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
