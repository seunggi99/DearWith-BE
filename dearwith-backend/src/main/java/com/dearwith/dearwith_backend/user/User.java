package com.dearwith.dearwith_backend.user;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    @Id
    private String id;

    private String email;      // 이메일 회원/카카오 회원 모두 가능
    private String password;   // 이메일 회원만 사용, bcrypt 등으로 암호화
    private String provider;   // "local" 또는 "kakao"
    private String kakaoId;    // 카카오 회원만 사용
    private String nickname;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
