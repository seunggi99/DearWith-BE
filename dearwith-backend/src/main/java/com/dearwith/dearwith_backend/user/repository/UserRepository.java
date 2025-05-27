package com.dearwith.dearwith_backend.user.repository;
import java.util.Optional;

import com.dearwith.dearwith_backend.user.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}
