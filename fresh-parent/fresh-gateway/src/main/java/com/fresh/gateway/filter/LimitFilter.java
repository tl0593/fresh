package com.fresh.gateway.filter;

import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.gateway.service.GatewayCacheService;
import com.fresh.gateway.util.GatewayFilterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class LimitFilter implements GlobalFilter, Ordered {

    private final GatewayCacheService cacheService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = GatewayFilterUtils.sanitizePath(request.getURI().getPath());
        if (GatewayFilterUtils.isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String clientIp = GatewayFilterUtils.getClientIp(request);
        return cacheService.isRateLimitExceeded(path, clientIp)
                .flatMap(exceeded -> {
                    if (Boolean.TRUE.equals(exceeded)) {
                        return GatewayFilterUtils.writeResult(exchange, HttpStatus.TOO_MANY_REQUESTS,
                                ErrorCodeEnum.TOO_MANY_REQUESTS.getCode(),
                                ErrorCodeEnum.TOO_MANY_REQUESTS.getMsg());
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -150;
    }
}
