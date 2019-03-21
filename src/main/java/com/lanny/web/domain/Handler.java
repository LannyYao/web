package com.lanny.web.domain;

import com.lanny.web.annotations.MyRequestBody;
import com.lanny.web.annotations.MyRequestParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.springframework.http.HttpMethod;

/**
 * @author Lanny Yao
 * @date 3/12/2019 10:50 AM
 */
@Getter
public class Handler {

    private Object controller;

    private Method method;

    private Pattern pattern;

    private HttpMethod httpMethod;

    private List<Argument> arguments;

    public Handler(Object controller, Method method, Pattern pattern, HttpMethod httpMethod) {
        this.controller = controller;
        this.method = method;
        this.pattern = pattern;
        this.httpMethod = httpMethod;

        arguments = new ArrayList<>();
        putParamIndexMapping(method);
    }

    private void putParamIndexMapping(Method method) {
        Annotation[][] pa = method.getParameterAnnotations();
        Class<?>[] pt = method.getParameterTypes();

        for (int i = 0; i < pa.length; i++) {
            Class<?> type = pt[i];

            //获取方法中加了注解的参数
            for (Annotation a : pa[i]) {
                if (a instanceof MyRequestParam) {
                    String paramName = ((MyRequestParam) a).value();
                    if (!"".equals(paramName.trim())) {
                        arguments.add(new Argument(i, type, paramName));
                    }
                }

                if (a instanceof MyRequestBody) {
                    String paramName = ((MyRequestBody) a).value();

                    if ("".equals(paramName.trim())) {
                        paramName = MyRequestBody.class.getName();
                    }

                    arguments.add(new Argument(i, type, paramName));
                }
            }

            //获取方法中的request和response参数
            if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                arguments.add(new Argument(i, type, type.getName()));
            }
        }
    }
}
