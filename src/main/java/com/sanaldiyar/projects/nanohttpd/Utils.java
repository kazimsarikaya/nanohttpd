/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd;

/**
 *
 * @author kazim
 */
class Utils {

    public static int indexOfArray(byte[] data, byte[] seek, int start) {
        for (int i = start; i < data.length; i++) {
            boolean flag = true;
            for (int j = 0; j < seek.length; j++) {
                if (data[i + j] != seek[j]) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                return i;
            }
        }
        return -1;
    }

    public static String readLine(byte[] data, int start) {
        byte[] line = new byte[1024];
        int linelen = 0;
        int i = start;
        while (i < data.length) {
            int cr, lf;
            cr = data[i++];
            if (cr == 13) {
                line[linelen++] = (byte) cr;
                lf = data[i++];
                if (lf == 10) {
                    line[linelen++] = (byte) lf;
                    break;
                }
            }
            line[linelen++] = (byte) cr;
        }

        return new String(line,0,linelen);
    }
}
