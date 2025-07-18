package com.example.controller;
import com.example.dto.ApiResponse;


import com.example.dto.FileUploadResponse;
import com.example.model.FileMetadata;
import com.example.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    /**
     * Subir un archivo
     * POST /api/files/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        logger.info("Iniciando subida de archivo: {}", file.getOriginalFilename());

        try {
            FileUploadResponse response = fileService.uploadFile(file);

            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success("Archivo subido exitosamente", response));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(response.getMessage()));
            }

        } catch (Exception e) {
            logger.error("Error al subir archivo: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno del servidor"));
        }
    }

    /**
     * Descargar un archivo
     * GET /api/files/download/{fileId}
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String fileId) {

        logger.info("Iniciando descarga de archivo: {}", fileId);

        try {
            ResponseInputStream<GetObjectResponse> s3Object = fileService.downloadFile(fileId);
            GetObjectResponse response = s3Object.response();

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + response.metadata().get("original-filename") + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, response.contentType());
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(response.contentLength()));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(s3Object));

        } catch (Exception e) {
            logger.error("Error al descargar archivo: ", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Listar todos los archivos
     * GET /api/files
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FileMetadata>>> listFiles() {

        logger.info("Obteniendo lista de archivos");

        try {
            List<FileMetadata> files = fileService.listFiles();
            return ResponseEntity.ok(ApiResponse.success(
                    "Lista de archivos obtenida exitosamente", files));

        } catch (Exception e) {
            logger.error("Error al listar archivos: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener la lista de archivos"));
        }
    }

    /**
     * Eliminar un archivo
     * DELETE /api/files/{fileId}
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String fileId) {

        logger.info("Eliminando archivo: {}", fileId);

        try {
            boolean deleted = fileService.deleteFile(fileId);

            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("Archivo eliminado exitosamente", null));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<Void>error("Archivo no encontrado"));

            }

        } catch (Exception e) {
            logger.error("Error al eliminar archivo: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al eliminar el archivo"));
        }
    }

    /**
     * Obtener información de un archivo específico
     * GET /api/files/{fileId}/info
     */
    @GetMapping("/{fileId}/info")
    public ResponseEntity<ApiResponse<FileMetadata>> getFileInfo(@PathVariable String fileId) {

        logger.info("Obteniendo información del archivo: {}", fileId);

        try {
            List<FileMetadata> files = fileService.listFiles();
            FileMetadata fileInfo = files.stream()
                    .filter(file -> fileId.equals(file.getFileName()))
                    .findFirst()
                    .orElse(null);

            if (fileInfo != null) {
                return ResponseEntity.ok(ApiResponse.success(
                        "Información del archivo obtenida exitosamente", fileInfo));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<FileMetadata>error("Archivo no encontrado"));
            }

        } catch (Exception e) {
            logger.error("Error al obtener información del archivo: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error al obtener la información del archivo"));
        }
    }

    /**
     * Health check del servicio
     * GET /api/files/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Servicio funcionando correctamente", "OK"));
    }
}

