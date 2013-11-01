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

    Response() {
    }

    /**
     * Returns response headers
     *
     * @return response headers
     */
    public HashMap<String, String> getHeaders() {
        return headers;
    }

    /**
     * Sets HTTP/1.1 Response codes
     *
     * @param statusCode response code
     */
    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Returns HTTP/1.1 response code
     *
     * @return response code
     */
    public StatusCode getStatusCode() {
        return statusCode;
    }

    /**
     * Returns responsedata as byte array
     *
     * @return response bytes
     */
    byte[] getResponseData() {
        return responseStream.toByteArray();
    }

    /**
     * Returns an output stream for response data
     *
     * @return outputstream
     */
    public ByteArrayOutputStream getResponseStream() {
        return responseStream;
    }

    /**
     * guesses and sets the content type form extension of requested path.
     *
     * @param extension requested file extension
     */
    public void guessAndAddContentType(String extension) {
        String ct;
        switch (extension) {
            case "css":
                ct = ContentType.TEXTCSS.toString();
                break;
            case "csv":
                ct = ContentType.TEXTCSV.toString();
                break;
            case "html":
            case "htm":
                ct = ContentType.TEXTHTML.toString();
                break;
            case "js":
                ct = ContentType.TEXTJAVASCRIPT.toString();
                break;
            case "text":
            case "txt":
                ct = ContentType.TEXTPLAIN.toString();
                break;
            case "xml":
                ct = ContentType.TEXTXML.toString();
                break;
            case "json":
                ct = ContentType.APPLICATIONJSON.toString();
                break;
            case "pdf":
                ct = ContentType.APPLICATIONPDF.toString();
                break;
            case "zip":
                ct = ContentType.APPLICATIONZIP.toString();
                break;
            case "gz":
                ct = ContentType.APPLICATIONGZIP.toString();
                break;
            case "bz":
                ct = ContentType.APPLICATIONBZIP.toString();
                break;
            case "m4a":
                ct = ContentType.AUDIOMP4.toString();
                break;
            case "mp3":
                ct = ContentType.AUDIOMPEG.toString();
                break;
            case "jpg":
            case "jpeg":
                ct = ContentType.IMAGEJPEG.toString();
                break;
            case "gif":
                ct = ContentType.IMAGEGIF.toString();
                break;
            case "png":
                ct = ContentType.IMAGEPNG.toString();
                break;
            case "mp4":
                ct = ContentType.VIDEOMP4.toString();
                break;
            case "avi":
            case "mpeg":
                ct = ContentType.VIDEOMPEG.toString();
                break;
            default:
                ct = ContentType.APPLICATIONOCTETSTREAM.toString();
                break;
        }
        headers.put("Content-Type", ct);
    }

}
