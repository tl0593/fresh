package com.fresh.order.config;

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
public class OrderContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            UserContextDTO ctx = new UserContextDTO();
            boolean hasCtx = false;
            String userId = request.getHeader("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                ctx.setUserId(Long.parseLong(userId));
                hasCtx = true;
            }
            String adminId = request.getHeader("X-Admin-Id");
            if (adminId != null && !adminId.isBlank()) {
                ctx.setAdminId(Long.parseLong(adminId));
                hasCtx = true;
            }
            String roleType = request.getHeader("X-Role-Type");
            if (roleType != null && !roleType.isBlank()) {
                ctx.setRoleType(Integer.parseInt(roleType));
            } else if (ctx.getAdminId() != null) {
                ctx.setRoleType(2);
            } else if (ctx.getUserId() != null) {
                ctx.setRoleType(1);
            }
            if (hasCtx) {
                ContextUtil.set(ctx);
            }
            filterChain.doFilter(request, response);
        } finally {
            ContextUtil.clear();
        }
    }
}
