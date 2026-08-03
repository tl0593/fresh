package com.fresh.data;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.stream.function.definition="
})
class FreshDataApplicationTests {

    @Test
    void contextLoads() {
    }

}
