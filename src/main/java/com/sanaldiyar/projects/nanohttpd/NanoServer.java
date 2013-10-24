package com.sanaldiyar.projects.nanohttpd;

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
    private int executionTimeout;
    private int keepAliveTimeout;

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
        if (!serve) {
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
                    executionTimeout = Integer.parseInt(config.getProperty("execution.timeout"));
                    keepAliveTimeout = Integer.parseInt(config.getProperty("connection.keepalivetimeout"));

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
                        Runnable clientRunnable=new NanoClient(clientSocketChannel, handler, executionTimeout, keepAliveTimeout, threadpool);
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
}
