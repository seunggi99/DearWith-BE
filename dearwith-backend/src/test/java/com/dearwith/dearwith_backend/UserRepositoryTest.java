package com.dearwith.dearwith_backend;

import com.dearwith.dearwith_backend.user.domain.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void 사용자_저장_및_조회_테스트() {

    }
}
