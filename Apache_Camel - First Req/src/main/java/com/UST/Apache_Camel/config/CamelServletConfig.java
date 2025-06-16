package com.UST.Apache_Camel.config;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelServletConfig {

    @Bean
    public ServletRegistrationBean<CamelHttpTransportServlet> servletRegistrationBean() {
        ServletRegistrationBean<CamelHttpTransportServlet> registration =
            new ServletRegistrationBean<>(new CamelHttpTransportServlet(), "/camel/*");
        registration.setName("CamelServlet");
        return registration;
    }
}