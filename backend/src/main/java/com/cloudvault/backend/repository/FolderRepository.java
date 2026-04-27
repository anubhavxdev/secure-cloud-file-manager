package com.cloudvault.backend.repository;

import com.cloudvault.backend.model.Folder;
import com.cloudvault.backend.model.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FolderRepository extends JpaRepository<Folder, UUID> {

    List<Folder> findByOwnerAndParent(User owner, Folder parent);

    List<Folder> findByOwnerAndParentIsNull(User owner);

    List<Folder> findByOwner(User owner);
}
