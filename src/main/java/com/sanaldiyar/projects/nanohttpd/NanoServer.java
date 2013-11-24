package com.sanaldiyar.projects.nanohttpd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.apache.log4j.PropertyConfigurator;
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
                    while (serve) {
                        SocketChannel clientSocketChannel;
                        try {
                            Socket clientSocket = serverSocketChannel.socket().accept();
                            clientSocketChannel = clientSocket.getChannel();
                        } catch (SocketTimeoutException ste) {
                            continue;
                        }
                        Runnable clientRunnable = new NanoClient(clientSocketChannel, handler, executionTimeout, keepAliveTimeout, threadpool, requestdatabuffer);
                        threadpool.execute(clientRunnable);
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
