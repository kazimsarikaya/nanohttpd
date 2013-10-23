/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

/**
 *
 * @author kazim
 */
public class Response {

    private final ByteArrayOutputStream responseStream = new ByteArrayOutputStream(8192);
    private StatusCode statusCode = StatusCode.SC404;
    private final HashMap<String, String> headers = new HashMap<>();

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
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
