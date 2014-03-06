/*
 Nano HTTPD HTTP Server
 Copryright © 2013 Kazım SARIKAYA

 This program is licensed under the terms of Sanal Diyar Software License. Please
 read the license file or visit http://license.sanaldiyar.com
 */
package com.sanaldiyar.projects.nanohttpd.nanohttpd;

/**
 * MIME Content Types
 *
 * @author kazim
 */
public enum ContentType {
    /* application types */

    APPLICATIONATOMXML("application/atom+xml"),
    APPLICATIONECMASCRIPT("application/ecmascript"),
    APPLICATIONJSON("application/json"),
    APPLICATIONJAVASCRIPT("application/javascript"),
    APPLICATIONOCTETSTREAM("application/octet-stream"),
    APPLICATIONPDF("application/pdf"),
    APPLICATIONPOSTSCRIPT("application/postscript"),
    APPLICATIONRDFXML("application/rdf+xml"),
    APPLICATIONRSSXML("application/rss+xml"),
    APPLICATIONSOAPXML("application/soap+xml"),
    APPLICATIONXHTMLXML("application/xhtml+xml"),
    APPLICATIONXML("application/xml"),
    APPLICATIONZIP("application/zip"),
    APPLICATIONBZIP("application/bzip"),
    APPLICATIONGZIP("application/gzip"),
    APPLICATIONXWWWFORMURLENCODED("application/x-www-form-urlencoded"),
    /* audio types */
    AUDIOMP4("audio/mp4"),
    AUDIOMPEG("audio/mpeg"),
    /* image types */
    IMAGEGIF("image/gif"),
    IMAGEJPEG("image/jpeg"),
    IMAGEPNG("image/png"),
    /* multipart types */
    MULTIPARTFORMDATA("multipart/form-data"),
    MULTIPARTMIXED("multipart/mixed"),
    /* text types */
    TEXTCMD("text/cmd"),
    TEXTCSS("text/css"),
    TEXTCSV("text/csv"),
    TEXTHTML("text/html"),
    TEXTJAVASCRIPT("text/javascript"),
    TEXTPLAIN("text/plain"),
    TEXTVCARD("text/vcard"),
    TEXTXML("text/xml"),
    /* video types */
    VIDEOMP4("video/mp4"),
    VIDEOMPEG("video/mpeg"),
    /* font types */
    FONTSVG("image/svg+xml"),
    FONTTTF("application/x-font-ttf"),
    FONTOTF("application/x-font-opentype"),
    FONTWOFF("application/font-woff"),
    FONTEOT("application/vnd.ms-fontobject"),;

    final private String contentType;

    private ContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String toString() {
        return this.contentType;
    }

    public static ContentType fromString(String value) {
        for (ContentType ct : values()) {
            if (ct.toString().equals(value)) {
                return ct;
            }
        }
        return ContentType.TEXTHTML;
    }

}
