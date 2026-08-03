package com.fresh.gateway.filter;

import com.fresh.gateway.config.FreshGatewayProperties;
import com.fresh.gateway.entity.GatewayAccessLog;
import com.fresh.gateway.service.GatewayAccessLogService;
import com.fresh.gateway.util.GatewayFilterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AccessLogFilter implements GlobalFilter, Ordered {

    private final GatewayAccessLogService accessLogService;
    private final FreshGatewayProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isAccessLogEnable()) {
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();
        exchange.getAttributes().put(GatewayFilterUtils.ATTR_START_TIME, startTime);

        return chain.filter(exchange)
                .doFinally(signal -> recordLog(exchange, startTime));
    }

    private void recordLog(ServerWebExchange exchange, long startTime) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        GatewayAccessLog log = new GatewayAccessLog();
        log.setRequestIp(GatewayFilterUtils.getClientIp(request));
        log.setRequestUrl(GatewayFilterUtils.sanitizePath(request.getURI().getPath()));
        log.setRequestMethod(request.getMethod() == null ? "UNKNOWN" : request.getMethod().name());
        log.setUserId(exchange.getAttribute(GatewayFilterUtils.ATTR_USER_ID));
        log.setToken(exchange.getAttribute(GatewayFilterUtils.ATTR_TOKEN));
        log.setResponseCode(response.getStatusCode() == null ? 200 : response.getStatusCode().value());
        log.setCostTime((int) (System.currentTimeMillis() - startTime));
        log.setCreateTime(LocalDateTime.now());

        accessLogService.offer(log);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
