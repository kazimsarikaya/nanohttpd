/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal class for each client. The clinet is executed for every connection.
 * If keep-alive requested client continue performing operations, otherwise
 * client closed. However the client also have lifetime timeout whenever
 * keep-alive is set
 *
 * @author kazim
 */
class NanoClient implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(NanoClient.class);
    private static final AtomicInteger clientIndex = new AtomicInteger(0);
    private final SocketChannel clientSocketChannel;
    private final NanoHandler handler;
    private final int executionTimeout;
    private final int keepAliveTimeout;
    private final ExecutorService threadpool;
    private final int requestdatabuffer;

    /**
     * Internal Constructor.
     *
     * @param clientSocketChannel client socket
     * @param handler nano handler
     * @param executionTimeout the handler execution timeout
     * @param keepAliveTimeout the client closing timeout
     * @param threadpool the thread pool for creating threads
     * @param tempPath temporary folder for large request datas
     * @param requestdatabuffer the threshold for request data length to storing
     * it inside temporary file
     */
    NanoClient(SocketChannel clientSocketChannel, NanoHandler handler, int executionTimeout, int keepAliveTimeout, ExecutorService threadpool, int requestdatabuffer) {
        this.clientSocketChannel = clientSocketChannel;
        this.handler = handler;
        this.executionTimeout = executionTimeout;
        this.keepAliveTimeout = keepAliveTimeout;
        this.threadpool = threadpool;
        this.requestdatabuffer = requestdatabuffer;
    }

    /**
     * The request parser method. It parses request and a construct a Request
     * object that will be used at NanoHandler.
     *
     * @param clientid the id of client for logging
     * @return the request data that sended by client
     * @throws Exception several exceptions if there is error at request
     * @see NanoHandler
     * @see Request
     */
    private Request parseRequest(int clientid) throws Exception {
        String path = "";
        boolean isheadersparsed = false;
        boolean isrequestdatabufferstarted = false;
        int readedrequestdatalen = 0;
        int contentlength = 0;
        int rsize;
        String requestline = null;
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        ByteBuffer reqdbuf = null;
        HashMap<String, String> headers = new HashMap<>();
        List<Cookie> cookies = new ArrayList<>();
        String method = "";
        URI pathURI = null;
        File tempfile = null;
        while ((rsize = clientSocketChannel.read(buffer)) > 0) {
            buffer.flip();
            byte[] data = buffer.array();
            BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data, 0, rsize));
            while (bis.available() > 0) {
                if (isheadersparsed) {
                    if (!isrequestdatabufferstarted) {
                        contentlength = Integer.parseInt(headers.get("Content-Length"));
                        if (contentlength <= this.requestdatabuffer) {
                            reqdbuf = ByteBuffer.allocate(contentlength);
                        } else {
                            tempfile = File.createTempFile("nanohttpd-", ".temp");
                            RandomAccessFile raf = new RandomAccessFile(tempfile, "rw");
                            reqdbuf = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, contentlength);
                        }
                        reqdbuf.clear();
                        isrequestdatabufferstarted = true;
                    }
                    byte[] tmpbd = new byte[bis.available()];
                    readedrequestdatalen += bis.read(tmpbd);
                    reqdbuf = reqdbuf.put(tmpbd);
                    if (contentlength == readedrequestdatalen) {
                        return new Request(reqdbuf, headers, pathURI, method, tempfile, cookies);
                    }
                } else {
                    byte line[] = new byte[1024];
                    int linelen = 0;
                    while (bis.available() > 0) {
                        int cr, lf;
                        cr = bis.read();
                        if (cr == 13) {
                            line[linelen++] = (byte) cr;
                            lf = bis.read();
                            if (lf == 10) {
                                line[linelen++] = (byte) lf;
                                break;
                            }
                            throw new Exception("Error at header line ending");
                        }
                        line[linelen++] = (byte) cr;
                    }
                    Scanner scanner = new Scanner(new ByteArrayInputStream(line));
                    String headerline = scanner.nextLine();
                    if (headerline.trim().isEmpty() && !isheadersparsed) {
                        if (requestline == null) {
                            throw new Exception("request error");
                        }
                        isheadersparsed = true;
                        if (headers.containsKey("Host")) {
                            String host = headers.get("Host");
                            pathURI = new URI("http://" + host + path);
                        }
                        logger.debug("client (" + clientid + ") headers parsed");
                        if (bis.available() == 0) {
                            if (headers.containsKey("Content-Length")) {
                                contentlength = Integer.parseInt(headers.get("Content-Length"));
                                if (contentlength == 0) {
                                    return new Request(null, headers, pathURI, method, null, cookies);
                                }
                            } else {
                                return new Request(null, headers, pathURI, method, null, cookies);
                            }
                        }
                        continue;
                    }
                    if (requestline == null) {
                        requestline = headerline;
                        String[] rlparts = requestline.split(" ");
                        if (!rlparts[2].trim().equals("HTTP/1.1")) {
                            buffer.clear();
                            buffer.put((rlparts[2] + " " + StatusCode.SC505.toString() + "\r\n").getBytes());
                            buffer.flip();
                            clientSocketChannel.write(buffer);
                            clientSocketChannel.close();
                            return null;
                        }
                        method = rlparts[0];
                        path = rlparts[1];
                        continue;
                    }
                    String[] headerparts = headerline.split(":", 2);
                    if (headerparts[0].trim().toLowerCase().equals("cookie")) {
                        cookies.add(Cookie.parseCookie(headerparts[1].trim()));
                    } else {
                        headers.put(headerparts[0].trim(), headerparts[1].trim());
                    }
                }
            }
            buffer.clear();
        }
        byte[] data = null;
        if (reqdbuf != null) {
            data = reqdbuf.array();
        }
        if (rsize == -1 && data == null && headers.isEmpty() && pathURI == null && method.trim().isEmpty()) {
            sendError(StatusCode.SC400);
            return null;
        }
        return new Request(null, headers, pathURI, method, null, cookies);
    }

    /**
     * Sends response to the client. Appends headers. Calculate content length
     * and sets at header.
     *
     * @param response The response object ot send clients
     * @throws Exception if there is error at client socket
     */
    private void parseResponse(Response response) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.clear();
        int responselength = response.getContentLength();
        byte[] data = null;
        if (responselength <= 0) {
            data = response.getStatusCode().toString().getBytes("utf-8");
            responselength = data.length;
        }
        buffer.put(("HTTP/1.1 " + response.getStatusCode().toString() + "\r\n").getBytes("utf-8"));
        buffer.put(("Content-Length: " + responselength + "\r\n").getBytes("utf-8"));
        for (Map.Entry<String, String> respheader : response.getHeaders().entrySet()) {
            buffer.put((respheader.getKey() + ": " + respheader.getValue() + "\r\n").getBytes("utf-8"));
        }
        for (Cookie cookie : response.getCookies()) {
            buffer.put(("Set-Cookie: " + cookie.toString()).getBytes("utf-8"));
        }
        buffer.put(("\r\n").getBytes("utf-8"));
        buffer.flip();
        clientSocketChannel.write(buffer);

        if (data != null) {
            ByteBuffer senddata = ByteBuffer.allocate(data.length);
            senddata.put(data);
            senddata.flip();
            if (clientSocketChannel.isConnected()) {
                clientSocketChannel.write(senddata);
            }
        } else {
            if (clientSocketChannel.isConnected()) {
                response.sendToSocketChannel(clientSocketChannel);
            }
        }

    }

    /**
     * Sends HTTP errors to clients
     *
     * @param sc Error status code
     * @throws Exception Exception if there is error at client socket
     */
    private void sendError(StatusCode sc) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        byte[] data = sc.toString().getBytes("utf-8");
        int responselength = data.length;
        buffer.put(("HTTP/1.1 " + sc.toString() + "\r\n").getBytes("utf-8"));
        buffer.put(("Content-Length: " + responselength + "\r\n").getBytes("utf-8"));
        buffer.put(("Content-Type: text/plain; charset=utf-8\r\n").getBytes("utf-8"));
        buffer.put(("\r\n").getBytes("utf-8"));
        buffer.flip();
        clientSocketChannel.write(buffer);

        ByteBuffer senddata = ByteBuffer.allocate(data.length);
        senddata.put(data);
        senddata.flip();
        if (clientSocketChannel.isConnected()) {
            try {
                clientSocketChannel.write(senddata);
            } catch (IOException ex) {
            }
        }
    }

    /**
     * The thread method. Parse request, use NanoHandler and sends reponse.
     */
    @Override
    public void run() {
        final int clientid = clientIndex.getAndIncrement();
        while (true) {
            try {
                logger.info("new client (" + clientid + ") at: " + clientSocketChannel.getRemoteAddress().toString());

                Callable<Request> responseHandler = new Callable<Request>() {

                    @Override
                    public Request call() throws Exception {
                        return parseRequest(clientid);
                    }
                };
                Future<Request> requestSubmit = threadpool.submit(responseHandler);
                Request tmpRequest;
                try {
                    tmpRequest = requestSubmit.get(keepAliveTimeout, TimeUnit.SECONDS);
                } catch (TimeoutException ex) {
                    tmpRequest = null;
                }
                final Request request = tmpRequest;
                if (request == null) {
                    if (clientSocketChannel.isConnected()) {
                        try {
                            clientSocketChannel.close();
                        } catch (Exception ex) {
                        }
                    }
                    return;
                }
                logger.debug("client (" + clientid + ") request parsed for path " + request.getPath());

                if (request.getHeaders().containsKey("Connection")) {
                    if (!request.getHeaders().get("Connection").equals("keep-alive")) {
                        clientSocketChannel.shutdownInput();
                    }
                } else {
                    clientSocketChannel.shutdownInput();
                }

                final Response response = new Response(File.createTempFile("nanohttpd-", ".temp"));
                Runnable handlerRunner = new Runnable() {

                    @Override
                    public void run() {
                        handler.handle(request, response);
                    }
                };
                Future<?> submit = threadpool.submit(handlerRunner);
                try {
                    submit.get(executionTimeout, TimeUnit.SECONDS);
                } catch (TimeoutException ex) {
                    logger.debug("client (" + clientid + ") timeout");
                    if (clientSocketChannel.isConnected()) {
                        sendError(StatusCode.SC503);
                    }
                    return;
                }
                request.close();

                parseResponse(response);

                response.close();

                if (request.getHeaders().containsKey("Connection")) {
                    if (!request.getHeaders().get("Connection").equals("keep-alive")) {
                        clientSocketChannel.shutdownOutput();
                        clientSocketChannel.close();
                        logger.debug("client (" + clientid + ") is ended");
                        return;
                    }
                } else {
                    clientSocketChannel.shutdownOutput();
                    clientSocketChannel.close();
                    logger.debug("client (" + clientid + ") is ended");
                    break;
                }

                logger.debug("client (" + clientid + ") response sended for path " + request.getPath());
            } catch (IOException ex) {
                logger.debug("error at client (" + clientid + ") remote end closed connection");
                logger.debug("client (" + clientid + ") is ended");
                return;
            } catch (Exception ex) {
                logger.error("error at client (" + clientid + ")", ex);
                try {
                    sendError(StatusCode.SC500);
                } catch (Exception ex1) {
                }
                logger.debug("client (" + clientid + ") is ended");
                return;
            }
        }
    }
}
