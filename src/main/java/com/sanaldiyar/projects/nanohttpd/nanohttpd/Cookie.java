/*
 Nano HTTPD HTTP Server
 Copryright © 2013 Kazım SARIKAYA

 This program is licensed under the terms of Sanal Diyar Software License. Please
 read the license file or visit http://license.sanaldiyar.com
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HTTP Cookie class.
 *
 * @author kazim
 */
public class Cookie {

    private final String name, value;
    private final long maxAge;
    private final String domain, path;
    private final boolean secure, httpOnly;

    public Cookie(String name, String value, long duration, TimeUnit timeunit, String domain, String path, boolean secure, boolean httpOnly) {

        this.name = name;
        this.value = value;
        this.maxAge = timeunit.toSeconds(duration);
        this.domain = domain;
        this.path = path;
        this.secure = secure;
        this.httpOnly = httpOnly;
    }

    public Cookie(String name, String value, long duration, TimeUnit timeunit, String domain, String path) {
        this(name, value, duration, timeunit, domain, path, true, true);
    }

    public Cookie(String name, String value, long duration, TimeUnit timeunit, String domain) {
        this(name, value, duration, timeunit, domain, "/", true, true);

    }

    public Cookie(String name, String value, long duration, String domain) {
        this(name, value, duration, TimeUnit.SECONDS, domain, "/", true, true);
    }

    public Cookie(String name, String value, String domain) {
        this(name, value, 365, TimeUnit.DAYS, domain, "/", true, true);
    }

    public Cookie(String name, String value) {
        this(name, value, 365, TimeUnit.DAYS, null, "/", true, true);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getDomain() {
        return domain;
    }

    public String getPath() {
        return path;
    }

    public boolean isSecure() {
        return secure;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public long getMaxAge() {
        return maxAge;
    }

    @Override
    public String toString() {
        String res = name + "=" + value + "; Max-Age=" + maxAge;
        if (path != null) {
            res += "; Path=" + path;
        }
        if (domain != null) {
            res += "; Domain=" + domain;
        }
        if (httpOnly) {
            res += "; HttpOnly";
        }
        if (secure) {
            res += "; Secure";
        }
        return res;
    }

    static List<Cookie> parseCookie(String cookiestr) {
        String[] cookieparts = cookiestr.split(";");
        List<Cookie> cookies = new ArrayList<>();
        for (String cookiepart : cookieparts) {
            String cookieitempart[] = cookiepart.split("=");
            cookies.add(new Cookie(cookieitempart[0].trim(), cookieitempart[1].trim()));
        }

        return cookies;
    }
}
