// FileUploadResponse.java
package com.example.dto;

public class FileUploadResponse {
    private boolean success;
    private String message;
    private String fileName;
    private String fileId;
    private long size;
    private String downloadUrl;

    public FileUploadResponse() {}

    public FileUploadResponse(boolean success, String message, String fileName,
                              String fileId, long size, String downloadUrl) {
        this.success = success;
        this.message = message;
        this.fileName = fileName;
        this.fileId = fileId;
        this.size = size;
        this.downloadUrl = downloadUrl;
    }

    // Getters y Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}