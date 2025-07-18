// Configuration
const API_BASE_URL = 'http://localhost:8096/api/files'; // Update with your backend URL
const ITEMS_PER_PAGE = 10;
let currentPage = 1;
let allFiles = [];

// DOM Elements
const uploadForm = document.getElementById('uploadForm');
const fileInput = document.getElementById('fileInput');
const filesTableBody = document.getElementById('filesTableBody');
const searchInput = document.getElementById('searchInput');
const refreshBtn = document.getElementById('refreshBtn');
const fileCount = document.getElementById('fileCount');
const uploadSpinner = document.getElementById('uploadSpinner');
const uploadBtn = document.getElementById('uploadBtn');
const pagination = document.getElementById('pagination');
const nombre = document.getElementById('nombre');
const allowedTypesEl = document.getElementById('allowedTypes');

// Modal Elements
const fileInfoModal = new bootstrap.Modal('#fileInfoModal');
const modalDownloadBtn = document.getElementById('modalDownloadBtn');

// Initialize the application
document.addEventListener('DOMContentLoaded', () => {
    loadFiles();
    loadConfig();

    // Event listeners
    uploadForm.addEventListener('submit', uploadFile);
    searchInput.addEventListener('input', handleSearch);
    refreshBtn.addEventListener('click', () => {
        currentPage = 1;
        loadFiles();
    });
});

// Load configuration from backend
async function loadConfig() {
    try {
        const response = await fetch(`${API_BASE_URL}/health`);
        if (response.ok) {
            const config = await response.json();
            // Update UI with config if available
           // bucketNameEl.textContent = ''; // Update with actual bucket name if available
            nombre.textContent = 'Jessica Rodriguez'; // Update with actual region if available
        }
    } catch (error) {
        console.error('Error loading config:', error);
    }
}

