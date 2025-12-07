package com.dearwith.dearwith_backend.user.dto;

import com.dearwith.dearwith_backend.user.entity.User;
import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserResponseDto {
    private final UUID id;
    private final String email;
    private final String nickname;
    private final Boolean isAdmin;

    public UserResponseDto(User u) {
        this.id = u.getId();
        this.email = u.getEmail();
        this.nickname = u.getNickname();
        this.isAdmin = u.isAdmin();
    }
}
