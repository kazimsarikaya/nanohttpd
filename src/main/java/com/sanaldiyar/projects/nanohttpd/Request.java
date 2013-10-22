/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd;

import java.net.URI;
import java.util.HashMap;

/**
 *
 * @author kazim
 */
public class Request {

    private final byte[] requestData;
    private final HashMap<String, String> headers;
    private final URI path;

    Request(byte[] requestData, HashMap<String, String> headers, URI path) {
        this.requestData = requestData;
        this.headers = headers;
        this.path = path;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public URI getPath() {
        return path;
    }

    public byte[] getRequestData() {
        return requestData;
    }

}
