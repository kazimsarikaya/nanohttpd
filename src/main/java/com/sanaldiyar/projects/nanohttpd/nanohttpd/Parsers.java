/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Shared request response parser class
 *
 * @author kazim
 */
public final class Parsers {

    private final static Logger logger = LoggerFactory.getLogger(Parsers.class);

    /**
     * The request parser method. It parses request and a construct a Request
     * object that will be used at NanoHandler.
     *
     * @param clientContext client context
     * @throws Exception several exceptions if there is error at request
     * @see NanoHandler
     * @see Request
     */
    public static void parseRequest(ClientContext clientContext) throws Exception {
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
        FileChannel channel = null;

        while ((rsize = clientContext.getSocketChannel().read(buffer)) > 0) {
            buffer.flip();
            byte[] data = buffer.array();
            BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data, 0, rsize));
            while (bis.available() > 0) {
                if (isheadersparsed) {
                    if (!isrequestdatabufferstarted) {
                        contentlength = Integer.parseInt(headers.get("Content-Length"));
                        if (contentlength <= NanoServer.createOrGetInstance().getRequestDataBuffer()) {
                            reqdbuf = ByteBuffer.allocate(contentlength);
                        } else {
                            tempfile = File.createTempFile("nanohttpd-", ".temp");
                            RandomAccessFile raf = new RandomAccessFile(tempfile, "rw");
                            channel = raf.getChannel();
                            reqdbuf = channel.map(FileChannel.MapMode.READ_WRITE, 0, contentlength);
                        }
                        reqdbuf.clear();
                        isrequestdatabufferstarted = true;
                    }
                    byte[] tmpbd = new byte[bis.available()];
                    readedrequestdatalen += bis.read(tmpbd);
                    reqdbuf = reqdbuf.put(tmpbd);
                    if (contentlength == readedrequestdatalen) {
                        clientContext.setRequest(new Request(reqdbuf, headers, pathURI, method, tempfile, cookies, channel));
                        return;
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
                            MDC.put("vhost", host.replace(":", "."));
                            pathURI = new URI("http://" + host + path);
                        }
                        logger.debug("client (" + clientContext.getClientId() + ") headers parsed");
                        if (bis.available() == 0) {
                            if (headers.containsKey("Content-Length")) {
                                contentlength = Integer.parseInt(headers.get("Content-Length"));
                                if (contentlength == 0) {
                                    clientContext.setRequest(new Request(null, headers, pathURI, method, null, cookies, null));
                                    return;
                                }
                            } else {
                                clientContext.setRequest(new Request(null, headers, pathURI, method, null, cookies, null));
                                return;
                            }
                        }
                        continue;
                    }
                    if (requestline == null) {
                        requestline = headerline;
                        String[] rlparts = requestline.split(" ");
                        if (!rlparts[2].trim().equals("HTTP/1.1")) {
                            clientContext.setStatusCode(StatusCode.SC505);
                            clientContext.setRequest(null);
                            return;
                        }
                        method = rlparts[0];
                        path = rlparts[1];
                        continue;
                    }
                    String[] headerparts = headerline.split(":", 2);
                    if (headerparts[0].trim().toLowerCase().equals("cookie")) {
                        cookies.addAll(Cookie.parseCookie(headerparts[1].trim()));
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
            clientContext.setStatusCode(StatusCode.SC400);
            clientContext.setRequest(null);
            return;
        }
        clientContext.setRequest(new Request(null, headers, pathURI, method, null, cookies, null));
    }

    /**
     * Sends response to the client. Appends headers. Calculate content length
     * and sets at header.
     *
     * @param clientContext client context
     * @throws Exception if there is error at client socket
     */
    public static void parseResponse(ClientContext clientContext) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        buffer.clear();
        int responselength = clientContext.getResponse().getContentLength();
        byte[] data = null;
        if (responselength <= 0) {
            data = clientContext.getResponse().getStatusCode().toString().getBytes("utf-8");
            responselength = data.length;
        }
        buffer.put(("HTTP/1.1 " + clientContext.getResponse().getStatusCode().toString() + "\r\n").getBytes("utf-8"));
        buffer.put(("Content-Length: " + responselength + "\r\n").getBytes("utf-8"));
        for (Map.Entry<String, String> respheader : clientContext.getResponse().getHeaders().entrySet()) {
            buffer.put((respheader.getKey() + ": " + respheader.getValue() + "\r\n").getBytes("utf-8"));
        }
        for (Cookie cookie : clientContext.getResponse().getCookies()) {
            buffer.put(("Set-Cookie: " + cookie.toString() + "\r\n").getBytes("utf-8"));
        }
        buffer.put(("\r\n").getBytes("utf-8"));
        buffer.flip();
        clientContext.getSocketChannel().write(buffer);

        if (data != null) {
            ByteBuffer senddata = ByteBuffer.allocate(data.length);
            senddata.put(data);
            senddata.flip();
            if (clientContext.getSocketChannel().isConnected()) {
                clientContext.getSocketChannel().write(senddata);
            }
        } else {
            if (clientContext.getSocketChannel().isConnected()) {
                clientContext.getResponse().sendToSocketChannel(clientContext.getSocketChannel());
            }
        }

    }

    /**
     * Sends HTTP errors to clients
     *
     * @param clientContext client contect
     * @throws Exception Exception if there is error at client socket
     */
    public static void sendError(ClientContext clientContext) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        byte[] data = clientContext.getStatusCode().toString().getBytes("utf-8");
        int responselength = data.length;
        buffer.put(("HTTP/1.1 " + clientContext.getStatusCode().toString() + "\r\n").getBytes("utf-8"));
        buffer.put(("Content-Length: " + responselength + "\r\n").getBytes("utf-8"));
        buffer.put(("Content-Type: text/plain; charset=utf-8\r\n").getBytes("utf-8"));
        buffer.put(("\r\n").getBytes("utf-8"));
        buffer.flip();
        clientContext.getSocketChannel().write(buffer);

        ByteBuffer senddata = ByteBuffer.allocate(data.length);
        senddata.put(data);
        senddata.flip();
        if (clientContext.getSocketChannel().isConnected()) {
            try {
                clientContext.getSocketChannel().write(senddata);
            } catch (IOException ex) {
            }
        }
    }
}
