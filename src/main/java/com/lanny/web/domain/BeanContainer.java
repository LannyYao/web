package com.lanny.web.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Lanny Yao
 * @date 3/14/2019 11:27 AM
 */

@AllArgsConstructor
@Getter
@Setter
public class BeanContainer {

    private String beanName;

    private Object instance;

}
