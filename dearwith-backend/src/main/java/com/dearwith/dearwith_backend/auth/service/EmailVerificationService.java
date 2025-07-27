package com.dearwith.dearwith_backend.auth.service;

import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.dearwith.dearwith_backend.common.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private final StringRedisTemplate redis;
    private final MailSender mailSender;  // 이메일 전송 추상화 컴포넌트

    public EmailVerificationService(StringRedisTemplate redis, MailSender mailSender) {
        this.redis = redis;
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String email) {
        String code = UUID.randomUUID().toString().substring(0, 6);
        String key  = "signup:verify:" + email;
        // 10분 TTL
        redis.opsForValue().set(key, code, Duration.ofMinutes(10));

        // SimpleMailMessage 생성
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[DearWith] 인증 코드");
        message.setText(String.format(
                        "안녕하세요, DearWith입니다.\n\n" +
                        "이메일 인증 코드는 [%s] 입니다.\n" +
                        "인증 코드는 10분간 유효합니다.\n\n" +
                        "본 메일은 발신 전용입니다.", code
        ));
        mailSender.send(message);
    }

    public void verifyCode(String email, String code) {
        String key = "signup:verify:" + email;
        String saved = redis.opsForValue().get(key);
        if (saved == null || !saved.equals(code)) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
        redis.delete(key);
    }
}
