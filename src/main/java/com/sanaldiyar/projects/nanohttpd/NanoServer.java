package com.sanaldiyar.projects.nanohttpd;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class NanoServer {

    private final static Logger logger = LoggerFactory.getLogger(NanoServer.class);
    private boolean serve = true;
    private NanoHandler handler;
    private String configurationFile = "nanohttpd.conf";
    private final Semaphore stopLock = new Semaphore(1);
    private Thread startThread;
    private ExecutorService threadpool;

    public void setHandler(NanoHandler handler) {
        this.handler = handler;
    }

    public NanoHandler getHandler() {
        return handler;
    }

    public String getConfigurationFile() {
        return configurationFile;
    }

    public void setConfigurationFile(String configurationFile) {
        this.configurationFile = configurationFile;
    }
    
    

    public void stop() {
        if(!serve){
            return;
        }
        try {
            serve = false;
            stopLock.acquire();
            threadpool.awaitTermination(10, TimeUnit.SECONDS);
            threadpool.shutdown();
        } catch (InterruptedException ex) {
            logger.debug("Error at stopping", ex);
        }
    }

    public void start() {
        startThread = new Thread("nanohttpd-starter") {

            @Override
            public void run() {
                PropertyConfigurator.configure(NanoServer.class.getResource("/logging.properties"));
                logger.info("Sanal Diyar Nano HTTPD version 0.1");
                try {
                    Properties config = new Properties();
                    FileInputStream configFile;
                    configFile = new FileInputStream(configurationFile);
                    config.load(configFile);
                    configFile = new FileInputStream(configurationFile);
                    PropertyConfigurator.configure(configFile);
                    logger.debug("New logging configuration loaded");

                    final Thread mainThread = Thread.currentThread();
                    Runtime.getRuntime().addShutdownHook(new Thread("nanohttpd-shutdown") {

                        @Override
                        public void run() {
                            try {
                                serve = false;
                                mainThread.join();
                                logger.info("Nano HTTPD is stopped");
                            } catch (InterruptedException ex) {
                                logger.debug("error att shutdown the server", ex);
                            }
                        }

                    });

                    int port = Integer.parseInt(config.getProperty("server.port"));
                    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.socket().setSoTimeout(500);
                    serverSocketChannel.bind(new InetSocketAddress(port));
                    logger.info("The server started on the port: " + port);

                    int threadpoolsize = Integer.parseInt(config.getProperty("server.threadpoolsize"));
                    threadpool = Executors.newFixedThreadPool(threadpoolsize, new ThreadFactory() {
                        private final AtomicInteger count = new AtomicInteger(0);

                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, "nanohttpd-thread-" + count.getAndIncrement());
                        }
                    });
                    logger.info("The server threadpool created with size: " + threadpoolsize);

                    stopLock.acquire();
                    final AtomicInteger clientIndex = new AtomicInteger(0);
                    while (serve) {
                        final SocketChannel clientSocketChannel;
                        try {
                            Socket clientSocket = serverSocketChannel.socket().accept();
                            clientSocketChannel = clientSocket.getChannel();
                        } catch (SocketTimeoutException ste) {
                            continue;
                        }
                        Runnable clientRunnable;
                        clientRunnable = new Runnable() {

                            @Override
                            public void run() {
                                int clientid = clientIndex.getAndIncrement();
                                try {
                                    logger.info("new client (" + clientid + ") at: " + clientSocketChannel.getRemoteAddress().toString());
                                    ByteBuffer buffer = ByteBuffer.allocate(8192);
                                    int rsize;
                                    String requestline = null;
                                    HashMap<String, String> headers = new HashMap<>();
                                    String method = "";
                                    URI pathURI = null;
                                    String path = "";
                                    ByteBuffer requestdatabuffer = null;
                                    boolean isheadersparsed = false;
                                    boolean isrequestdatabufferstarted = false;
                                    int readedrequestdatalen = 0;
                                    int contentlength = 0;
                                    readreuestfor:
                                    while ((rsize = clientSocketChannel.read(buffer)) > 0) {
                                        buffer.flip();
                                        byte[] data = buffer.array();
                                        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data, 0, rsize));
                                        while (bis.available() > 0) {
                                            if (isheadersparsed) {
                                                if (!isrequestdatabufferstarted) {
                                                    contentlength = Integer.parseInt(headers.get("Content-Length"));
                                                    requestdatabuffer = ByteBuffer.allocate(contentlength);
                                                    requestdatabuffer.clear();
                                                    isrequestdatabufferstarted = true;
                                                }
                                                byte[] tmpbd = new byte[bis.available()];
                                                readedrequestdatalen += bis.read(tmpbd);
                                                requestdatabuffer = requestdatabuffer.put(tmpbd);
                                                if (contentlength == readedrequestdatalen) {
                                                    break readreuestfor;
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
                                                        break readreuestfor;
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
                                                        return;
                                                    }
                                                    method = rlparts[0];
                                                    path = rlparts[1];
                                                    continue;
                                                }
                                                String[] headerparts = headerline.split(":", 2);
                                                headers.put(headerparts[0].trim(), headerparts[1].trim());
                                            }
                                        }
                                        buffer.clear();
                                    }
                                    logger.debug("client (" + clientid + ") request parsed for path ");
                                    clientSocketChannel.shutdownInput();
                                    byte[] data = null;
                                    if (requestdatabuffer != null) {
                                        data = requestdatabuffer.array();
                                    }
                                    Request request = new Request(data, headers, pathURI);
                                    Response response = new Response();
                                    handler.handle(request, response);
                                    buffer.clear();
                                    int responselength = response.getResponseData().length;

                                    if (responselength == 0) {
                                        data = response.getStatusCode().toString().getBytes("utf-8");
                                        responselength = data.length;
                                    } else {
                                        data = response.getResponseData();
                                    }
                                    buffer.put(("HTTP/1.1 " + response.getStatusCode().toString() + "\r\n").getBytes("utf-8"));
                                    buffer.put(("Content-Length: " + responselength + "\r\n").getBytes("utf-8"));
                                    buffer.put(("\r\n").getBytes("utf-8"));
                                    buffer.flip();
                                    clientSocketChannel.write(buffer);

                                    ByteBuffer senddata = ByteBuffer.allocate(data.length);
                                    senddata.put(data);
                                    senddata.flip();
                                    if (clientSocketChannel.isConnected()) {
                                        clientSocketChannel.write(senddata);
                                    }

                                    clientSocketChannel.shutdownOutput();
                                    clientSocketChannel.close();
                                    logger.debug("client (" + clientid + ") response sended for path ");
                                } catch (Exception ex) {
                                    logger.debug("error at client (" + clientid + ") io", ex);
                                }
                            }
                        };
                        threadpool.execute(clientRunnable);
                    }
                    stopLock.release();
                } catch (InterruptedException | IOException | NumberFormatException ex) {
                    logger.debug("Error occured: ", ex);
                }
            }

        };
        startThread.start();
    }
}
