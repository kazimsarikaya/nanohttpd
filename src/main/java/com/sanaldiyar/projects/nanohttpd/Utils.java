/*
Nano HTTPD HTTP Server
Copryright © 2013 Kazım SARIKAYA

This program is licensed under the terms of Sanal Diyar Software License. Please
read the license file or visit http://license.sanaldiyar.com
*/
package com.sanaldiyar.projects.nanohttpd;

import java.nio.ByteBuffer;

/**
 * AN helper class for several operations
 *
 * @author kazim
 */
class Utils {

    /**
     * Finds start indes of the seek array inside data array. Search starts from
     * the parameter start
     *
     * @param data array that searched at
     * @param seek array that will be searched
     * @param start start index of seek array
     * @return the start of first occurance of index of seek array
     */
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

    /**
     * Reads a line string from the data. Line can both end with LF and CRLF.
     *
     * @param data the array for extracting line
     * @param start the begin index of line
     * @return the line with CR/LF characters.
     */
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

        return new String(line, 0, linelen);
    }

    /**
     * Extracts line from byte buffer. Line extracted from the current position
     * of byte buffer. Line can both end with LF and CRLF.
     *
     * @param data the byte buffer
     * @return the line with CR/LF characters
     */
    public static String readLine(ByteBuffer data) {
        byte[] line = new byte[1024];
        int linelen = 0;
        while (data.position() < data.limit()) {
            int cr, lf;
            cr = data.get();
            if (cr == 13) {
                line[linelen++] = (byte) cr;
                lf = data.get();
                if (lf == 10) {
                    line[linelen++] = (byte) lf;
                    break;
                }
            }
            line[linelen++] = (byte) cr;
        }

        return new String(line, 0, linelen);
    }
}
