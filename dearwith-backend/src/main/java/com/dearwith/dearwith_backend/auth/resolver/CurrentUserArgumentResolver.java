package com.dearwith.dearwith_backend.auth.resolver;

import com.dearwith.dearwith_backend.auth.annotation.CurrentUser;
import com.dearwith.dearwith_backend.auth.entity.CustomUserDetails;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @CurrentUser 붙어 있고, 타입이 UUID 또는 CustomUserDetails 인 파라미터만 지원
        if (!parameter.hasParameterAnnotation(CurrentUser.class)) {
            return false;
        }

        Class<?> type = parameter.getParameterType();
        return UUID.class.equals(type) || CustomUserDetails.class.equals(type);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        // 인증 안 된 경우 (비로그인) → null 리턴
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails cud)) {
            return null;
        }

        Class<?> type = parameter.getParameterType();

        // @CurrentUser UUID userId
        if (UUID.class.equals(type)) {
            return cud.getId();
        }

        // @CurrentUser CustomUserDetails currentUser
        if (CustomUserDetails.class.equals(type)) {
            return cud;
        }

        return null;
    }
}