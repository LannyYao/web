package com.lanny.web.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Lanny Yao
 * @date 3/13/2019 4:41 PM
 */
@Data
@AllArgsConstructor
public class Argument {

    // index of the args list
    private int index;

    private Class<?> type;

    private String key;

}
