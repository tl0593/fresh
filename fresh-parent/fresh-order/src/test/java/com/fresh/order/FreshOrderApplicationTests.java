package com.fresh.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = FreshOrderApplication.class)
@TestPropertySource(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.stream.function.definition="
})
class FreshOrderApplicationTests {

    @Test
    void contextLoads() {
    }
}
