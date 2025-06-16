//package com.UST.ItemBridge.config;
//
//import org.apache.camel.component.servlet.CamelHttpTransportServlet;
//import org.springframework.boot.web.servlet.ServletRegistrationBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class CamelServletConfig {
//
//    @Bean
//    public ServletRegistrationBean camelServletRegistrationBean() {
//        ServletRegistrationBean servlet = new ServletRegistrationBean(
//                new CamelHttpTransportServlet(), "/camel/*");
//        servlet.setName("camelServlet");
//        return servlet;
//    }
//}
