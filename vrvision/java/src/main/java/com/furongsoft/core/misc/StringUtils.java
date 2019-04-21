package com.furongsoft.core.misc;

/**
 * 字符串工具类
 *
 * @author Alex
 */
public class StringUtils {
    /**
     * 字符串是否为空
     *
     * @param object 字符串
     * @return 是否为空
     */
    public static boolean isNullOrEmpty(Object object) {
        if (object == null) {
            return true;
        }

        if (!(object instanceof String)) {
            return false;
        }

        return ((String) object).isEmpty();
    }
}
