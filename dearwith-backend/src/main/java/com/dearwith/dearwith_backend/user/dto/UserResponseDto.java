package com.dearwith.dearwith_backend.user.dto;

import com.dearwith.dearwith_backend.user.domain.User;
import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserResponseDto {
    private final UUID id;
    private final String email;
    private final String nickname;

    public UserResponseDto(User u) {
        this.id = u.getId();
        this.email = u.getEmail();
        this.nickname = u.getNickname();
    }

}
