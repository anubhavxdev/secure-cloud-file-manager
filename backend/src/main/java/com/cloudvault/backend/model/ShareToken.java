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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "share_tokens")
public class ShareToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @Column(name = "token", nullable = false, unique = true, updatable = false)
    private UUID token;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ShareToken() {
    }

    public ShareToken(FileEntity file, Instant expiresAt) {
        this.file = file;
        this.token = UUID.randomUUID();
        this.expiresAt = expiresAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.token == null) {
            this.token = UUID.randomUUID();
        }
    }

    /**
     * Returns true if this share token has expired.
     * A token with null expiresAt never expires.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public FileEntity getFile() {
        return file;
    }

    public UUID getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // --- Setters ---

    public void setId(UUID id) {
        this.id = id;
    }

    public void setFile(FileEntity file) {
        this.file = file;
    }

    public void setToken(UUID token) {
        this.token = token;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // --- equals / hashCode / toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShareToken that = (ShareToken) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ShareToken{" +
                "id=" + id +
                ", token=" + token +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
