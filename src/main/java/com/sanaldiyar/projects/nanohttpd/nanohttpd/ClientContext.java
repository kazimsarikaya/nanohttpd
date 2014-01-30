/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

import java.nio.channels.SocketChannel;

/**
 *
 * @author kazim
 */
public class ClientContext {

    private final SocketChannel socketChannel;
    private final Integer clientId;
    private Response response;
    private Request request;

    public ClientContext(SocketChannel socketChannel, Integer clientId) {
        this.socketChannel = socketChannel;
        this.clientId = clientId;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public Integer getClientId() {
        return clientId;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

}
