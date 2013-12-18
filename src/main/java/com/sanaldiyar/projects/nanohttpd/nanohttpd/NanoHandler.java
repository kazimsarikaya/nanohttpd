/*
Nano HTTPD HTTP Server
Copryright © 2013 Kazım SARIKAYA

This program is licensed under the terms of Sanal Diyar Software License. Please
read the license file or visit http://license.sanaldiyar.com
*/
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

/**
 * A Nano Handler for http operations. A program should implement this interface
 * and sets it at NanoServer. For each operation the handler executed. Server
 * uses one instance of Handler instance hence the implementation should be
 * thread safe.
 *
 * @see NanoServer
 * @author kazim
 */
public interface NanoHandler {

    /**
     * The handle method of handler. Server executes this method for each
     * operation.
     *
     * @param request The http request instance
     * @param response The http response instance
     * @see Request
     * @see Response
     */
    public void handle(Request request, Response response);
}
