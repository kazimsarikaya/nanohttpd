/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

/**
 *
 * @author kazim
 */
public interface NanoSessionManager {

    public Object get(String key);

    public void set(String key, Object value);

    public void delete(String key);

}
