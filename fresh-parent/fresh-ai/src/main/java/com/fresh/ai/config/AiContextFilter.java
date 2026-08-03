package com.fresh.ai.config;

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
public class AiContextFilter extends OncePerRequestFilter {

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
            String adminId = request.getHeader("X-Admin-Id");
            if (adminId != null && !adminId.isBlank()) {
                UserContextDTO ctx = ContextUtil.get();
                if (ctx == null) {
                    ctx = new UserContextDTO();
                }
                ctx.setAdminId(Long.parseLong(adminId));
                ctx.setRoleType(2);
                ContextUtil.set(ctx);
            }
            filterChain.doFilter(request, response);
        } finally {
            ContextUtil.clear();
        }
    }
}
