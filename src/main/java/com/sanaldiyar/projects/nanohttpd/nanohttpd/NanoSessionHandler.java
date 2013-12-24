/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

/**
 * The nano session handler.
 * The session management bundles should have at least one implementation of
 * this interface. And should register as a serviceonly one of the 
 * implementation.
 * @author kazim
 */
public interface NanoSessionHandler {

    /**
     * Parses request and returns the session manager 
     * @param request the request from client
     * @return the session manager. 
     */
    public NanoSessionManager parseRequest(Request request);

    /**
     * Stores session information to the response. Especially cookies.
     * @param nanoSessionManager session manager
     * @param response the response
     */
    public void parseResponse(NanoSessionManager nanoSessionManager, Response response);
}
