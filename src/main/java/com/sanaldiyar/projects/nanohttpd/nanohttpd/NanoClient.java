/*
 Nano HTTPD HTTP Server
 Copryright © 2013 Kazım SARIKAYA

 This program is licensed under the terms of Sanal Diyar Software License. Please
 read the license file or visit http://license.sanaldiyar.com
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Internal class for each client. The clinet is executed for every connection.
 * If keep-alive requested client continue performing operations, otherwise
 * client closed. However the client also have lifetime timeout whenever
 * keep-alive is set
 *
 * @author kazim
 */
class NanoClient implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(NanoClient.class);
    private final ClientContext clientContext;
    private final NanoHandler handler;

    /**
     * Internal Constructor.
     *
     * @param clientContext client context
     * @param handler nano handler
     */
    NanoClient(ClientContext clientContext, NanoHandler handler) {
        this.clientContext = clientContext;
        this.handler = handler;
    }

    /**
     * The thread method. Parse request, use NanoHandler and sends reponse.
     */
    @Override
    public void run() {
        while (true) {
            try {
                logger.info("new client (" + clientContext.getClientId() + ") at: " + clientContext.getSocketChannel().getRemoteAddress().toString());

                Runnable responseHandler = new Runnable() {

                    @Override
                    public void run() {
                        try {
                            Parsers.parseRequest(clientContext);
                        } catch (Exception ex) {
                            logger.error("error at request parsing for the client (" + clientContext.getClientId() + ")", ex);
                            clientContext.setRequest(null);
                        }
                    }
                };
                Future requestSubmit = NanoServer.createOrGetInstance().getThreadpool().submit(responseHandler);
                try {
                    requestSubmit.get(NanoServer.createOrGetInstance().getKeepAliveTimeout(), TimeUnit.SECONDS);
                } catch (Exception ex) {
                }

                if (clientContext.getRequest() == null) {
                    if (clientContext.getSocketChannel().isConnected()) {
                        try {
                            clientContext.getSocketChannel().close();
                        } catch (Exception ex) {
                        }
                    }
                    return;
                }
                logger.debug("client (" + clientContext.getClientId() + ") request parsed for path " + clientContext.getRequest().getPath());

                String vhost = clientContext.getRequest().getPath().getHost() + "." + clientContext.getRequest().getPath().getPort();
                MDC.put("vhost", vhost);

                if (clientContext.getRequest().getHeaders().containsKey("Connection")) {
                    if (!clientContext.getRequest().getHeaders().get("Connection").equals("keep-alive")) {
                        clientContext.getSocketChannel().shutdownInput();
                    }
                } else {
                    clientContext.getSocketChannel().shutdownInput();
                }

                NanoSessionManager nanoSessionManager = null;

                if (handler instanceof NanoSession) {
                    if (NanoServer.createOrGetInstance().getNanoSessionHandler() != null) {
                        nanoSessionManager = NanoServer.createOrGetInstance().getNanoSessionHandler().parseRequest(clientContext.getRequest());
                    }
                    ((NanoSession) handler).setNanoSessionManager(nanoSessionManager);
                }

                clientContext.setResponse(new Response(File.createTempFile("nanohttpd-", ".temp")));
                clientContext.getResponse().setRequestURL(clientContext.getRequest().getPath());
                Runnable handlerRunner = new Runnable() {

                    @Override
                    public void run() {
                        handler.handle(clientContext.getRequest(), clientContext.getResponse());
                    }
                };
                Future<?> submit = NanoServer.createOrGetInstance().getThreadpool().submit(handlerRunner);
                try {
                    submit.get(NanoServer.createOrGetInstance().getExecutionTimeout(), TimeUnit.SECONDS);
                } catch (TimeoutException ex) {
                    logger.debug("client (" + clientContext.getClientId() + ") timeout");
                    if (clientContext.getSocketChannel().isConnected()) {
                        Parsers.sendError(clientContext, StatusCode.SC503);
                    }
                    return;
                }
                clientContext.getRequest().close();

                if (handler instanceof NanoSession) {
                    if (NanoServer.createOrGetInstance().getNanoSessionHandler() != null) {
                        NanoServer.createOrGetInstance().getNanoSessionHandler().parseResponse(nanoSessionManager, clientContext.getResponse());
                    }
                }

                Parsers.parseResponse(clientContext);

                clientContext.getResponse().close();

                if (clientContext.getRequest().getHeaders().containsKey("Connection")) {
                    if (!clientContext.getRequest().getHeaders().get("Connection").equals("keep-alive")) {
                        clientContext.getSocketChannel().shutdownOutput();
                        clientContext.getSocketChannel().close();
                        logger.debug("client (" + clientContext.getClientId() + ") is ended");
                        return;
                    }
                } else {
                    clientContext.getSocketChannel().shutdownOutput();
                    clientContext.getSocketChannel().close();
                    logger.debug("client (" + clientContext.getClientId() + ") is ended");
                    break;
                }

                logger.debug("client (" + clientContext.getClientId() + ") response sended for path " + clientContext.getRequest().getPath());
            } catch (IOException ex) {
                logger.debug("error at client (" + clientContext.getClientId() + ") remote end closed connection");
                logger.debug("client (" + clientContext.getClientId() + ") is ended");
                return;
            } catch (Exception ex) {
                logger.error("error at client (" + clientContext.getClientId() + ")", ex);
                try {
                    Parsers.sendError(clientContext, StatusCode.SC500);
                } catch (Exception ex1) {
                }
                logger.debug("client (" + clientContext.getClientId() + ") is ended");
                return;
            }
        }
    }

}
