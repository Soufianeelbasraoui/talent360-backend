package com.talent360.auth.repository;

import com.talent360.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.revokedAt = :now WHERE r.familyId = :familyId")
    void revokeFamily(@Param("familyId") String familyId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.revokedAt = :now WHERE r.user.id = :userId")
    void revokeAllForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
