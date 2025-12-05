package com.dearwith.dearwith_backend.auth.entity;

import com.dearwith.dearwith_backend.user.entity.User;
import com.dearwith.dearwith_backend.user.enums.Role;
import com.dearwith.dearwith_backend.user.enums.UserStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {


    private final UUID id;
    private final String email;
    private final String password;
    private final Role role;
    private final UserStatus userStatus;
    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.userStatus = user.getUserStatus();
        this.user = user;
    }
}
