/*
 Nano HTTPD HTTP Server
 Copryright © 2013 Kazım SARIKAYA

 This program is licensed under the terms of Sanal Diyar Software License. Please
 read the license file or visit http://license.sanaldiyar.com
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
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
class NanoClient implements Runnable, Comparable<NanoClient> {

    private final static Logger logger = LoggerFactory.getLogger(NanoClient.class);
    private final List<ClientContext> clientContexts;
    private final Selector selector;

    /**
     * Internal Constructor.
     *
     * @param clientContext client context
     * @param handler nano handler
     */
    NanoClient() throws Exception {
        this.clientContexts = new ArrayList<>();
        this.selector = Selector.open();
    }

    @Override
    public int compareTo(NanoClient o) {
        return this.clientContexts.size() - o.clientContexts.size();
    }

    public void addClientContext(ClientContext clientContext) {
        try {
            clientContext.getSocketChannel().configureBlocking(false);
            selector.wakeup();
            clientContext.getSocketChannel().register(selector, SelectionKey.OP_READ, clientContext);
            logger.info("new client (" + clientContext.getClientId() + ") at: " + clientContext.getSocketChannel().getRemoteAddress().toString());
            clientContexts.add(clientContext);
        } catch (IOException ex) {
            logger.error("can not connect to client (" + clientContext.getClientId() + ")", ex);
        }

    }

    /**
     * The thread method. Parse request, use NanoHandler and sends reponse.
     */
    @Override
    public void run() {
        while (NanoServer.createOrGetInstance().isServe()) {

            try {
                int nofk = selector.select(500);
                if (nofk == 0 && NanoServer.createOrGetInstance().isServe()) {
                    continue;
                }
                if (!NanoServer.createOrGetInstance().isServe()) {
                    return;
                }
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    final ClientContext clientContext = (ClientContext) key.attachment();

                    if (key.isReadable() && !clientContext.isRequesthandled()) {

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
                            continue;
                        }
                        logger.debug("client (" + clientContext.getClientId() + ") request parsed for path " + clientContext.getRequest().getPath());

                        String vhost = clientContext.getRequest().getPath().getHost() + "." + clientContext.getRequest().getPath().getPort();
                        MDC.put("vhost", vhost);

                        if (clientContext.getRequest().getHeaders().containsKey("Connection")) {
                            if (!clientContext.getRequest().getHeaders().get("Connection").equals("keep-alive")) {
                                clientContext.getSocketChannel().shutdownInput();
                                clientContext.setKeepalive(false);
                            } else {
                                clientContext.setKeepalive(true);
                            }
                        } else {
                            clientContext.getSocketChannel().shutdownInput();
                            clientContext.setKeepalive(false);
                        }

                        if (NanoServer.createOrGetInstance().getHandler() instanceof NanoSession) {
                            if (NanoServer.createOrGetInstance().getNanoSessionHandler() != null) {
                                clientContext.setNanoSessionManager(NanoServer.createOrGetInstance().getNanoSessionHandler().parseRequest(clientContext.getRequest()));
                            }
                            ((NanoSession) NanoServer.createOrGetInstance().getHandler()).setNanoSessionManager(clientContext.getNanoSessionManager());
                        }
                        try {
                            clientContext.setResponse(new Response(File.createTempFile("nanohttpd-", ".temp")));
                        } catch (Exception ex) {
                            logger.error("can not create response for client (" + clientContext.getClientId() + ")", ex);
                            continue;
                        }
                        clientContext.getResponse().setRequestURL(clientContext.getRequest().getPath());
                        Runnable handlerRunner = new Runnable() {

                            @Override
                            public void run() {
                                NanoServer.createOrGetInstance().getHandler().handle(clientContext.getRequest(), clientContext.getResponse());
                            }
                        };
                        Future<?> submit = NanoServer.createOrGetInstance().getThreadpool().submit(handlerRunner);
                        try {
                            submit.get(NanoServer.createOrGetInstance().getExecutionTimeout(), TimeUnit.SECONDS);
                        } catch (TimeoutException | InterruptedException | ExecutionException ex) {
                            logger.debug("client (" + clientContext.getClientId() + ") timeout");
                            if (clientContext.getSocketChannel().isConnected()) {
                                try {
                                    clientContext.setStatusCode(StatusCode.SC503);
                                } catch (Exception ex1) {
                                    logger.error("can not send error code to client (" + clientContext.getClientId() + ")", ex1);
                                }
                            }
                            return;
                        }
                        clientContext.getRequest().close();
                        clientContext.setRequest(null);

                        clientContext.setRequesthandled(true);
                        selector.wakeup();
                        clientContext.getSocketChannel().register(selector, SelectionKey.OP_WRITE, clientContext);
                    } else if (key.isWritable() && clientContext.isRequesthandled()) {
                        if (NanoServer.createOrGetInstance().getHandler() instanceof NanoSession) {
                            if (NanoServer.createOrGetInstance().getNanoSessionHandler() != null) {
                                NanoServer.createOrGetInstance().getNanoSessionHandler().parseResponse(clientContext.getNanoSessionManager(), clientContext.getResponse());
                            }
                        }
                        try {
                            if (clientContext.getStatusCode() == StatusCode.SCNONE) {
                                Parsers.parseResponse(clientContext);
                            } else {
                                Parsers.sendError(clientContext);
                            }
                            logger.debug("client (" + clientContext.getClientId() + ") response sended for path " + clientContext.getResponse().getRequestURL());

                        } catch (Exception ex) {
                            logger.error("error at sending response to the client (" + clientContext.getClientId() + ")", ex);
                        }

                        clientContext.getResponse().close();
                        clientContext.setResponse(null);
                        clientContext.setRequesthandled(false);

                        if (!clientContext.isKeepalive()) {
                            clientContext.getSocketChannel().shutdownOutput();
                            clientContext.getSocketChannel().close();
                            clientContexts.remove(clientContext);
                            logger.debug("client (" + clientContext.getClientId() + ") is ended");
                        } else {
                            selector.wakeup();
                            clientContext.getSocketChannel().register(selector, SelectionKey.OP_READ, clientContext);
                        }

                    } else if (key.isWritable() && !clientContext.isRequesthandled()) {
                        clientContext.getSocketChannel().shutdownOutput();
                        clientContext.getSocketChannel().close();
                        clientContexts.remove(clientContext);
                        logger.debug("client (" + clientContext.getClientId() + ") is ended");
                    }

                }
            } catch (IOException ex) {
                logger.error("selection can not work", ex);
            }
        }
    }

}
