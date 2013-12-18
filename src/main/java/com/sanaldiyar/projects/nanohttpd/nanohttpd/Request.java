/*
Nano HTTPD HTTP Server
Copryright © 2013 Kazım SARIKAYA

This program is licensed under the terms of Sanal Diyar Software License. Please
read the license file or visit http://license.sanaldiyar.com
*/
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kazim
 */
public class Request implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(Request.class);

    /**
     * Freeing temporary file with deleting it.
     *
     * @throws IOException error at temp file deletion
     */
    @Override
    public void close() throws IOException {
        if (tempFile != null) {
            tempFile.delete();
        }
    }

    /**
     * Internal class for storing mutltipart form data parts. Each part have
     * headers and start,end positions inside buffer. End position is included
     * inside data
     */
    class FormDataPart {

        HashMap<String, String> headers = new HashMap<>();
        int start;
        int end;
    }

    /**
     * The internal input stream for uploaded filesF
     */
    class UploadFileInputStream extends InputStream {

        int start;
        int end;
        int position;

        public UploadFileInputStream(int start, int end) {
            this.start = start;
            this.end = end;
            this.position = start;
        }

        @Override
        public int read() throws IOException {
            if (position > end) {
                return -1;
            }
            return requestData.get(position++);
        }

        @Override
        public synchronized void reset() throws IOException {
            position = start;
        }

        @Override
        public int available() throws IOException {
            return this.end - this.position + 1;
        }

    }

    private final ByteBuffer requestData;
    private final HashMap<String, String> headers;
    private final URI path;
    private final String method;
    private final HashMap<String, Object> parameters = new HashMap<>();
    private final File tempFile;
    private final List<Cookie> cookies;

    /**
     * Internal constructor
     *
     * @param requestData sended content
     * @param headers http request headers
     * @param path the requested path
     * @param method the request method
     * @param tempFile the temp file for storing large request data.
     */
    Request(ByteBuffer requestData, HashMap<String, String> headers, URI path, String method, File tempFile, List<Cookie> cookies) {
        this.requestData = requestData;
        this.headers = headers;
        this.path = path;
        this.method = method;
        this.tempFile = tempFile;
        this.cookies = cookies;
        init();
    }

    /**
     * parsing request data
     */
    private void init() {
        parameters.clear();
        String lang = "utf-8";
        if (headers.containsKey("Content-Language")) {
            lang = headers.get("Content-Language");
        }
        String query = path.getQuery();
        if (query != null) {
            parseURLEncodings(query, lang);
        }
        if (requestData != null) {
            if (requestData.limit() != 0) {
                String ct = headers.get("Content-Type");
                if (ct.toLowerCase().equals(ContentType.APPLICATIONXWWWFORMURLENCODED.toString())) {
                    try {
                        query = new String(requestData.array(), lang);
                    } catch (UnsupportedEncodingException ex) {
                        try {
                            query = new String(requestData.array(), "utf-8");
                        } catch (UnsupportedEncodingException ex1) {
                        }
                    }
                    parseURLEncodings(query, lang);
                } else if (ct.startsWith(ContentType.MULTIPARTFORMDATA.toString()) || ct.startsWith(ContentType.MULTIPARTMIXED.toString())) {
                    List<FormDataPart> formdataparts = parseMultiPartBlocks(ct, 0, requestData.limit());
                    parseMultiParts(formdataparts, lang);
                }

            }
        }
    }

    /**
     * Parses each part data
     *
     * @param multiparts the list of parts
     * @param lang the encoding for strings. Default utf-8
     */
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
                } else if (tct.startsWith(ContentType.MULTIPARTMIXED.toString())) {
                    List<FormDataPart> mixedparts = parseMultiPartBlocks(tct, fdp.start, fdp.end);
                    parseMultiParts(mixedparts, lang);
                } else {
                    parseAsBinary(fdp, fname, fdinfop);
                }
            }
        }
    }

    /**
     * Encodes data part as string and stores at parameters hashmap. A field can
     * have multiple values hence the values are stored inside List.
     *
     * @param fdp part
     * @param fname field name
     * @param lang string encoding
     */
    private void parseAsString(FormDataPart fdp, String fname, String lang) {
        String value;
        byte[] data = new byte[fdp.end - fdp.start + 1];
        try {
            requestData.position(fdp.start);
            requestData.get(data);
            value = new String(data, lang);
        } catch (UnsupportedEncodingException ex) {
            value = new String(data);
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

    /**
     * Creates an input stream for the posted file. Stores input steam inside
     * hashmap of uploaded filename
     *
     * @param fdp part
     * @param fname field name
     * @param fdinfop file name information
     */
    private void parseAsBinary(FormDataPart fdp, String fname, String[] fdinfop) {
        String filen = "unknown";
        if (parameters.containsKey(fname)) {
            HashMap<String, InputStream> values = (HashMap<String, InputStream>) parameters.get(fname);
            if (fdinfop.length > 2) {
                if (fdinfop[2].trim().startsWith("filename=")) {
                    filen = fdinfop[2].replace("filename=", "").replace("\"", "").trim();
                } else {
                    filen += values.size();
                }
            } else {
                filen += values.size();
            }
            values.put(filen, new UploadFileInputStream(fdp.start, fdp.end));
        } else {
            HashMap<String, InputStream> values = new HashMap<>();
            if (fdinfop.length > 2) {
                if (fdinfop[2].trim().startsWith("filename=")) {
                    filen = fdinfop[2].replace("filename=", "").replace("\"", "").trim();
                } else {
                    filen += values.size();
                }
            } else {
                filen += values.size();
            }
            values.put(filen, new UploadFileInputStream(fdp.start, fdp.end));
            parameters.put(fname, values);
        }
    }

    /**
     * parser for multipart form data blocks. Divide blocks using boundary and
     * create FormDataPart with headers and data.
     *
     * @param ct content type
     * @param start start of request data
     * @param end end of request data
     * @return list of parts
     */
    private List<FormDataPart> parseMultiPartBlocks(String ct, int start, int end) {
        List<FormDataPart> result = new ArrayList<>();
        String boundary = ct.substring(ct.indexOf("boundary=")).trim();
        boundary = "--" + boundary.replace("boundary=", "");
        byte[] bound = boundary.getBytes();
        requestData.position(start);
        requestData.limit(end);
        requestData.position(requestData.position() + bound.length);
        while (true) {
            requestData.position(requestData.position() + 2);
            FormDataPart fdp = new FormDataPart();
            while (true) {
                String header = Utils.readLine(requestData);
                if (header.trim().isEmpty()) {
                    break;
                }
                String[] headerparts = header.split(":", 2);
                fdp.headers.put(headerparts[0], headerparts[1]);
            }
            fdp.start = requestData.position();
            int j = 0;
            for (; requestData.position() < requestData.limit();) {
                if (requestData.get() == bound[j]) {
                    j++;
                } else {
                    j = 0;
                }
                if (j == bound.length) {
                    break;
                }
            }
            fdp.end = requestData.position() - bound.length - 3;
            result.add(fdp);
            int rem = requestData.limit() - requestData.position();
            if (rem == 4) {
                break;
            }
        }
        return result;
    }

    /**
     * Query string parser
     *
     * @param data query string
     * @param lang string encoding
     */
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

    /**
     * Normalize special characters
     *
     * @param value
     * @param lang string encoding
     * @return string
     */
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

    /**
     * Returns HTTP headers
     *
     * @return HTTP headers
     */
    public HashMap<String, String> getHeaders() {
        return headers;
    }

    /**
     * Returns requested path
     *
     * @return requested path
     */
    public URI getPath() {
        return path;
    }

    /**
     * Returns request paramaters. KeySet is consist of field names. Each field
     * may have more then a value. Except uploaded files, other fields's values
     * are inside list of string. Upload files are inside HashMap whose keys are
     * uploaded file names.
     *
     * @return request parameters
     */
    public HashMap<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Returns request method
     *
     * @return request method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns cookies.
     * Returns the cookies send by client.
     * @return cookies
     */
    public List<Cookie> getCookies() {
        return cookies;
    }

}
