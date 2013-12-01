/*
Nano HTTPD HTTP Server
Copryright © 2013 Kazım SARIKAYA

This program is licensed under the terms of Sanal Diyar Software License. Please
read the license file or visit http://license.sanaldiyar.com
*/
package com.sanaldiyar.projects.nanohttpd;

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
public class Activator implements BundleActivator, ServiceListener {

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
            this.context.addServiceListener(this, "(&(objectClass=" + NanoHandler.class.getName() + ")(VirtualHost=*))");
        }

        nanoServer.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        nanoServer.stop();
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        synchronized (handlerChain) {
            String virtualHost = (String) event.getServiceReference().getProperty("VirtualHost");
            if (event.getType() == ServiceEvent.REGISTERED) {
                NanoHandler handler = (NanoHandler) this.context.getService(event.getServiceReference());
                handlerChain.registerHandler(virtualHost, handler);
            } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                handlerChain.removeHandler(virtualHost);
                context.ungetService(event.getServiceReference());
            }
        }
    }

}
