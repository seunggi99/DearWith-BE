package com.dearwith.dearwith_backend.external.apple;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.*;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleIdTokenVerifier {

    private final AppleAuthProperties props;

    private volatile RemoteJWKSet<SecurityContext> jwkSet;

    private RemoteJWKSet<SecurityContext> getJwkSet() throws MalformedURLException {
        if (jwkSet == null) {
            jwkSet = new RemoteJWKSet<>(new URL(props.getSignin().getJwksUrl()));
        }
        return jwkSet;
    }

    public AppleIdTokenClaims verify(String idToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(idToken);

            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                    signedJWT.getHeader().getAlgorithm(),
                    getJwkSet()
            );
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(keySelector);

            SecurityContext ctx = null;
            var claims = jwtProcessor.process(signedJWT, ctx);

            // 기본 검증 (iss, aud, exp 등)
            String iss = claims.getIssuer();
            List<String> aud = claims.getAudience();
            Instant exp = claims.getExpirationTime().toInstant();

            if (!"https://appleid.apple.com".equals(iss)) {
                throw new IllegalStateException("Invalid issuer: " + iss);
            }
            if (aud == null || !aud.contains(props.getSignin().getClientId())) {
                throw new IllegalStateException("Invalid audience: " + aud);
            }
            if (exp.isBefore(Instant.now())) {
                throw new IllegalStateException("ID token expired");
            }

            String sub = claims.getSubject();
            String email = claims.getStringClaim("email");
            Boolean emailVerified = claims.getBooleanClaim("email_verified");
            Boolean isPrivateEmail = claims.getBooleanClaim("is_private_email");

            return new AppleIdTokenClaims(
                    sub,
                    email,
                    emailVerified != null && emailVerified,
                    isPrivateEmail != null && isPrivateEmail
            );
        } catch (ParseException | MalformedURLException | BadJOSEException | JOSEException e) {
            log.error("Failed to verify Apple id_token", e);
            throw BusinessException.withMessage(
                    ErrorCode.OPERATION_FAILED,
                    "애플 계정 인증에 실패했습니다. 잠시 후 다시 시도해주세요.");
        } catch (Exception e) {
            log.error("Apple id_token validation failed", e);
            throw BusinessException.withMessage(
                    ErrorCode.OPERATION_FAILED,
                    "애플 계정 정보가 올바르지 않습니다.");
        }
    }
}
