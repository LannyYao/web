package com.lanny.web.dispatcher;

import static com.lanny.web.GlobalConstants.CONTENT_TYPE_APPLICATION_JSON;
import static com.lanny.web.GlobalConstants.EMPTY_STRING;
import static com.lanny.web.GlobalConstants.PROPERTIES_LOCATION;
import static com.lanny.web.GlobalConstants.SCAN_PACKAGE;
import static com.lanny.web.GlobalConstants.SLASH;
import static com.lanny.web.utils.CaseUtils.lowerFirstCase;
import static com.lanny.web.utils.Converter.convertToInt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanny.web.annotations.MyAutowired;
import com.lanny.web.annotations.MyController;
import com.lanny.web.annotations.MyRequestBody;
import com.lanny.web.annotations.MyRequestMapping;
import com.lanny.web.annotations.MyService;
import com.lanny.web.domain.Argument;
import com.lanny.web.domain.BeanContainer;
import com.lanny.web.domain.Handler;
import com.lanny.web.exception.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.util.StreamUtils;

/**
 * @author Lanny Yao
 * @date 3/11/2019 7:45 PM
 */
@Slf4j
@RequiredArgsConstructor
public class MyDispatcherServlet extends HttpServlet {

    private final MyBeanFactory beanFactory;

    private final PackageScanner scanner;

    private Properties properties = new Properties();

    private List<Handler> handlerMapping = new ArrayList<>();

    /**
     * 等待Get请求
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.warn("doGet");
        doDispatcher(req, resp, HttpMethod.GET);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.warn("doPost");

        doDispatcher(req, resp, HttpMethod.POST);
        resp.setStatus(201);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        if (resp.getContentType() == null) {
            resp.setContentType(CONTENT_TYPE_APPLICATION_JSON);
        }

        HttpMethod httpMethod = HttpMethod.valueOf(req.getMethod());
        switch (httpMethod) {
            case GET:
                doGet(req, resp);
                break;
            case POST:
                doPost(req, resp);
                break;
            default:
                resp.sendError(400, "Unsupported Operation " + httpMethod);
        }
    }

    /**
     * 初始化阶段调用
     */
    @Override
    public void init(ServletConfig config) {
        log.info("Init HttpServlet");

        //加载配置文件
        doLoadConfig(config.getInitParameter(PROPERTIES_LOCATION));

        //根据配置文件扫描所有相关的类
        scanner.doScan(properties.getProperty(SCAN_PACKAGE));

        //初始化所有相关类的实例，将其放入IOC容易之中
        doInstance();
        if (beanFactory.isEmpty()) {
            return;
        }

        //实现DI
        doAutowired();
        //初始化HandlerMapping
        initHandlerMapping();

    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp, HttpMethod httpMethod)
            throws IOException {

        Handler handler;
        try {
            handler = getHandler(req, httpMethod);
        } catch (NotFoundException nfe) {
            log.error("Endpoint not found");
            resp.sendError(404, "Endpoint not found");
            return;
        } catch (UnsupportedOperationException uoe) {
            log.error("Unsupported Operation");
            resp.sendError(400, "Unsupported Operation");
            return;
        }

        //获取方法参数列表
        Method method = handler.getMethod();
        Class<?>[] parameterTypes = method.getParameterTypes();

        //保存所有需要自动赋值的参数
        Object[] paramValues = new Object[parameterTypes.length];
        List<Argument> arguments = handler.getArguments();

        Map<String, String[]> params = req.getParameterMap();
        for (Entry<String, String[]> param : params.entrySet()) {
            String value = Arrays.toString(param.getValue())
                    .replaceAll("\\[|\\]", EMPTY_STRING.replaceAll(",\\s", ","));

            //如果找到匹配的对象。则开始填充参数
            String targetKey = param.getKey();
            Optional<Argument> argument = getArgument(arguments, targetKey);
            if (!argument.isPresent()) {
                continue;
            }

            Argument targetArgument = argument.get();

            int index = targetArgument.getIndex();
            paramValues[index] = convertToInt(parameterTypes[index], value);
        }

        Optional<Argument> body = getArgument(arguments, MyRequestBody.class.getName());
        if (body.isPresent()) {
            String requestBody = StreamUtils.copyToString(req.getInputStream(), Charset.defaultCharset());
            int bodyIndex = body.map(Argument::getIndex).orElse(-1);

            paramValues[bodyIndex] = new ObjectMapper().readValue(requestBody, body.get().getType());
        }

        //设置方法中的request和response对象
        int regIndex = getArgument(arguments, HttpServletRequest.class.getName()).map(Argument::getIndex).orElse(-1);
        paramValues[regIndex] = req;
        int respIndex = getArgument(arguments, HttpServletResponse.class.getName()).map(Argument::getIndex).orElse(-1);
        paramValues[respIndex] = resp;

        try {
            Class<?> returnType = method.getReturnType();

            if (returnType == Void.class) {
                method.invoke(handler.getController(), paramValues);
                return;
            }

            Object result = method.invoke(handler.getController(), paramValues);
            resp.getWriter().write(new ObjectMapper().writeValueAsString(result));
        } catch (IllegalAccessException e) {
            log.error("Illegal access for method {}", handler.getMethod(), e);
        } catch (InvocationTargetException e) {
            log.error("Invoke failed for method {}, caused by", handler.getMethod(), e);
        }
    }

