/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd;

/**
 *
 * @author kazim
 */
public enum StatusCode {

    SC100(100, "Continue"),
    SC101(101, "Switching Protocols"),
    SC200(200, "OK"),
    SC201(201, "Created"),
    SC202(202, "Accepted"),
    SC203(203, "Non-Authoritative Information"),
    SC204(204, "No Content"),
    SC205(205, "Reset Content"),
    SC206(206, "Partial Content"),
    SC300(300, "Multiple Choices"),
    SC301(301, "Moved Permanently"),
    SC302(302, "Found"),
    SC303(303, "See Other"),
    SC304(304, "Not Modified"),
    SC305(305, "Use Proxy"),
    SC307(307, "Temporary Redirect"),
    SC400(400, "Bad Request"),
    SC401(401, "Unauthorized"),
    SC402(402, "Payment Required"),
    SC403(403, "Forbidden"),
    SC404(404, "Not Found"),
    SC405(405, "Method Not Allowed"),
    SC406(406, "Not Acceptable"),
    SC407(407, "Proxy Authentication Required"),
    SC408(408, "Request Time-out"),
    SC409(409, "Conflict"),
    SC410(410, "Gone"),
    SC411(411, "Length Required"),
    SC412(412, "Precondition Failed"),
    SC413(413, "Request Entity Too Large"),
    SC414(414, "Request-URI Too Large"),
    SC415(415, "Unsupported Media Type"),
    SC416(416, "Requested range not satisfiable"),
    SC417(417, "Expectation Failed"),
    SC500(500, "Internal Server Error"),
    SC501(501, "Not Implemented"),
    SC502(502, "Bad Gateway"),
    SC503(503, "Service Unavailable"),
    SC504(504, "Gateway Time-out"),
    SC505(505, "HTTP Version not supported"),;
    private final int statusCode;
    private final String reasonPhrase;

    StatusCode(int statusCode, String reasonPhrase) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    @Override
    public String toString() {
        return statusCode + " " + reasonPhrase;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
