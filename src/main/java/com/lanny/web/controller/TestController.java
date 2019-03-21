package com.lanny.web.controller;

import com.lanny.web.annotations.MyAutowired;
import com.lanny.web.annotations.MyController;
import com.lanny.web.annotations.MyRequestBody;
import com.lanny.web.annotations.MyRequestMapping;
import com.lanny.web.annotations.MyRequestParam;
import com.lanny.web.service.TestService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

/**
 * @author Lanny Yao
 * @date 3/11/2019 7:33 PM
 */
@Slf4j
@MyController
@MyRequestMapping(value = "/demo")
public class TestController {

    @MyAutowired
    private TestService testService;

    private Map<String, Object> data = new HashMap<>();

    @MyRequestMapping
    public Map<String, Object> request(HttpServletRequest request, HttpServletResponse response) {

        log.info("Query all");
        return data;
    }

    @MyRequestMapping(value = "/query", httpMethod = HttpMethod.GET)
    public Object query(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("id") String id) {
        log.info("Query data for {}", id);
        return data.get(id);
    }

    @MyRequestMapping(value = "/save", httpMethod = HttpMethod.POST)
    public Map<String, String> save(HttpServletRequest request, HttpServletResponse response,
            @MyRequestBody Object object) {
        String id = UUID.randomUUID().toString().replace("-", "");
        log.info("Save data for {}", id);

        data.put(id, object);
        Map<String, String> resp = new HashMap<>();
        resp.put("id", id);

        return resp;
    }

}
