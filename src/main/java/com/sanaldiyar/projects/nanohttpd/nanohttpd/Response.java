/*
 Nano HTTPD HTTP Server
 Copryright © 2013 Kazım SARIKAYA

 This program is licensed under the terms of Sanal Diyar Software License. Please
 read the license file or visit http://license.sanaldiyar.com
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author kazim
 */
public class Response implements Closeable {

    class ResponseOutputStream extends OutputStream {

        int position = 8192;
        MappedByteBuffer mbb;

        @Override
        public void write(int b) throws IOException {
            if (position == 8192) {
                mbb = null;
                mbb = channel.map(FileChannel.MapMode.READ_WRITE, contentLength, 8192);
                position = 0;
            }
            mbb.put((byte) (b & 0xFF));
            contentLength++;
            position++;
        }

        @Override
        public void close() throws IOException {
            mbb = null;
        }

    }

    private final OutputStream responseStream;
    private StatusCode statusCode = StatusCode.SC404;
    private final HashMap<String, String> headers = new HashMap<>();
    private final List<Cookie> cookies = new ArrayList<>();
    private final File tempfile;
    private final FileChannel channel;
    private int contentLength = 0;
    private URI requestURL;

    Response(File tempfile) throws Exception {
        this.tempfile = tempfile;
        RandomAccessFile raf = new RandomAccessFile(tempfile, "rw");
        channel = raf.getChannel();
        responseStream = new ResponseOutputStream();
    }

    /**
     * Returns response headers
     *
     * @return response headers
     */
    public HashMap<String, String> getHeaders() {
        return headers;
    }

    /**
     * Return HTTP Cookies. You can set response cookies adding to this list.
     *
     * @return cookies
     */
    public List<Cookie> getCookies() {
        return cookies;
    }

    /**
     * Sets HTTP/1.1 Response codes
     *
     * @param statusCode response code
     */
    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Returns HTTP/1.1 response code
     *
     * @return response code
     */
    public StatusCode getStatusCode() {
        return statusCode;
    }

    /**
     * Returns content length
     *
     * @return content length
     */
    int getContentLength() {
        return contentLength;
    }

    /**
     * Send data to the client.
     *
     * @param socketChannel the remote (client) end
     * @throws IOException error at sending data
     */
    void sendToSocketChannel(SocketChannel socketChannel) throws IOException {
        MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0, contentLength);
        while (map.position() < map.limit()) {
            int len = map.remaining() > 8192 ? 8192 : map.remaining();
            byte[] data = new byte[len];
            map.get(data);
            socketChannel.write(ByteBuffer.wrap(data));
        }
        map = null;
    }

    /**
     * Returns an output stream for response data
     *
     * @return outputstream
     */
    public OutputStream getResponseStream() {
        return responseStream;
    }

    /**
     * guesses and sets the content type form extension of requested path.
     *
     * @param extension requested file extension
     */
    public void guessAndAddContentType(String extension) {
        String ct;
        switch (extension) {
            case "css":
                ct = ContentType.TEXTCSS.toString();
                break;
            case "csv":
                ct = ContentType.TEXTCSV.toString();
                break;
            case "html":
            case "htm":
                ct = ContentType.TEXTHTML.toString();
                break;
            case "js":
                ct = ContentType.TEXTJAVASCRIPT.toString();
                break;
            case "text":
            case "txt":
                ct = ContentType.TEXTPLAIN.toString();
                break;
            case "xml":
                ct = ContentType.TEXTXML.toString();
                break;
            case "json":
                ct = ContentType.APPLICATIONJSON.toString();
                break;
            case "pdf":
                ct = ContentType.APPLICATIONPDF.toString();
                break;
            case "zip":
                ct = ContentType.APPLICATIONZIP.toString();
                break;
            case "gz":
                ct = ContentType.APPLICATIONGZIP.toString();
                break;
            case "bz":
                ct = ContentType.APPLICATIONBZIP.toString();
                break;
            case "m4a":
                ct = ContentType.AUDIOMP4.toString();
                break;
            case "mp3":
                ct = ContentType.AUDIOMPEG.toString();
                break;
            case "jpg":
            case "jpeg":
                ct = ContentType.IMAGEJPEG.toString();
                break;
            case "gif":
                ct = ContentType.IMAGEGIF.toString();
                break;
            case "png":
                ct = ContentType.IMAGEPNG.toString();
                break;
            case "mp4":
                ct = ContentType.VIDEOMP4.toString();
                break;
            case "avi":
            case "mpeg":
                ct = ContentType.VIDEOMPEG.toString();
                break;
            default:
                ct = ContentType.APPLICATIONOCTETSTREAM.toString();
                break;
        }
        headers.put("Content-Type", ct);
    }

    @Override
    public void close() throws IOException {
        responseStream.close();
        channel.close();
        tempfile.delete();
    }

    /**
     * returns the information which url the response belongs to.
     *
     * @return the url
     */
    public URI getRequestURL() {
        return requestURL;
    }

    /**
     * Sets request url
     *
     * @param requestURL the request URL
     */
    void setRequestURL(URI requestURL) {
        this.requestURL = requestURL;
    }

}
