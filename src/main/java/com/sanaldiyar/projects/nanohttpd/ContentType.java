/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sanaldiyar.projects.nanohttpd;

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
    VIDEOMPEG("video/mpeg"),;

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
