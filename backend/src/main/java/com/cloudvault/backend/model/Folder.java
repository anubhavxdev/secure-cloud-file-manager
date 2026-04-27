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
@Table(name = "folders")
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Folder() {
    }

    public Folder(String name, User owner, Folder parent) {
        this.name = name;
        this.owner = owner;
        this.parent = parent;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getOwner() {
        return owner;
    }

    public Folder getParent() {
        return parent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // --- Setters ---

    public void setId(UUID id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public void setParent(Folder parent) {
        this.parent = parent;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // --- equals / hashCode / toString ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Folder folder = (Folder) o;
        return id != null && Objects.equals(id, folder.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Folder{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
