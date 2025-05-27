package com.dearwith.dearwith_backend.auth.service.impl;

import com.dearwith.dearwith_backend.common.exception.InvalidVerificationCodeException;
import com.dearwith.dearwith_backend.auth.service.EmailVerificationService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private final StringRedisTemplate redis;
    private final MailSender mailSender;  // 이메일 전송 추상화 컴포넌트

    public EmailVerificationServiceImpl(StringRedisTemplate redis, MailSender mailSender) {
        this.redis = redis;
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationCode(String email) {
        String code = UUID.randomUUID().toString().substring(0, 6);
        String key  = "signup:verify:" + email;
        // 10분 TTL
        redis.opsForValue().set(key, code, Duration.ofMinutes(10));

        // SimpleMailMessage 생성
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[DearWith] 인증 코드");
        message.setText("코드: " + code);

        mailSender.send(message);
    }

    @Override
    public void verifyCode(String email, String code) {
        String key = "signup:verify:" + email;
        String saved = redis.opsForValue().get(key);
        if (saved == null || !saved.equals(code)) {
            throw new InvalidVerificationCodeException();
        }
        // 한 번만 사용
        redis.delete(key);
    }
}
