package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.auth.service.AuthService;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.user.domain.User;
import com.dearwith.dearwith_backend.user.domain.enums.UserStatus;
import com.dearwith.dearwith_backend.user.dto.UserResponseDto;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthService authService;
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
    public List<User> findAll() {
        return userRepository.findAll();
    }
    public User findOne(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public void updateNickname(UUID userId, String newNickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        authService.validateDuplicateUserByNickname(newNickname);
        user.updateNickname(newNickname);
        userRepository.save(user);
    }

    @Transactional
    public void deleteById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        user.markDeleted();
        userRepository.save(user);
    }

    // 회원 본인 정보 조회
    public UserResponseDto getCurrentUser(UUID userId) {
        User user = findOne(userId);
        return new UserResponseDto(user);
    }

    // 모든 회원 조회
    public List<UserResponseDto> getAllUsers() {
        return findAll().stream()
                .map(UserResponseDto::new)
                .collect(Collectors.toList());
    }

}
