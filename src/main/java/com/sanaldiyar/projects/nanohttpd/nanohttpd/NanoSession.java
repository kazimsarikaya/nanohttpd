/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

/**
 * Nano Session.
 * Provides session support. For sessions, the handler should implement this
 * interface.
 * @author kazim
 */
public interface NanoSession {

    /**
     * Sets session manager to the handler.
     * @param nanoSessionManager the session manager.
     */
    void setNanoSessionManager(NanoSessionManager nanoSessionManager);
}
