/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd;

import java.io.PrintWriter;
import java.util.Scanner;

/**
 *
 * @author kazim
 */
public class App {

    public static void main(String[] args) {

        final NanoServer nanoServer = new NanoServer();
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread("nanohttpd-shutdown") {

            @Override
            public void run() {
                try {
                    nanoServer.stop();
                    mainThread.join();
                } catch (InterruptedException ex) {

                }
            }

        });

        nanoServer.setHandler(new NanoHandler() {

            @Override
            public void handle(Request request, Response response) {
                PrintWriter pw = new PrintWriter(response.getResponseStream());
                response.getHeaders().put("Content-Type", "text/html; charset=utf-8");
                response.setStatusCode(StatusCode.SC200);
                pw.println("<html><head><title>Deneme Sayfası</title></head><body>Deneme 1 2 3</body><html>");
                pw.close();
            }
        });
        nanoServer.start();
        Scanner s = new Scanner(System.in);
        s.nextLine();
        nanoServer.stop();
    }
}
