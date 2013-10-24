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
    private final HashMap<String, Object> parameters = new HashMap<>();

    Request(byte[] requestData, HashMap<String, String> headers, URI path, String method) {
        this.requestData = requestData;
        this.headers = headers;
        this.path = path;
        this.method = method;
        init();
    }

    private void init() {
        parameters.clear();
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
                } else if (ct.startsWith("multipart/form-data;") || ct.startsWith("multipart/mixed;")) {
                    List<FormDataPart> formdataparts = parseMultiPartBlocks(ct, requestData);
                    parseMultiParts(formdataparts, lang);
                }

            }
        }
    }

    private void parseMultiParts(List<FormDataPart> multiparts, String lang) {
        for (FormDataPart fdp : multiparts) {
            String cd = fdp.headers.get("Content-Disposition");
            String[] fdinfop = cd.split(";");
            String fname = fdinfop[1].replace("name=", "").replace("\"", "").trim();
            fname = decodeString(fname, lang);
            String value = "";
            if (!fdp.headers.containsKey("Content-Type")) {
                parseAsString(fdp, fname, lang);
            } else {
                String tct = fdp.headers.get("Content-Type").trim();
                String cs = "utf-8";
                if (tct.indexOf(";") != -1) {
                    String[] tmp = tct.split(";", 2);
                    if (tmp[1].trim().startsWith("charset=")) {
                        cs = tmp[1].trim().replace("charset=", "").trim();
                    }
                    tct = tmp[0].trim();
                }
                if (tct.startsWith("text/")) {
                    parseAsString(fdp, fname, cs);
                } else if (tct.startsWith("application/")) {
                    if (tct.endsWith("json") || tct.endsWith("javascript")
                            || tct.endsWith("xml") || tct.endsWith("html")) {
                        parseAsString(fdp, fname, cs);
                    } else {
                        parseAsBinary(fdp, fname, fdinfop);
                    }
                } else if (tct.startsWith("multipart/mixed")) {
                    List<FormDataPart> mixedparts = parseMultiPartBlocks(tct, fdp.data);
                    parseMultiParts(mixedparts, lang);
                } else {
                    parseAsBinary(fdp, fname, fdinfop);
                }
            }
        }
        logger.debug("asd");
    }

    private void parseAsString(FormDataPart fdp, String fname, String lang) {
        String value;
        try {
            value = new String(fdp.data, lang);
        } catch (UnsupportedEncodingException ex) {
            value = new String(fdp.data);
        }
        value = decodeString(value, lang);
        if (parameters.containsKey(fname)) {
            List<String> values = (List<String>) parameters.get(fname);
            values.add(value);
        } else {
            List<String> values = new ArrayList<>();
            values.add(value);
            parameters.put(fname, values);
        }
    }

    private void parseAsBinary(FormDataPart fdp, String fname, String[] fdinfop) {
        String filen = "unknown";
        if (parameters.containsKey(fname)) {
            HashMap<String, byte[]> values = (HashMap<String, byte[]>) parameters.get(fname);
            if (fdinfop.length > 2) {
                if (fdinfop[2].trim().startsWith("filename=")) {
                    filen = fdinfop[2].replace("filename=", "").replace("\"", "").trim();
                } else {
                    filen += values.size();
                }
            } else {
                filen += values.size();
            }
            values.put(filen, fdp.data);
        } else {
            HashMap<String, byte[]> values = new HashMap<>();
            if (fdinfop.length > 2) {
                if (fdinfop[2].trim().startsWith("filename=")) {
                    filen = fdinfop[2].replace("filename=", "").trim();
                } else {
                    filen += values.size();
                }
            } else {
                filen += values.size();
            }
            values.put(filen, fdp.data);
            parameters.put(fname, values);
        }
    }

    private List<FormDataPart> parseMultiPartBlocks(String ct, byte[] partData) {
        List<FormDataPart> result = new ArrayList<>();
        String boundary = ct.substring(ct.indexOf("boundary=")).trim();
        boundary = "--" + boundary.replace("boundary=", "") + "\r\n";
        byte[] bound = boundary.getBytes();
        int start = 0;
        int swift = bound.length;
        while (true) {
            int idx1 = Utils.indexOfArray(partData, bound, start);
            if (idx1 == -1) {
                break;
            }
            int idx2 = Utils.indexOfArray(partData, bound, idx1 + swift);
            if (idx2 == -1) {
                boundary = boundary.trim() + "--\r\n";
                bound = boundary.getBytes();
                idx2 = Utils.indexOfArray(partData, bound, idx1 + swift);
                if (idx2 == -1) {
                    break;
                }
            }
            start = idx2;
            byte[] data = new byte[idx2 - idx1 - swift];
            System.arraycopy(partData, idx1 + swift, data, 0, data.length);
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
            result.add(fdp);
        }
        return result;
    }

    private void parseURLEncodings(String data, String lang) {
        String[] paramkeyvalue = data.split("&");
        for (String keyvalue : paramkeyvalue) {
            String[] parts = keyvalue.split("=");
            String key = decodeString(parts[0], lang);
            String value = decodeString(parts[1], lang);
            if (parameters.containsKey(key)) {
                List<String> values = (List<String>) parameters.get(key);
                values.add(value);
            } else {
                List<String> values = new ArrayList<>();
                values.add(value);
                parameters.put(key, values);
            }
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

    public HashMap<String, Object> getParameters() {
        return parameters;
    }

}
