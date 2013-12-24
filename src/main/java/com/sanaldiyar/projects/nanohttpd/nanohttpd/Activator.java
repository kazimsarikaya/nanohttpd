/*
 Nano HTTPD HTTP Server
 Copryright © 2013 Kazım SARIKAYA

 This program is licensed under the terms of Sanal Diyar Software License. Please
 read the license file or visit http://license.sanaldiyar.com
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

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
            } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                nanoServer.setNanoSessionHandler(null);
                context.ungetService(event.getServiceReference());
            }
        }
    }

}
