/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kazim
 */
public class Request {

    private final Logger logger = LoggerFactory.getLogger(Request.class);

    class FormDataPart {

        HashMap<String, String> headers = new HashMap<>();
        byte[] data;
    }

    private final byte[] requestData;
    private final HashMap<String, String> headers;
    private final URI path;
    private final String method;
    private final HashMap<String, String> parameters = new HashMap<>();
    private final List<FormDataPart> formdataparts = new ArrayList<>();

    Request(byte[] requestData, HashMap<String, String> headers, URI path, String method) {
        this.requestData = requestData;
        this.headers = headers;
        this.path = path;
        this.method = method;
        init();
    }

    private void init() {
        String lang = "utf-8";
        if (headers.containsKey("Content-Language")) {
            lang = headers.get("Content-Language");
        }
        String query = path.getQuery();
        parseURLEncodings(query, lang);
        if (requestData != null) {
            if (requestData.length != 0) {
                String ct = headers.get("Content-Type");
                if (ct.toLowerCase().equals("application/x-www-form-urlencoded")) {
                    try {
                        query = new String(requestData, lang);
                    } catch (UnsupportedEncodingException ex) {
                        try {
                            query = new String(requestData, "utf-8");
                        } catch (UnsupportedEncodingException ex1) {
                        }
                    }
                    parseURLEncodings(query, lang);
                } else if (ct.startsWith("multipart/form-data;")) {
                    String boundary = ct.substring(ct.indexOf("boundary=")).trim();
                    boundary = "--" + boundary.replace("boundary=", "") + "\r\n";
                    byte[] bound = boundary.getBytes();
                    int start = 0;
                    int swift = bound.length;
                    while (true) {
                        int idx1 = Utils.indexOfArray(requestData, bound, start);
                        if (idx1 == -1) {
                            break;
                        }
                        int idx2 = Utils.indexOfArray(requestData, bound, idx1 + swift);
                        if (idx2 == -1) {
                            boundary = boundary.trim() + "--\r\n";
                            bound = boundary.getBytes();
                            idx2 = Utils.indexOfArray(requestData, bound, idx1 + swift);
                            if (idx2 == -1) {
                                break;
                            }
                        }
                        start = idx2;
                        byte[] data = new byte[idx2 - idx1 - swift];
                        System.arraycopy(requestData, idx1 + swift, data, 0, data.length);
                        String line;
                        int lstart = 0;
                        FormDataPart fdp = new FormDataPart();
                        while (true) {
                            line = Utils.readLine(data, lstart);
                            if (line.trim().isEmpty()) {
                                lstart += line.length();
                                break;
                            }
                            String[] parts = line.split(":", 2);
                            fdp.headers.put(parts[0].trim(), parts[1].trim());
                            lstart += line.length();
                        }
                        byte[] fdpdata = new byte[data.length - lstart - 2];
                        System.arraycopy(data, lstart, fdpdata, 0, fdpdata.length);
                        fdp.data = fdpdata;
                        formdataparts.add(fdp);
                    }
                    logger.debug("asd");
                }

            }
        }
    }

    private void parseURLEncodings(String data, String lang) {
        String[] paramkeyvalue = data.split("&");
        for (String keyvalue : paramkeyvalue) {
            String[] parts = keyvalue.split("=");
            String key = decodeString(parts[0], lang);
            String value = decodeString(parts[1], lang);
            parameters.put(key, value);
        }
    }

    private String decodeString(String value, String lang) {
        String key = "";
        try {
            key = URLDecoder.decode(value, lang);
        } catch (UnsupportedEncodingException ex) {
            try {
                key = URLDecoder.decode(value, "utf-8");
            } catch (UnsupportedEncodingException ex1) {
            }
        }
        return key;
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
