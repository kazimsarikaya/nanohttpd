/*
 Nano HTTPD HTTP Server
 Copryright © 2013 Kazım SARIKAYA

 This program is licensed under the terms of Sanal Diyar Software License. Please
 read the license file or visit http://license.sanaldiyar.com
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.sift.MDCBasedDiscriminator;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.sift.AppenderFactory;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;
import java.io.FileInputStream;
import java.util.Properties;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator class. Activates nano httpd server for an osgi framework
 *
 * @author kazim
 */
public class Activator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    private BundleContext context;
    private final NanoHandlerChain handlerChain = new NanoHandlerChain();
    private NanoServer nanoServer;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;

        configureLogging("nanohttpd.conf");

        nanoServer = new NanoServer();
        nanoServer.setHandler(handlerChain);
        nanoServer.setConfigurationFile("nanohttpd.conf");

        synchronized (handlerChain) {
            this.context.addServiceListener(new NanoHandlerServiceListener(), "(&(objectClass=" + NanoHandler.class.getName() + ")(VirtualHost=*))");
        }

        this.context.addServiceListener(new NanoSessionHandlerServiceListener(), "(objectClass=" + NanoSessionHandler.class.getName() + ")");

        nanoServer.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        nanoServer.stop();
    }

    /**
     * Listens registration of nanohandler's
     */
    private class NanoHandlerServiceListener implements ServiceListener {

        @Override
        public void serviceChanged(ServiceEvent event) {
            synchronized (handlerChain) {
                String virtualHost = (String) event.getServiceReference().getProperty("VirtualHost");
                if (event.getType() == ServiceEvent.REGISTERED) {
                    NanoHandler handler = (NanoHandler) context.getService(event.getServiceReference());
                    handlerChain.registerHandler(virtualHost, handler);
                } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                    handlerChain.removeHandler(virtualHost);
                    context.ungetService(event.getServiceReference());
                }
            }
        }
    }

    /**
     * Listens registration of nanosessionhandler's
     */
    private class NanoSessionHandlerServiceListener implements ServiceListener {

        @Override
        public void serviceChanged(ServiceEvent event) {
            if (event.getType() == ServiceEvent.REGISTERED) {
                NanoSessionHandler handler = (NanoSessionHandler) context.getService(event.getServiceReference());
                nanoServer.setNanoSessionHandler(handler);
                handlerChain.setNanoSessionHandler(handler);
            } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                nanoServer.setNanoSessionHandler(null);
                handlerChain.setNanoSessionHandler(null);
                context.ungetService(event.getServiceReference());
            }
        }
    }

    private void configureLogging(String configurationFile) {
        try {
            Properties config = new Properties();
            FileInputStream configFile;
            configFile = new FileInputStream(configurationFile);
            config.load(configFile);

            String discriminator_defaultvalue = config.getProperty("logging.discriminator.defaultvalue");
            final String logfile_basedir = config.getProperty("logging.logfile.basedir");
            final String logfile_pattern = config.getProperty("logging.logfile.pattern");
            final int history_maxcount = Integer.parseInt(config.getProperty("logging.history.count"));
            final String history_maxfilesize = config.getProperty("logging.history.maxfilesize");
            final String logpattern = config.getProperty("logging.logpattern");

            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            StatusManager statusManager = lc.getStatusManager();

            if (statusManager != null) {
                statusManager.add(new InfoStatus("Configuring logger", lc));
            }

            SiftingAppender sa = new SiftingAppender();
            sa.setName("SIFT");
            sa.setContext(lc);

            MDCBasedDiscriminator discriminator = new MDCBasedDiscriminator();
            discriminator.setKey("vhost");
            discriminator.setDefaultValue(discriminator_defaultvalue);
            discriminator.start();

            sa.setDiscriminator(discriminator);

            sa.setAppenderFactory(new AppenderFactory<ILoggingEvent>() {

                @Override
                public Appender<ILoggingEvent> buildAppender(Context context, String discriminatingValue) throws JoranException {
                    RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
                    appender.setName("ROLLINGFILE-" + discriminatingValue);
                    appender.setContext(context);
                    appender.setFile(logfile_basedir + discriminatingValue + ".log");

                    TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
                    policy.setContext(context);
                    policy.setMaxHistory(history_maxcount);
                    policy.setFileNamePattern(logfile_basedir + discriminatingValue + logfile_pattern + ".log.gz");
                    policy.setParent(appender);
                    policy.start();

                    SizeAndTimeBasedFNATP<ILoggingEvent> innerpolicy = new SizeAndTimeBasedFNATP<>();
                    innerpolicy.setContext(context);
                    innerpolicy.setMaxFileSize(history_maxfilesize);
                    innerpolicy.setTimeBasedRollingPolicy(policy);
                    innerpolicy.start();

                    policy.setTimeBasedFileNamingAndTriggeringPolicy(innerpolicy);
                    policy.start();

                    appender.setRollingPolicy(policy);

                    PatternLayoutEncoder pl = new PatternLayoutEncoder();
                    pl.setContext(context);
                    pl.setPattern(logpattern);
                    pl.start();
                    appender.setEncoder(pl);

                    appender.start();
                    return appender;
                }
            });

            sa.start();
            ch.qos.logback.classic.Logger rootLogger = lc.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            rootLogger.detachAppender("console");
            rootLogger.addAppender(sa);
        } catch (Exception ex) {
            logger.error("Error at configuring logger", ex);
        }
    }

}
