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
public interface NanoHandler {

    public void handle(Request request, Response response);
}
