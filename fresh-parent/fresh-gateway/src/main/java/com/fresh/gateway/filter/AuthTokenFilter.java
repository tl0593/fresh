package com.fresh.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.fresh.common.constant.RedisKeyConstant;
import com.fresh.common.dto.UserContextDTO;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.gateway.config.FreshGatewayProperties;
import com.fresh.gateway.util.GatewayFilterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final FreshGatewayProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = GatewayFilterUtils.sanitizePath(request.getURI().getPath());
        if (GatewayFilterUtils.isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String token = request.getHeaders().getFirst(properties.getTokenHeader());
        if (token == null || token.isBlank()) {
            return GatewayFilterUtils.writeResult(exchange, HttpStatus.UNAUTHORIZED,
                    ErrorCodeEnum.UNAUTHORIZED.getCode(), ErrorCodeEnum.UNAUTHORIZED.getMsg());
        }

        return redisTemplate.opsForValue().get(RedisKeyConstant.USER_TOKEN + token)
                .flatMap(json -> {
                    UserContextDTO ctx = JSON.parseObject(json, UserContextDTO.class);
                    if (ctx == null) {
                        return GatewayFilterUtils.writeResult(exchange, HttpStatus.UNAUTHORIZED,
                                ErrorCodeEnum.UNAUTHORIZED.getCode(), ErrorCodeEnum.UNAUTHORIZED.getMsg());
                    }
                    // ServerWebExchange attributes 底层 ConcurrentHashMap 不允许 null value
                    if (ctx.getUserId() != null) {
                        exchange.getAttributes().put(GatewayFilterUtils.ATTR_USER_ID, ctx.getUserId());
                    }
                    if (ctx.getAdminId() != null) {
                        exchange.getAttributes().put(GatewayFilterUtils.ATTR_ADMIN_ID, ctx.getAdminId());
                    }
                    exchange.getAttributes().put(GatewayFilterUtils.ATTR_TOKEN, token);

                    ServerHttpRequest.Builder builder = request.mutate()
                            .header(properties.getTokenHeader(), token);
                    if (ctx.getUserId() != null) {
                        builder.header("X-User-Id", String.valueOf(ctx.getUserId()));
                    }
                    if (ctx.getAdminId() != null) {
                        builder.header("X-Admin-Id", String.valueOf(ctx.getAdminId()));
                    }
                    if (ctx.getRoleType() != null) {
                        builder.header("X-Role-Type", String.valueOf(ctx.getRoleType()));
                    }
                    return chain.filter(exchange.mutate().request(builder.build()).build());
                })
                .switchIfEmpty(GatewayFilterUtils.writeResult(exchange, HttpStatus.UNAUTHORIZED,
                        ErrorCodeEnum.UNAUTHORIZED.getCode(), ErrorCodeEnum.UNAUTHORIZED.getMsg()));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
