/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

import java.nio.channels.SocketChannel;
import java.util.Objects;

/**
 *
 * @author kazim
 */
public class ClientContext {

    private final SocketChannel socketChannel;
    private final Integer clientId;
    private Response response;
    private Request request;
    private boolean requesthandled = false;
    private NanoSessionManager nanoSessionManager;
    private StatusCode statusCode = StatusCode.SCNONE;
    private boolean keepalive = false;

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

    public boolean isRequesthandled() {
        return requesthandled;
    }

    public void setRequesthandled(boolean requesthandled) {
        this.requesthandled = requesthandled;
    }

    public NanoSessionManager getNanoSessionManager() {
        return nanoSessionManager;
    }

    public void setNanoSessionManager(NanoSessionManager nanoSessionManager) {
        this.nanoSessionManager = nanoSessionManager;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public boolean isKeepalive() {
        return keepalive;
    }

    public void setKeepalive(boolean keepalive) {
        this.keepalive = keepalive;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ClientContext other = (ClientContext) obj;
        if (!Objects.equals(this.clientId, other.clientId)) {
            return false;
        }
        return true;
    }

}
