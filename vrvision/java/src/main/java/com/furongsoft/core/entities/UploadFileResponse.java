package com.furongsoft.core.entities;

import org.springframework.http.HttpStatus;

/**
 * 上传文件响应内容
 *
 * @author Alex
 */
public class UploadFileResponse extends RestResponse {
    public UploadFileResponse(HttpStatus status, String uuid) {
        super(status);
        setData(uuid);
    }
}
