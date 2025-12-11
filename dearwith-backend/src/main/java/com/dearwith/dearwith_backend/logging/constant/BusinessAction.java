package com.dearwith.dearwith_backend.logging.constant;

public interface BusinessAction {

    interface Auth {
        // 이메일
        String EMAIL_SIGN_IN_SUCCESS = "EMAIL_SIGN_IN_SUCCESS";
        String EMAIL_SIGN_IN_FAILED = "EMAIL_SIGN_IN_FAILED";
        String LOGOUT = "LOGOUT";

        // 토큰 재발급
        String TOKEN_REISSUE_SUCCESS = "TOKEN_REISSUE_SUCCESS";
        String TOKEN_REISSUE_FAILED = "TOKEN_REISSUE_FAILED";

        // 카카오
        String KAKAO_SIGN_IN_SUCCESS = "KAKAO_SIGN_IN_SUCCESS";
        String KAKAO_SIGN_IN_FAILED = "KAKAO_SIGN_IN_FAILED";
        String KAKAO_NEED_SIGNUP = "KAKAO_NEED_SIGNUP";

        //  애플
        String APPLE_SIGN_IN_SUCCESS = "APPLE_SIGN_IN_SUCCESS";
        String APPLE_SIGN_IN_FAILED = "APPLE_SIGN_IN_FAILED";
        String APPLE_NEED_SIGNUP = "APPLE_NEED_SIGNUP";

        // Owner 검증
        String OWNER_VALIDATE_REQUESTER_NOT_FOUND = "OWNER_VALIDATE_REQUESTER_NOT_FOUND";
        String OWNER_VALIDATE_UNAUTHORIZED = "OWNER_VALIDATE_UNAUTHORIZED";
    }

    interface User {
        String USER_SIGN_UP           = "USER_SIGN_UP";
        String USER_SIGN_UP_BY_ADMIN  = "USER_SIGN_UP_BY_ADMIN";
        String USER_SOCIAL_SIGN_UP    = "USER_SOCIAL_SIGN_UP";

        String USER_PASSWORD_RESET    = "USER_PASSWORD_RESET";
        String USER_PASSWORD_CHANGE   = "USER_PASSWORD_CHANGE";

        String USER_NICKNAME_CHANGE   = "USER_NICKNAME_CHANGE";
        String USER_DELETE            = "USER_DELETE";

        String USER_SUSPEND           = "USER_SUSPEND";
        String USER_WRITE_RESTRICT    = "USER_WRITE_RESTRICT";
        String USER_UNSUSPEND         = "USER_UNSUSPEND";
    }

    interface Push {
        String PUSH_SEND_FAILED      = "PUSH_SEND_FAILED";
        String PUSH_DEVICE_REGISTER  = "PUSH_DEVICE_REGISTER";
        String PUSH_DEVICE_UNREGISTER = "PUSH_DEVICE_UNREGISTER";
    }
}