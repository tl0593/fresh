package com.fresh.goods.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 本地上传静态资源由 {@link com.fresh.goods.controller.LocalFileController} 提供 */
@Configuration
public class GoodsWebMvcConfig implements WebMvcConfigurer {
}
