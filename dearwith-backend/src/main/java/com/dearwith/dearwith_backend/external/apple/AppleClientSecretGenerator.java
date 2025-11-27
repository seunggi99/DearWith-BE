package com.dearwith.dearwith_backend.external.apple;

import com.dearwith.dearwith_backend.common.exception.BusinessException;
import com.dearwith.dearwith_backend.common.exception.ErrorCode;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleClientSecretGenerator {

    private final AppleAuthProperties props;

    // 캐싱용
    private volatile String cachedClientSecret;
    private volatile long cachedExpireEpoch = 0L;

    /**
     * 애플 OAuth client_secret 생성 (ES256)
     * - 이미 만들어 둔 값이 30분 이상 유효하면 재사용
     */
    public String generate() {
        long now = Instant.now().getEpochSecond();

        // 아직 유효한 client_secret 있으면 재사용
        if (cachedClientSecret != null && cachedExpireEpoch - now > 1800) {
            return cachedClientSecret;
        }

        try {
            // 1) Base64 로 인코딩된 p8 키를 ECPrivateKey로 디코딩
            ECPrivateKey privateKey = loadPrivateKeyFromBase64(props.getSignin().getPrivateKey());

            // 2) ES256 서명자 생성
            JWSSigner signer = new ECDSASigner(privateKey);

            // 3) JWT Header
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(props.getSignin().getKeyId())
                    .type(JOSEObjectType.JWT)
                    .build();

            // 4) JWT Claims
            long exp = now + 60L * 60L * 2; // 2시간 유효
            cachedExpireEpoch = exp;

            com.nimbusds.jwt.JWTClaimsSet claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                    .issuer(props.getIssuer())                 // teamId
                    .issueTime(new Date(now * 1000))
                    .expirationTime(new Date(exp * 1000))
                    .audience(props.getAudience())             // https://appleid.apple.com
                    .subject(props.getSignin().getClientId())  // Service ID (com.dearwith.login)
                    .build();

            // 5) 서명
            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(signer);

            // 6) 캐싱 및 반환
            cachedClientSecret = signedJWT.serialize();
            return cachedClientSecret;

        } catch (Exception e) {
            log.error("Failed to generate Apple client_secret", e);
            throw BusinessException.withMessage(
                    ErrorCode.OPERATION_FAILED,
                    "애플 로그인 구성 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    /**
     * ENV / YML 에 저장된 Base64 문자열을 ECPrivateKey로 변환
     */
    private ECPrivateKey loadPrivateKeyFromBase64(String base64Key) throws Exception {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("Apple private key(base64)가 비어 있습니다.");
        }

        // 1) Base64 디코딩 → PKCS#8 바이너리
        byte[] pkcs8Bytes = Base64.getDecoder().decode(base64Key);

        // 2) KeySpec 생성
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);

        // 3) EC KeyFactory로 변환
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return (ECPrivateKey) keyFactory.generatePrivate(keySpec);
    }
}