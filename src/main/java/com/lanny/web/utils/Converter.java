package com.lanny.web.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Lanny Yao
 * @date 3/14/2019 11:19 AM
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Converter {

    public static Object convertToInt(Class<?> type, String value) {
        if (Integer.class == type) {
            return Integer.valueOf(value);
        }
        return value;
    }

}
