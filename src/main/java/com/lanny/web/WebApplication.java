package com.lanny.web;

import com.lanny.web.dispatcher.MyBeanFactory;
import com.lanny.web.dispatcher.MyDispatcherServlet;
import com.lanny.web.dispatcher.PackageScanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

    @Bean
    @Primary
    public ServletRegistrationBean getServletRegistrationBean(MyBeanFactory beanFactory, PackageScanner scanner) {
        ServletRegistrationBean bean = new ServletRegistrationBean(new MyDispatcherServlet(beanFactory, scanner));
        bean.addUrlMappings("/demo/*");
        bean.addInitParameter("location", "application.properties");
        return bean;
    }
}