    private Optional<Argument> getArgument(List<Argument> arguments, String targetKey) {
        return arguments.stream()
                .filter(argument -> targetKey.equals(argument.getKey()))
                .findAny();
    }

    private Handler getHandler(HttpServletRequest req, HttpMethod httpMethod) {

        if (handlerMapping.isEmpty()) {
            throw new NotFoundException();
        }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, EMPTY_STRING).replaceAll("/+", SLASH);

        List<Handler> matched = new ArrayList<>();

        for (Handler handler : handlerMapping) {

            Matcher matcher = handler.getPattern().matcher(url);
            if (!matcher.matches()) {
                continue;
            }

            matched.add(handler);
        }

        if (matched.isEmpty()) {
            throw new NotFoundException();
        }

        return matched.stream()
                .filter(handler -> handler.getHttpMethod() == httpMethod)
                .findFirst()
                .orElseThrow(UnsupportedOperationException::new);
    }

    private void initHandlerMapping() {

        if (beanFactory.isEmpty()) {
            return;
        }

        List<BeanContainer> beans = beanFactory.getBeans();
        for (BeanContainer bean : beans) {

            Object instance = bean.getInstance();
            Class clazz = instance.getClass();

            if (!clazz.isAnnotationPresent(MyController.class)) {
                return;
            }
            String baseUrl = EMPTY_STRING;
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping annotation = (MyRequestMapping) clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = annotation.value();

            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }

                MyRequestMapping myRequestMapping = method.getAnnotation(MyRequestMapping.class);
                String regex = (SLASH + baseUrl + myRequestMapping.value()).replaceAll("/+", SLASH);
                Pattern pattern = Pattern.compile(regex);

                handlerMapping.add(new Handler(instance, method, pattern, myRequestMapping.httpMethod()));

                log.info("Mapping : {}  {}", regex, method);

            }
        }
    }

    private void doAutowired() {
        List<BeanContainer> beans = beanFactory.getBeans();
        for (BeanContainer bean : beans) {
            //获取到所有的字段属性
            Object instance = bean.getInstance();
            Field[] fields = instance.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }

                MyAutowired annotation = field.getAnnotation(MyAutowired.class);
                String targetBean = annotation.value().trim();
                if (EMPTY_STRING.equals(targetBean)) {
                    targetBean = field.getType().getName();
                }

                //要想访问私有的，或受保护的，要强制授权访问
                try {
                    field.setAccessible(true);
                    field.set(instance, beanFactory.getBean(targetBean));
                } catch (IllegalAccessException e) {
                    log.info("Error while setting field value, ", e);
                }
            }
        }

    }

    private void doInstance() {

        List<String> classNames = scanner.getAllClasses();
        if (classNames.isEmpty()) {
            return;
        }

        //如果不为空，用反射初始化实例
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);

                //bean 实例化阶段,初始化IOC容器
                //IOC容器规则
                //1. key: 默认用类名首字母小写

                if (clazz.isAnnotationPresent(MyController.class)) {

                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    beanFactory.register(beanName, clazz.newInstance());

                } else if (clazz.isAnnotationPresent(MyService.class)) {

                    //2. 如果有指定beanName，优先选择
                    MyService annotation = clazz.getAnnotation(MyService.class);
                    String beanName = annotation.value();
                    if (EMPTY_STRING.equals(beanName.trim())) {
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }

                    Object instance = clazz.newInstance();
                    beanFactory.register(beanName, instance);

                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        //3. 如果是接口，可以用接口的类型作为key
                        beanFactory.register(i.getName(), instance);
                    }
                }

            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            log.error("Error occurred while initial instance. ", e);
        }
    }

    private void doLoadConfig(String location) {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            properties.load(is);
        } catch (IOException e) {
            log.error("Error occurred while loading properties, ", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                log.error("Error occurred while closing inputStream, ", e);
            }
        }
    }
}
