package com.lanny.web.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Lanny Yao
 * @date 3/13/2019 5:29 PM
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CaseUtils {

    public static String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}
