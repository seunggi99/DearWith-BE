package com.dearwith.dearwith_backend.user.service;

import com.dearwith.dearwith_backend.user.domain.User;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;


    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다: " + email));
    }
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findOne(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for id: " + userId));
    }

    public void updateNickname(String userId, String newNickname) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다: " + userId));
        u.setNickname(newNickname);
        userRepository.save(u);
    }

    public void deleteById(String id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("유저가 없습니다: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
    }
}
