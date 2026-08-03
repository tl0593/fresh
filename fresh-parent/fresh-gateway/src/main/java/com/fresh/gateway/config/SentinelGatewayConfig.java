package com.fresh.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class SentinelGatewayConfig {

    private static final String[] ROUTE_IDS = {
            "fresh-user", "fresh-goods", "fresh-order",
            "fresh-ai", "fresh-message", "fresh-data"
    };

    @PostConstruct
    public void initRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        for (String routeId : ROUTE_IDS) {
            rules.add(new GatewayFlowRule(routeId)
                    .setCount(500)
                    .setIntervalSec(1));
        }
        GatewayRuleManager.loadRules(rules);

        BlockRequestHandler blockHandler = (ServerWebExchange exchange, Throwable throwable) ->
                ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{\"code\":503,\"msg\":\"服务器繁忙，请稍后再试\",\"data\":null}");
        GatewayCallbackManager.setBlockHandler(blockHandler);
    }
}
