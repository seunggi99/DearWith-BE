package com.dearwith.dearwith_backend.auth.service;

import com.dearwith.dearwith_backend.auth.JwtTokenProvider;
import com.dearwith.dearwith_backend.auth.dto.JwtRequest;
import com.dearwith.dearwith_backend.auth.dto.JwtResponse;
import com.dearwith.dearwith_backend.auth.dto.SigninRequestDto;
import com.dearwith.dearwith_backend.user.domain.AuthProvider;
import com.dearwith.dearwith_backend.user.domain.Role;
import com.dearwith.dearwith_backend.user.domain.User;
import com.dearwith.dearwith_backend.auth.dto.SignupRequest;
import com.dearwith.dearwith_backend.user.repository.UserRepository;
import com.dearwith.dearwith_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    @Autowired
    private final JwtTokenProvider jwtTokenProvider;
    @Autowired
    private final UserService userService;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public JwtResponse signup(SignupRequest req) {
        JwtResponse resp = new JwtResponse();
        try {
            // 1) 이미 가입된 이메일인지 체크
            if (userRepo.existsByEmail(req.getEmail())) {
                throw new IllegalStateException("이미 가입된 이메일입니다.");
            }

            // 2) 최종 User 엔티티 생성
            User u = User.builder()
                    .email(req.getEmail())
                    .password(passwordEncoder.encode(req.getPassword()))
                    .nickname(req.getNickname())
                    .agreements(req.getAgreements())
                    .provider(AuthProvider.LOCAL.name())
                    .role(Role.ROLE_USER)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            User saved = userRepo.save(u);

            // 3) JwtResponse로 래핑하여 반환
            resp.setOurUsers(saved);
            resp.setMessage("회원 저장 성공");
            resp.setStatusCode(200);

        } catch (Exception e) {
            resp.setStatusCode(500);
            resp.setError(e.getMessage());
        }

        return resp;
    }

    public JwtResponse signIn(SigninRequestDto signinRequest){
        JwtResponse response = new JwtResponse();

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(signinRequest.getEmail(),signinRequest.getPassword()));
            var user = userRepo.findByEmail(signinRequest.getEmail()).orElseThrow();
            System.out.println("USER IS: "+ user);
            var jwt = jwtTokenProvider.generateToken(user);
            var refreshToken = jwtTokenProvider.generateRefreshToken(new HashMap<>(), user);
            response.setStatusCode(200);
            response.setToken(jwt);
            response.setNickname(user.getNickname());
            response.setRefreshToken(refreshToken);
            response.setExpirationTime("24Hr");
            response.setMessage("로그인 성공");
        }catch (Exception e){
            response.setStatusCode(500);
            response.setError(e.getMessage());
        }
        return response;
    }

    public JwtResponse refreshToken(JwtRequest refreshTokenRequest){
        JwtResponse response = new JwtResponse();
        try {
            String ourName = jwtTokenProvider.extractUsername(refreshTokenRequest.getToken());
            User users = userRepo.findByEmail(ourName).orElseThrow();
            if (jwtTokenProvider.isTokenValid(refreshTokenRequest.getToken(), users)) {
                var jwt = jwtTokenProvider.generateToken(users);
                response.setStatusCode(200);
                response.setToken(jwt);
                response.setRefreshToken(refreshTokenRequest.getToken());
                response.setExpirationTime("24Hr");
                response.setMessage("재발급 성공");
            } else {
                response.setStatusCode(401);
                response.setError("유효하지 않은 토큰");
            }
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setError(e.getMessage());
        }
        return response;
    }


    public JwtResponse validateToken(JwtRequest validateTokenRequest) {
        JwtResponse response = new JwtResponse();
        try {
            String ourName = jwtTokenProvider.extractUsername(validateTokenRequest.getToken());
            User user = userRepo.findByEmail(ourName).orElseThrow(() -> new Exception("사용자를 찾을 수 없습니다."));
            // 토큰 유효성 검사
            if (jwtTokenProvider.isTokenValid(validateTokenRequest.getToken(), user)) {
                response.setStatusCode(200); // OK
                response.setMessage("토큰이 유효합니다.");
            } else {
                response.setStatusCode(401); // Unauthorized
                response.setMessage("유효하지 않은 토큰입니다.");
            }
        } catch (Exception e) {
            // 예외 발생 시
            response.setStatusCode(500); // Internal Server Error
            response.setError(e.getMessage());
        }
        return response;
    }
}
