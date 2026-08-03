package com.fresh.user.config;

import com.fresh.common.constant.RedisKeyConstant;
import com.fresh.common.dto.UserContextDTO;
import com.fresh.common.util.ContextUtil;
import com.fresh.common.util.JsonUtils;
import com.fresh.common.util.RedisUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@Order(1)
@RequiredArgsConstructor
public class UserContextFilter extends OncePerRequestFilter {

    private static final Set<String> WHITE_LIST = Set.of(
            "/mini/login", "/admin/login", "/feign/"
    );

    private final RedisUtils redisUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            if (WHITE_LIST.stream().anyMatch(path::contains)) {
                filterChain.doFilter(request, response);
                return;
            }
            String token = request.getHeader("Authorization");
            if (token != null && !token.isBlank()) {
                String json = redisUtils.get(RedisKeyConstant.USER_TOKEN + token);
                if (json != null) {
                    ContextUtil.set(JsonUtils.fromJson(json, UserContextDTO.class));
                }
            } else {
                String userId = request.getHeader("X-User-Id");
                if (userId != null && !userId.isBlank()) {
                    UserContextDTO ctx = new UserContextDTO();
                    ctx.setUserId(Long.parseLong(userId));
                    ctx.setRoleType(1);
                    ContextUtil.set(ctx);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            ContextUtil.clear();
        }
    }
}
