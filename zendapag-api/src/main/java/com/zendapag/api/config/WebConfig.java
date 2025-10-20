package com.zendapag.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.config.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebMvc
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${zendapag.cors.allowed-origins:http://localhost:3000,https://localhost:3000}")
    private String[] allowedOrigins;

    @Value("${zendapag.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String[] allowedMethods;

    @Value("${zendapag.cors.allowed-headers:*}")
    private String[] allowedHeaders;

    @Value("${zendapag.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${zendapag.request-logging.enabled:true}")
    private boolean requestLoggingEnabled;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders)
                .allowCredentials(allowCredentials)
                .maxAge(3600); // 1 hour preflight cache
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setExposedHeaders(List.of("X-Total-Count", "X-Rate-Limit-Remaining", "X-Rate-Limit-Retry-After"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();

        if (requestLoggingEnabled) {
            filter.setIncludeClientInfo(true);
            filter.setIncludeQueryString(true);
            filter.setIncludePayload(false); // Disable payload logging for security
            filter.setIncludeHeaders(true);
            filter.setMaxPayloadLength(1000);
            filter.setAfterMessagePrefix("REQUEST: ");
        }

        return filter;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static resources with caching
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365))
                                           .cachePublic());

        // Swagger UI resources
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
                .setCacheControl(CacheControl.maxAge(Duration.ofHours(1)));

        // API documentation resources
        registry.addResourceHandler("/api-docs/**")
                .addResourceLocations("classpath:/META-INF/resources/")
                .setCacheControl(CacheControl.maxAge(Duration.ofMinutes(30)));
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .favorPathExtension(false)
                .ignoreAcceptHeader(false)
                .defaultContentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .mediaType("json", org.springframework.http.MediaType.APPLICATION_JSON)
                .mediaType("xml", org.springframework.http.MediaType.APPLICATION_XML);
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add custom interceptors here if needed
        registry.addInterceptor(new RequestMetricsInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health/**", "/actuator/**");
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(30000); // 30 seconds
        configurer.setTaskExecutor(asyncTaskExecutor());
    }

    @Bean
    public org.springframework.core.task.TaskExecutor asyncTaskExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor =
            new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("api-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return executor;
    }

    // Custom interceptor for request metrics
    public static class RequestMetricsInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                               jakarta.servlet.http.HttpServletResponse response,
                               Object handler) {
            request.setAttribute("startTime", System.currentTimeMillis());
            return true;
        }

        @Override
        public void afterCompletion(jakarta.servlet.http.HttpServletRequest request,
                                  jakarta.servlet.http.HttpServletResponse response,
                                  Object handler,
                                  Exception ex) {
            Long startTime = (Long) request.getAttribute("startTime");
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Request {} {} completed in {}ms with status {}",
                    request.getMethod(), request.getRequestURI(), duration, response.getStatus());
            }
        }
    }
}