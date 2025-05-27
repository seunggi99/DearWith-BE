package com.dearwith.dearwith_backend.user.domain;

import lombok.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;


@Document(collection = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User implements UserDetails{
    @Id // MongoDB _id 필드에 매핑
    private String id;

    private String email;
    private String password;
    private String provider; // 예: 'email', 'kakao'
    private String kakaoId; // 카카오 로그인 사용 시
    private String nickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Role role;
    private List<Agreement> agreements; // <-- 이 필드를 추가합니다.

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 사용자의 권한(역할) 목록을 반환합니다.
        // 예시: 모든 사용자에게 "ROLE_USER" 권한 부여
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        // TODO: 실제 애플리케이션 로직에 따라 사용자의 실제 역할을 로드하여 반환하도록 구현해야 합니다.
        // 예: if (this.isAdmin) return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Override
    public String getPassword() {
        return password; // 암호화된 비밀번호 반환
    }

    @Override
    public String getUsername() {
        // 사용자 식별자로 사용할 값을 반환합니다. 일반적으로 이메일 또는 사용자 이름을 사용합니다.
        return email; // 여기서는 이메일을 사용자 이름으로 사용합니다.
    }

    @Override
    public boolean isAccountNonExpired() {
        // 계정 만료 여부 (true: 만료되지 않음)
        return true; // 필요에 따라 만료 로직 구현
    }

    @Override
    public boolean isAccountNonLocked() {
        // 계정 잠금 여부 (true: 잠겨있지 않음)
        return true; // 필요에 따라 잠금 로직 구현
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 자격 증명(비밀번호) 만료 여부 (true: 만료되지 않음)
        return true; // 필요에 따라 비밀번호 만료 로직 구현
    }

    @Override
    public boolean isEnabled() {
        // 계정 활성화 여부 (true: 활성화됨)
        return true; // 필요에 따라 활성화/비활성화 로직 구현
    }
}
