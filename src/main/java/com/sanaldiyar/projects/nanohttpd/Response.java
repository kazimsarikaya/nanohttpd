/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd;

import java.io.ByteArrayOutputStream;

/**
 *
 * @author kazim
 */
public class Response {

    private String contentType;
    private final ByteArrayOutputStream responseStream = new ByteArrayOutputStream(8192);
    private StatusCode statusCode = StatusCode.SC404;

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    byte[] getResponseData() {
        return responseStream.toByteArray();
    }

    public ByteArrayOutputStream getResponseStream() {
        return responseStream;
    }

}
