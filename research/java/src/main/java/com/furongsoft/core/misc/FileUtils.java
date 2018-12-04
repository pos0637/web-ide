package com.furongsoft.core.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * 文件工具
 *
 * @author Alex
 */
public class FileUtils {
    /**
     * 读取文件内容并进行Base64编码
     *
     * @param path 文件路径
     * @return 文件内容
     */
    public static String readBase64(Path path) {
        return Base64.getEncoder().encodeToString(readFile(path));
    }

    /**
     * 读取文件内容
     *
     * @param path 文件路径
     * @return 文件内容
     */
    public static byte[] readFile(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            Tracker.error(e);
            return null;
        }
    }
}
