/*
 Nano HTTPD HTTP Server
 Copryright © 2013 Kazım SARIKAYA

 This program is licensed under the terms of Sanal Diyar Software License. Please
 read the license file or visit http://license.sanaldiyar.com
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Nano HTTPD Server.
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
    private int executionTimeout;
    private int keepAliveTimeout;
    private String tempPath;
    private int requestdatabuffer;
    private NanoSessionHandler nanoSessionHandler = null;
    private static final AtomicInteger clientIndex = new AtomicInteger(0);

    private static NanoServer instance = null;

    private NanoServer() {

    }

    public static NanoServer createOrGetInstance() {
        if (instance == null) {
            instance = new NanoServer();
        }

        return instance;
    }

    public int getRequestDataBuffer() {
        return requestdatabuffer;
    }

    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public int getExecutionTimeout() {
        return executionTimeout;
    }

    public ExecutorService getThreadpool() {
        return threadpool;
    }

    public NanoSessionHandler getNanoSessionHandler() {
        return nanoSessionHandler;
    }

    public boolean isServe() {
        return serve;
    }

    /**
     * Session Management Handler setter.
     *
     * @param nanoSessionHandler session handler
     */
    public void setNanoSessionHandler(NanoSessionHandler nanoSessionHandler) {
        this.nanoSessionHandler = nanoSessionHandler;
    }

    /**
     * Sets the NanoHandler instance.
     *
     * @param handler The nano handler
     * @see NanoHandler
     */
    public void setHandler(NanoHandler handler) {
        this.handler = handler;
    }

    /**
     * Returns the NanoHandler instance
     *
     * @return The nano handler
     */
    public NanoHandler getHandler() {
        return handler;
    }

    /**
     * Returns configuration file path.
     *
     * @return configuration file path
     */
    public String getConfigurationFile() {
        return configurationFile;
    }

    /**
     * Sets configuration file path.
     *
     * @param configurationFile configuration file path
     */
    public void setConfigurationFile(String configurationFile) {
        this.configurationFile = configurationFile;
    }

    /**
     * Stops NanoServer.
     */
    public void stop() {
        if (!serve) {
            return;
        }
        try {
            logger.info("Try to shutdown nanohttpd server");
            serve = false;
            stopLock.acquire();
            threadpool.awaitTermination(15, TimeUnit.SECONDS);
            threadpool.shutdown();
            logger.info("nanohttpd server shutdown is completed");
        } catch (InterruptedException ex) {
            logger.debug("Error at stopping", ex);
        }
    }

    /**
     * Starts NanoServer.
     */
    public void start() {
        startThread = new Thread("nanohttpd-starter") {

            @Override
            public void run() {
                logger.info("Sanal Diyar Nano HTTPD version 0.1");
                try {
                    Properties config = new Properties();
                    FileInputStream configFile;
                    configFile = new FileInputStream(configurationFile);
                    config.load(configFile);

                    executionTimeout = Integer.parseInt(config.getProperty("execution.timeout"));
                    keepAliveTimeout = Integer.parseInt(config.getProperty("connection.keepalivetimeout"));

                    tempPath = config.getProperty("server.temppath");
                    File tempPathFile = new File(tempPath);
                    if (!tempPathFile.exists()) {
                        tempPathFile.mkdir();
                    }
                    System.setProperty("java.io.tmpdir", tempPath);
                    requestdatabuffer = Integer.parseInt(config.getProperty("server.requestdatabuffer"));

                    int port = Integer.parseInt(config.getProperty("server.port"));
                    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                    //serverSocketChannel.socket().setSoTimeout(500);
                    serverSocketChannel.bind(new InetSocketAddress(port));
                    Selector serverSelector = Selector.open();
                    serverSocketChannel.configureBlocking(false);
                    serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
                    logger.info("The server started on the port: " + port);

                    int threadpoolsize = Integer.parseInt(config.getProperty("server.threadpool.size"));
                    int threadpoolacceptors = Integer.parseInt(config.getProperty("server.threadpool.acceptors"));
                    threadpool = Executors.newFixedThreadPool(threadpoolsize, new ThreadFactory() {
                        private final AtomicInteger count = new AtomicInteger(0);

                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, "nanohttpd-thread-" + count.getAndIncrement());
                        }
                    });
                    logger.info("The server threadpool created with size: " + threadpoolsize);

                    PriorityQueue<NanoClient> nanoClients = new PriorityQueue<>();
                    for (int i = 0; i < threadpoolacceptors; i++) {
                        NanoClient nanoClient;
                        try {
                            nanoClient = new NanoClient();
                            threadpool.execute(nanoClient);
                            nanoClients.offer(nanoClient);
                        } catch (Exception ex) {
                            logger.error("can not create nano client", ex);
                        }
                    }

                    stopLock.acquire();
                    while (serve) {

                        int numofc = serverSelector.select(15 * 1000);
                        if (numofc == 0 && serve) {
                            continue;
                        }
                        if (!serve) {
                            stopLock.release();
                            return;
                        }
                        Iterator<SelectionKey> keys = serverSelector.selectedKeys().iterator();
                        while (keys.hasNext()) {
                            SelectionKey key = keys.next();

                            if (key.isAcceptable()) {
                                ServerSocketChannel tmp = (ServerSocketChannel) key.channel();
                                SocketChannel clientSocketChannel = tmp.accept();
                                ClientContext clientContext = new ClientContext(clientSocketChannel, clientIndex.getAndIncrement());
                                NanoClient nanoClient = nanoClients.poll();
                                nanoClient.addClientContext(clientContext);
                                nanoClients.offer(nanoClient);
                            }

                            keys.remove();
                        }

                    }
                    stopLock.release();
                } catch (InterruptedException | IOException | NumberFormatException ex) {
                    logger.error("Error occured: ", ex);
                }
            }

        };
        startThread.start();
    }

    public void join() {
        try {
            startThread.join();
        } catch (InterruptedException ex) {
            logger.error("Can not join server thread", ex);
        }
    }
}
