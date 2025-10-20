package com.zendapag.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${zendapag.api.version:1.0.0}")
    private String apiVersion;

    @Value("${zendapag.api.base-url:http://localhost:8081}")
    private String apiBaseUrl;

    @Value("${zendapag.api.staging-url:https://api-staging.zendapag.com}")
    private String apiStagingUrl;

    @Value("${zendapag.api.production-url:https://api.zendapag.com}")
    private String apiProductionUrl;

    @Bean
    public OpenAPI zendapagOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .externalDocs(externalDocs())
                .servers(servers())
                .components(securityComponents())
                .security(List.of(
                        new SecurityRequirement().addList("bearerAuth"),
                        new SecurityRequirement().addList("apiKeyAuth")
                ));
    }

    private Info apiInfo() {
        return new Info()
                .title("Zendapag PIX Payment API")
                .description("""
                    # Zendapag PIX Payment Platform API

                    Complete PIX payment processing platform for businesses in Brazil.

                    ## Features
                    - **PIX Payments**: Create, track, and manage PIX transactions
                    - **Merchant Management**: Complete merchant account management
                    - **Webhooks**: Real-time payment notifications
                    - **Reports**: Comprehensive financial analytics and reporting
                    - **Security**: JWT authentication with role-based access control
                    - **Rate Limiting**: Built-in API rate limiting protection

                    ## Getting Started
                    1. Register your merchant account via `/api/v1/auth/register`
                    2. Complete KYC verification
                    3. Configure your webhook endpoints
                    4. Start processing payments!

                    ## Rate Limits
                    - **Payments**: 100 requests/minute
                    - **Merchants**: 50 requests/minute
                    - **Webhooks**: 20 requests/minute
                    - **Reports**: 10 requests/minute

                    ## Support
                    For technical support, contact our development team.
                    """)
                .version(apiVersion)
                .contact(apiContact())
                .license(apiLicense());
    }

    private Contact apiContact() {
        return new Contact()
                .name("Zendapag Development Team")
                .email("dev@zendapag.com")
                .url("https://zendapag.com/support");
    }

    private License apiLicense() {
        return new License()
                .name("Proprietary")
                .url("https://zendapag.com/terms");
    }

    private ExternalDocumentation externalDocs() {
        return new ExternalDocumentation()
                .description("Zendapag Developer Documentation")
                .url("https://docs.zendapag.com");
    }

    private List<Server> servers() {
        return List.of(
                new Server()
                        .url(apiBaseUrl)
                        .description("Development Server"),
                new Server()
                        .url(apiStagingUrl)
                        .description("Staging Server"),
                new Server()
                        .url(apiProductionUrl)
                        .description("Production Server")
        );
    }

    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", bearerAuthScheme())
                .addSecuritySchemes("apiKeyAuth", apiKeyAuthScheme());
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Bearer token authentication. Include the token in the Authorization header as 'Bearer {token}'.");
    }

    private SecurityScheme apiKeyAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("API Key authentication. Include your API key in the X-API-Key header.");
    }
}