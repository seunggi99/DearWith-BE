package com.dearwith.dearwith_backend.user.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


/**
 * 약관 동의 정보
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Agreement {
    private AgreementType type;     // 어떤 약관에 동의했는지
    private LocalDateTime agreedAt; // 동의 시각
}