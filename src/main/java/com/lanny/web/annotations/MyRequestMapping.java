package com.lanny.web.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.HttpMethod;

/**
 * @author Lanny Yao
 * @date 3/11/2019 7:38 PM
 */
@Target(value = {ElementType.TYPE, ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {

    String value() default "";

    HttpMethod httpMethod() default HttpMethod.GET;
}