// Load files from API
async function loadFiles() {
    try {
        showLoading(true);
        const response = await fetch(`${API_BASE_URL}`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const apiResponse = await response.json();

        if (apiResponse.success) {
            allFiles = apiResponse.data;
            displayFiles(allFiles);
            updatePagination();
            showAlert('success', 'Archivos cargados exitosamente');
        } else {
            throw new Error(apiResponse.message || 'Error al cargar archivos');
        }
    } catch (error) {
        console.error('Error loading files:', error);
        showAlert('danger', error.message || 'Error al cargar archivos');
        filesTableBody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center py-4 text-danger">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    Error al cargar archivos
                </td>
            </tr>
        `;
    } finally {
        showLoading(false);
    }
}

// Upload file to S3
async function uploadFile(e) {
    e.preventDefault();

    if (!fileInput.files.length) {
        showAlert('warning', 'Por favor seleccione un archivo');
        return;
    }

    const file = fileInput.files[0];

    // ✅ Nueva validación de tamaño (10 MB)
    const maxSizeBytes = 10 * 1024 * 1024;
    if (file.size > maxSizeBytes) {
        showAlert('danger', `El archivo es demasiado grande (${formatFileSize(file.size)}). Límite: 10 MB.`);
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
        uploadSpinner.classList.remove('d-none');
        uploadBtn.disabled = true;

        const response = await fetch(`${API_BASE_URL}/upload`, {
            method: 'POST',
            body: formData
        });

        const apiResponse = await response.json();

        if (apiResponse.success) {
            showAlert('success', apiResponse.message || 'Archivo subido exitosamente');
            fileInput.value = '';
            currentPage = 1;
            loadFiles();
        } else {
            throw new Error(apiResponse.message || 'Error al subir archivo');
        }
    } catch (error) {
        console.error('Error uploading file:', error);
        showAlert('danger', error.message || 'Error al subir archivo');

        if (error.message.includes('El archivo es demasiado grande')) {
            fileInput.value = '';
        }
    } finally {
        uploadSpinner.classList.add('d-none');
        uploadBtn.disabled = false;
    }
}

// Delete file from S3
async function deleteFile(fileId, fileName) {
    if (!confirm(`¿Está seguro que desea eliminar "${fileName}"?`)) return;

    try {
        showLoading(true);
        const response = await fetch(`${API_BASE_URL}/${fileId}`, {
            method: 'DELETE'
        });

        const apiResponse = await response.json();

        if (apiResponse.success) {
            showAlert('success', apiResponse.message || 'Archivo eliminado exitosamente');
            loadFiles();
        } else {
            throw new Error(apiResponse.message || 'Error al eliminar archivo');
        }
    } catch (error) {
        console.error('Error deleting file:', error);
        showAlert('danger', error.message || 'Error al eliminar archivo');
    } finally {
        showLoading(false);
    }
}

// Download file from S3
function downloadFile(fileId, fileName) {
    // Direct download trigger
    window.open(`${API_BASE_URL}/download/${fileId}`, '_blank');
}

// Show file info modal
async function showFileInfo(fileId) {
    try {
        const response = await fetch(`${API_BASE_URL}/${fileId}/info`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const apiResponse = await response.json();

        if (apiResponse.success) {
            const file = apiResponse.data;
            document.getElementById('fileInfoModalTitle').textContent = `Información: ${file.fileName}`;

            modalDownloadBtn.href = `${API_BASE_URL}/download/${file.fileName}`;

            const fileInfoHTML = `
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <div class="card h-100">
                            <div class="card-body">
                                <h6 class="card-subtitle mb-2 text-muted">Información Básica</h6>
                                <ul class="list-group list-group-flush">
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        <span>Nombre:</span>
                                        <span class="fw-bold">${file.fileName}</span>
                                    </li>
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        <span>Tamaño:</span>
                                        <span class="fw-bold">${formatFileSize(file.size)}</span>
                                    </li>
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        <span>Tipo:</span>
                                        <span class="fw-bold">${file.contentType || 'Desconocido'}</span>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6 mb-3">
                        <div class="card h-100">
                            <div class="card-body">
                                <h6 class="card-subtitle mb-2 text-muted">Metadatos S3</h6>
                                <ul class="list-group list-group-flush">
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        <span>Clave S3:</span>
                                        <span class="fw-bold text-truncate" style="max-width: 150px;">${file.s3Key}</span>
                                    </li>
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        <span>Subido:</span>
                                        <span class="fw-bold">${formatDateTime(file.uploadDate)}</span>
                                    </li>
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        <span>ID:</span>
                                        <span class="fw-bold">${file.fileName}</span>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="alert alert-info mt-3">
                    <i class="fas fa-info-circle me-2"></i>
                    La URL de descarga expirará después de 1 hora.
                </div>
            `;

            document.getElementById('fileInfoModalBody').innerHTML = fileInfoHTML;
            fileInfoModal.show();
        } else {
            throw new Error(apiResponse.message || 'Error al obtener información');
        }
    } catch (error) {
        console.error('Error getting file info:', error);
        showAlert('danger', error.message || 'Error al obtener información del archivo');
    }
}

// Display files in table with pagination
function displayFiles(files) {
    const startIdx = (currentPage - 1) * ITEMS_PER_PAGE;
    const endIdx = startIdx + ITEMS_PER_PAGE;
    const paginatedFiles = files.slice(startIdx, endIdx);

    if (!paginatedFiles.length) {
        filesTableBody.innerHTML = `
            <tr>
                <td colspan="5" class="no-files-message">
                    <i class="fas fa-folder-open"></i>
                    No se encontraron archivos
                </td>
            </tr>
        `;
        fileCount.textContent = '0 archivos';
        return;
    }

    filesTableBody.innerHTML = paginatedFiles.map(file => `
        <tr class="file-row">
            <td data-label="Nombre">
                <div class="d-flex align-items-center">
                    <div class="file-icon bg-${getFileColor(file.contentType || file.fileName)}">
                        <i class="${getFileIcon(file.contentType || file.fileName)}"></i>
                    </div>
                    <div>
                        <div class="fw-semibold">${file.originalFileName}</div>
                        <small class="text-muted d-block">ID: ${file.fileName}</small>
                    </div>
                </div>
            </td>
            <td data-label="Tamaño">${formatFileSize(file.size)}</td>
            <td data-label="Tipo">
                <span class="badge file-type-badge bg-${getFileColor(file.contentType || file.fileName)}">
                    ${getFileType(file.contentType || file.fileName)}
                </span>
            </td>
            <td data-label="Subido">${formatDateTime(file.uploadDate)}</td>
            <td data-label="Acciones">
                <div class="d-flex justify-content-end action-btns">
                    <button onclick="downloadFile('${file.fileName}', '${file.originalFileName}')" 
                            class="btn btn-sm btn-success action-btn" 
                            title="Descargar">
                        <i class="fas fa-download"></i>
                    </button>
                    <button onclick="showFileInfo('${file.fileName}')" 
                            class="btn btn-sm btn-info action-btn text-white" 
                            title="Información">
                        <i class="fas fa-info-circle"></i>
                    </button>
                    <button onclick="deleteFile('${file.fileName}', '${file.originalFileName}')" 
                            class="btn btn-sm btn-danger action-btn" 
                            title="Eliminar">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');

    fileCount.textContent = `${files.length} ${files.length === 1 ? 'archivo' : 'archivos'}`;
}

// Update pagination controls
function updatePagination() {
    const totalPages = Math.ceil(allFiles.length / ITEMS_PER_PAGE);

    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let paginationHTML = '';

    // Previous button
    paginationHTML += `
        <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="changePage(${currentPage - 1})">
                <i class="fas fa-chevron-left"></i>
            </a>
        </li>
    `;

    // Page numbers
    for (let i = 1; i <= totalPages; i++) {
        paginationHTML += `
            <li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" onclick="changePage(${i})">${i}</a>
            </li>
        `;
    }

    // Next button
    paginationHTML += `
        <li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
            <a class="page-link" href="#" onclick="changePage(${currentPage + 1})">
                <i class="fas fa-chevron-right"></i>
            </a>
        </li>
    `;

    pagination.innerHTML = paginationHTML;
}

// Change page
function changePage(page) {
    currentPage = page;
    displayFiles(allFiles);
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Handle search
function handleSearch() {
    const searchTerm = searchInput.value.toLowerCase();

    if (!searchTerm) {
        displayFiles(allFiles);
        updatePagination();
        return;
    }

    const filteredFiles = allFiles.filter(file =>
        file.originalFileName.toLowerCase().includes(searchTerm) ||
        (file.fileName && file.fileName.toLowerCase().includes(searchTerm))
    );

    currentPage = 1;
    displayFiles(filteredFiles);

    // Update pagination for filtered results
    const totalFilteredPages = Math.ceil(filteredFiles.length / ITEMS_PER_PAGE);
    if (totalFilteredPages <= 1) {
        pagination.innerHTML = '';
    } else {
        updatePagination();
    }
}

// Helper: Show loading state
function showLoading(loading) {
    if (loading) {
        filesTableBody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center py-4">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Cargando...</span>
                    </div>
                    <p class="mt-2">Cargando archivos...</p>
                </td>
            </tr>
        `;
    }
}

// Helper: Show alert
function showAlert(type, message) {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.role = 'alert';
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;

    const alertsContainer = document.getElementById('alertsContainer');
    alertsContainer.prepend(alertDiv);

    setTimeout(() => {
        alertDiv.classList.remove('show');
        setTimeout(() => alertDiv.remove(), 150);
    }, 5000);
}

// Helper: Format file size
function formatFileSize(bytes) {
    if (!bytes) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// Helper: Format date and time
function formatDateTime(dateString) {
    if (!dateString) return 'N/A';
    try {
        const date = new Date(dateString);
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    } catch {
        return dateString;
    }
}

// Helper: Get file icon
function getFileIcon(fileTypeOrName) {
    const type = (fileTypeOrName || '').toLowerCase();
    const extension = (fileTypeOrName || '').split('.').pop().toLowerCase();

    if (type.includes('image')) return 'fas fa-image';
    if (type.includes('pdf')) return 'fas fa-file-pdf';
    if (type.includes('word') || extension === 'doc' || extension === 'docx') return 'fas fa-file-word';
    if (type.includes('excel') || extension === 'xls' || extension === 'xlsx') return 'fas fa-file-excel';
    if (type.includes('powerpoint') || extension === 'ppt' || extension === 'pptx') return 'fas fa-file-powerpoint';
    if (type.includes('zip') || extension === 'rar' || extension === '7z') return 'fas fa-file-archive';
    if (type.includes('audio')) return 'fas fa-file-audio';
    if (type.includes('video')) return 'fas fa-file-video';
    if (type.includes('text') || extension === 'txt') return 'fas fa-file-alt';
    if (extension === 'csv') return 'fas fa-file-csv';
    if (extension === 'html' || extension === 'htm') return 'fas fa-file-code';
    if (extension === 'js' || extension === 'json') return 'fab fa-js-square';

    return 'fas fa-file';
}

// Helper: Get file type
function getFileType(fileTypeOrName) {
    const type = (fileTypeOrName || '').toLowerCase();
    const extension = (fileTypeOrName || '').split('.').pop().toLowerCase();

    if (type.includes('image')) return 'Imagen';
    if (type.includes('pdf')) return 'PDF';
    if (type.includes('word') || extension === 'doc' || extension === 'docx') return 'Word';
    if (type.includes('excel') || extension === 'xls' || extension === 'xlsx') return 'Excel';
    if (type.includes('powerpoint') || extension === 'ppt' || extension === 'pptx') return 'PowerPoint';
    if (type.includes('zip') || extension === 'rar' || extension === '7z') return 'ZIP';
    if (type.includes('audio')) return 'Audio';
    if (type.includes('video')) return 'Video';
    if (type.includes('text') || extension === 'txt') return 'Texto';
    if (extension === 'csv') return 'CSV';
    if (extension === 'html' || extension === 'htm') return 'HTML';
    if (extension === 'js') return 'JS';
    if (extension === 'json') return 'JSON';

    return extension.toUpperCase() || 'Archivo';
}

// Helper: Get file color class
function getFileColor(fileTypeOrName) {
    const type = (fileTypeOrName || '').toLowerCase();
    const extension = (fileTypeOrName || '').split('.').pop().toLowerCase();

    if (type.includes('image')) return 'image';
    if (type.includes('pdf')) return 'pdf';
    if (type.includes('word') || extension === 'doc' || extension === 'docx') return 'doc';
    if (type.includes('excel') || extension === 'xls' || extension === 'xlsx') return 'xls';
    if (type.includes('powerpoint') || extension === 'ppt' || extension === 'pptx') return 'ppt';
    if (type.includes('zip') || extension === 'rar' || extension === '7z') return 'zip';

    return 'default';
}