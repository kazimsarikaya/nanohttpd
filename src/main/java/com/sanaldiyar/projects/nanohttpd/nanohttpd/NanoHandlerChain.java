/*
 Nano HTTPD HTTP Server
 Copryright © 2013 Kazım SARIKAYA

 This program is licensed under the terms of Sanal Diyar Software License. Please
 read the license file or visit http://license.sanaldiyar.com
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

import java.io.PrintWriter;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kazim
 */
public class NanoHandlerChain implements NanoHandler, NanoSession {

    private final Logger logger = LoggerFactory.getLogger(NanoHandlerChain.class);

    private final HashMap<String, NanoHandler> handlers = new HashMap<>();

    private final NanoHandler defaultHandler = new NanoHandler() {

        @Override
        public void handle(Request request, Response response) {
            response.setStatusCode(StatusCode.SC404);
            response.guessAndAddContentType("html");
            PrintWriter pw = new PrintWriter(response.getResponseStream());
            pw.println("<!DOCTYPE HTML><html>");
            pw.println("<head><title>Nano HTTPD</title></head>");
            pw.println("<body>");
            pw.println("<h1>Nano HTTPD</h1><hr/>");
            pw.println("<p>Unknown web site: " + request.getPath().getHost() + "</p>");
            pw.println("</body>");
            pw.println("</html>");
            pw.close();
        }

    };

    private ThreadLocal<NanoSessionManager> nanoSessionManager = new InheritableThreadLocal<>();

    public boolean registerHandler(String virtualHost, NanoHandler handler) {
        if (!handlers.containsKey(virtualHost)) {
            handlers.put(virtualHost, handler);
            logger.debug("a handler for " + virtualHost + " is registered");
            return true;
        }
        logger.error("a virtual handler is already registered for " + virtualHost);
        return false;
    }

    public boolean removeHandler(String virtualHost) {
        if (handlers.containsKey(virtualHost)) {
            handlers.remove(virtualHost);
            logger.debug("the handler for " + virtualHost + " is removed");
            return true;
        }
        logger.error("a virtual handler is not found for " + virtualHost);
        return false;
    }

    @Override
    public void handle(Request request, Response response) {
        String virtualHost = request.getPath().getHost();
        if (handlers.containsKey(virtualHost)) {
            logger.debug("a virtual host handler found");
            NanoHandler handler = handlers.get(virtualHost);
            if (handler instanceof NanoSession) {
                ((NanoSession) handler).setNanoSessionManager(nanoSessionManager.get());
            }
            handler.handle(request, response);
            logger.debug("request handled");
            return;
        }
        logger.debug("redirected to default logger");
        defaultHandler.handle(request, response);
    }

    @Override
    public synchronized void setNanoSessionManager(NanoSessionManager nanoSessionManager) {
        this.nanoSessionManager.set(nanoSessionManager);
    }

}
