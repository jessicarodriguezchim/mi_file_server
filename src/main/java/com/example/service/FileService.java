package com.example.service;


import com.example.dto.FileUploadResponse;
import com.example.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${file.upload.allowed-types}")
    private String allowedTypes;

    public FileUploadResponse uploadFile(MultipartFile file) {
        try {
            // Validaciones
            if (file.isEmpty()) {
                return new FileUploadResponse(false, "El archivo está vacío", null, null, 0, null);
            }

            if (!isValidFileType(file)) {
                return new FileUploadResponse(false, "Tipo de archivo no permitido", null, null, 0, null);
            }

            // Generar nombre único para el archivo
            String fileId = UUID.randomUUID().toString();
            String originalFileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String s3Key = "files/" + fileId + "." + fileExtension;

            // Crear metadata para S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .metadata(java.util.Map.of(
                            "original-filename", originalFileName,
                            "upload-date", LocalDateTime.now().toString(),
                            "file-id", fileId
                    ))
                    .build();

            // Subir archivo a S3
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // Generar URL de descarga
            String downloadUrl = generateDownloadUrl(s3Key);

            logger.info("Archivo subido exitosamente: {} -> {}", originalFileName, s3Key);

            return new FileUploadResponse(
                    true,
                    "Archivo subido exitosamente",
                    originalFileName,
                    fileId,
                    file.getSize(),
                    downloadUrl
            );

        } catch (IOException e) {
            logger.error("Error al leer el archivo: ", e);
            return new FileUploadResponse(false, "Error al procesar el archivo", null, null, 0, null);
        } catch (S3Exception e) {
            logger.error("Error de S3: ", e);
            return new FileUploadResponse(false, "Error al subir el archivo a S3", null, null, 0, null);
        } catch (Exception e) {
            logger.error("Error inesperado: ", e);
            return new FileUploadResponse(false, "Error interno del servidor", null, null, 0, null);
        }
    }

    public ResponseInputStream<GetObjectResponse> downloadFile(String fileId) {
        try {
            String s3Key = findS3KeyByFileId(fileId);
            if (s3Key == null) {
                throw new RuntimeException("Archivo no encontrado");
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            return s3Client.getObject(getObjectRequest);

        } catch (S3Exception e) {
            logger.error("Error al descargar archivo de S3: ", e);
            throw new RuntimeException("Error al descargar el archivo");
        }
    }

    public List<FileMetadata> listFiles() {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix("files/")
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
            List<FileMetadata> files = new ArrayList<>();

            for (S3Object s3Object : response.contents()) {
                // Obtener metadata del objeto
                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Object.key())
                        .build();

                HeadObjectResponse headResponse = s3Client.headObject(headRequest);

                FileMetadata metadata = new FileMetadata();
                metadata.setS3Key(s3Object.key());
                metadata.setSize(s3Object.size());
                metadata.setContentType(headResponse.contentType());
                metadata.setOriginalFileName(headResponse.metadata().get("original-filename"));
                metadata.setFileName(headResponse.metadata().get("file-id"));
                metadata.setDownloadUrl(generateDownloadUrl(s3Object.key()));

                // Parsear fecha de subida
                String uploadDateStr = headResponse.metadata().get("upload-date");
                if (uploadDateStr != null) {
                    metadata.setUploadDate(LocalDateTime.parse(uploadDateStr));
                }

                files.add(metadata);
            }

            return files;

        } catch (S3Exception e) {
            logger.error("Error al listar archivos: ", e);
            throw new RuntimeException("Error al obtener la lista de archivos");
        }
    }

    public boolean deleteFile(String fileId) {
        try {
            String s3Key = findS3KeyByFileId(fileId);
            if (s3Key == null) {
                return false;
            }

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            logger.info("Archivo eliminado: {}", s3Key);
            return true;

        } catch (S3Exception e) {
            logger.error("Error al eliminar archivo: ", e);
            return false;
        }
    }

    public String generateDownloadUrl(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            // URL pre-firmada válida por 1 hora
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Client.utilities().getUrl(GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build()).toString();

        } catch (Exception e) {
            logger.error("Error al generar URL de descarga: ", e);
            return null;
        }
    }

    private boolean isValidFileType(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) return false;

        String extension = getFileExtension(fileName).toLowerCase();
        return allowedTypes.toLowerCase().contains(extension);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private String findS3KeyByFileId(String fileId) {
        try {
            List<FileMetadata> files = listFiles();
            return files.stream()
                    .filter(file -> fileId.equals(file.getFileName()))
                    .map(FileMetadata::getS3Key)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.error("Error al buscar archivo por ID: ", e);
            return null;
        }
    }
}