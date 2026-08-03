package com.fresh.gateway.config;

import com.fresh.common.base.Result;
import com.fresh.common.util.JsonUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class HealthRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> healthRoute() {
        return route()
                .GET("/gateway/health", request -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("service", "fresh-gateway");
                    data.put("time", LocalDate.now().toString());
                    Result<Map<String, Object>> result = Result.success(data);
                    result.setMsg("网关服务正常");
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(JsonUtils.toJson(result));
                })
                .build();
    }
}
