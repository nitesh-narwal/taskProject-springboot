package com.example.shopapi.repository.jpa;

import com.example.shopapi.entity.EmailVerificationToken;
import com.example.shopapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByUser(User user);

    Optional<EmailVerificationToken> findByUserAndUsedFalse(User user);

    @Modifying
    @Query("UPDATE EmailVerificationToken evt SET evt.used = true WHERE evt.token = :token")
    void markAsUsed(@Param("token") String token);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken evt WHERE evt.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken evt WHERE evt.user = :user")
    void deleteByUser(@Param("user") User user);

    boolean existsByUserAndUsedFalseAndExpiryDateAfter(User user, LocalDateTime now);
}

