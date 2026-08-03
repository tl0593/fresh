package com.fresh.message.config;

import com.fresh.common.dto.UserContextDTO;
import com.fresh.common.util.ContextUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class MessageContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String userId = request.getHeader("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                UserContextDTO ctx = new UserContextDTO();
                ctx.setUserId(Long.parseLong(userId));
                ctx.setRoleType(1);
                ContextUtil.set(ctx);
            }
            filterChain.doFilter(request, response);
        } finally {
            ContextUtil.clear();
        }
    }
}
