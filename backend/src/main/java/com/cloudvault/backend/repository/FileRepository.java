package com.cloudvault.backend.repository;

import com.cloudvault.backend.model.FileEntity;
import com.cloudvault.backend.model.Folder;
import com.cloudvault.backend.model.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    List<FileEntity> findByOwner(User owner);

    List<FileEntity> findByOwnerAndFolder(User owner, Folder folder);

    List<FileEntity> findByOwnerAndFolderIsNull(User owner);
}
