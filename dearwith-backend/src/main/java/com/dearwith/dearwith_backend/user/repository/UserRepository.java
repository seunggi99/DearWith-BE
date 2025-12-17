package com.dearwith.dearwith_backend.user.repository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    boolean existsByEmailAndDeletedAtIsNull(String email);
    Optional<User> findByNicknameAndDeletedAtIsNull(String nickname);
    boolean existsByNicknameAndDeletedAtIsNull(String nickname);
    @Query("""
    SELECT u
    FROM User u
    WHERE (u.userStatus = 'ACTIVE' OR u.userStatus = 'WRITE_RESTRICTED')
      AND u.deletedAt IS NULL
    """)
    List<User> findAllByUserStatusLoginAllowed();

    List<User> findByUserStatusInAndSuspendedUntilBefore(
            Collection<UserStatus> statuses,
            LocalDate until
    );
}
