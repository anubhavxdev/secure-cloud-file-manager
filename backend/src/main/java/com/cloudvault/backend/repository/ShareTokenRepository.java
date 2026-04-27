package com.cloudvault.backend.repository;

import com.cloudvault.backend.model.FileEntity;
import com.cloudvault.backend.model.ShareToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShareTokenRepository extends JpaRepository<ShareToken, UUID> {

    Optional<ShareToken> findByToken(UUID token);

    List<ShareToken> findByFile(FileEntity file);

    void deleteByFile(FileEntity file);
}
