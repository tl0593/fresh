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
public class BlackIpFilter implements GlobalFilter, Ordered {

    private final GatewayCacheService cacheService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = GatewayFilterUtils.sanitizePath(request.getURI().getPath());
        if (GatewayFilterUtils.isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String clientIp = GatewayFilterUtils.getClientIp(request);
        return cacheService.isBlackIp(clientIp)
                .flatMap(black -> {
                    if (Boolean.TRUE.equals(black)) {
                        return GatewayFilterUtils.writeResult(exchange, HttpStatus.FORBIDDEN,
                                ErrorCodeEnum.FORBIDDEN.getCode(), "禁止访问");
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
