package com.lanny.web.dispatcher;

import static com.lanny.web.GlobalConstants.CLASS_SUFFIX;
import static com.lanny.web.GlobalConstants.DOT;
import static com.lanny.web.GlobalConstants.EMPTY_STRING;
import static com.lanny.web.GlobalConstants.SLASH;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Lanny Yao
 * @date 3/14/2019 1:36 PM
 */
@Slf4j
@Component
public class PackageScanner {

    private Map<String, List<String>> classNamesByPackage;

    public PackageScanner() {
        this.classNamesByPackage = new HashMap<>();
    }

    public List<String> doScan(String packageName) {

        //进行递归扫描
        String root = packageName.replaceAll("\\.", SLASH);
        URL url = this.getClass().getClassLoader().getResource(root);

        if (null == url) {
            throw new IllegalArgumentException("Invalid scan package");
        }

        File classDir = new File(url.getFile());
        if (classDir.listFiles() == null || Objects.requireNonNull(classDir.listFiles()).length == 0) {
            log.warn("No file found in given scan package");
            return new ArrayList<>();
        }

        List<String> classNames = new ArrayList<>();
        for (File file : Objects.requireNonNull(classDir.listFiles())) {
            if (file.isDirectory()) {

                doScan(packageName + DOT + file.getName());
            } else {

                String className = packageName + DOT + file.getName().replace(CLASS_SUFFIX, EMPTY_STRING);
                classNames.add(className);
            }
        }

        return classNamesByPackage.put(packageName, classNames);
    }

    public List<String> getClassesIn(String packageName) {
        return classNamesByPackage.get(packageName);
    }

    public List<String> getAllClasses() {

        return classNamesByPackage.keySet().stream()
                .map(classNamesByPackage::get)
                .flatMap(Collection::stream)
                .collect(toList());
    }
}
