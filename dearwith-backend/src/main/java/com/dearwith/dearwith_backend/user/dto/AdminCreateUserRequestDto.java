package com.dearwith.dearwith_backend.user.dto;

import com.dearwith.dearwith_backend.auth.dto.AgreementDto;
import com.dearwith.dearwith_backend.user.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminCreateUserRequestDto{

    private String email;

    private String password;

    private String nickname;

    private List<AgreementDto> agreements;

    private Role role;
}
