package com.fresh.gateway.util;

import com.alibaba.fastjson2.JSON;
import com.fresh.common.base.Result;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class GatewayFilterUtils {

    public static final String ATTR_USER_ID = "gatewayUserId";
    public static final String ATTR_ADMIN_ID = "gatewayAdminId";
    public static final String ATTR_TOKEN = "gatewayToken";
    public static final String ATTR_START_TIME = "gatewayStartTime";

    private static final Set<String> WHITE_LIST = Set.of(
            "/api/user/mini/login",
            "/api/user/admin/login",
            "/gateway/health",
            // 微信异步通知无用户 Token，正式环境依赖签名校验
            "/api/order/order/pay/notify",
            // 首页/分类浏览（未登录也可看）
            "/api/goods/goods/hot",
            "/api/goods/goods/list",
            "/api/goods/category/tree",
            "/api/goods/seckill/list",
            "/api/goods/group/list"
    );

    private GatewayFilterUtils() {
    }

    public static boolean isWhiteListed(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        // 已上传图片静态资源需免登录访问（小程序/管理端预览）
        if (path.startsWith("/api/goods/upload/") && !path.equals("/api/goods/upload/image")) {
            return true;
        }
        // 商品详情只读（首页折扣区补图）
        if (path.matches("^/api/goods/goods/\\d+$")) {
            return true;
        }
        // 商品评价列表/统计只读（未登录也可看）
        if (path.matches("^/api/goods/comment/list/\\d+$")) {
            return true;
        }
        if (path.matches("^/api/goods/comment/stats/\\d+$")) {
            return true;
        }
        return WHITE_LIST.contains(path) || path.contains("/feign/");
    }

    public static String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddress() == null ? "unknown"
                    : request.getRemoteAddress().getAddress().getHostAddress();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    public static String sanitizePath(String path) {
        if (path == null) {
            return "";
        }
        return path.replaceAll("[<>\"']", "");
    }

    public static Mono<Void> writeResult(ServerWebExchange exchange, HttpStatus status, int code, String msg) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = JSON.toJSONString(Result.fail(code, msg));
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
