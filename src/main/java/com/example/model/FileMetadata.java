// FileMetadata.java
package com.example.model;

import java.time.LocalDateTime;

public class FileMetadata {
    private String fileName;
    private String originalFileName;
    private String contentType;
    private long size;
    private String s3Key;
    private LocalDateTime uploadDate;
    private String downloadUrl;

    public FileMetadata() {}

    public FileMetadata(String fileName, String originalFileName, String contentType,
                        long size, String s3Key, LocalDateTime uploadDate) {
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.size = size;
        this.s3Key = s3Key;
        this.uploadDate = uploadDate;
    }

    // Getters y Setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}