package com.sparkcleancebu.http_helper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FormData {
	public static final String separator = "----WebKitFormBoundary" + UUID.randomUUID().toString();
    private StringBuilder data;
    private List<FileData> files = new ArrayList<>();

    public FormData() {
        this.data = new StringBuilder();
        // Initialize with boundary
        this.data.append("--").append(separator);
    }

    // Append string form-data
    public FormData append(String key, String value) {
        this.data.append("\r\n");
        this.data.append("Content-Disposition: form-data; name=\"").append(key).append("\"\r\n\r\n");
        this.data.append(value).append("\r\n");
        this.data.append("--").append(separator);

        return this;
    }

    // Append file form-data
    public FormData append(String key, String filename, String path) {
        this.files.add(new FileData(key, filename, path));
        return this;
    }

    // Build multipart form-data
    public ByteArrayOutputStream build() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            // Write the string data part (non-file fields)
            outputStream.write(this.data.toString().getBytes(StandardCharsets.UTF_8));

            // Process the file parts (file uploads)
            for (int i = 0; i < this.files.size(); i++) {
                FileData fileData = this.files.get(i);
                StringBuilder tempBuf = new StringBuilder();

                // Append file metadata (e.g., Content-Disposition)
                tempBuf.append("\r\n");
                tempBuf.append("Content-Disposition: form-data; name=\"")
                        .append(fileData.key)
                        .append("\"; filename=\"")
                        .append(fileData.name)
                        .append("\"\r\n");

                // Dynamically set Content-Type based on the file type
                String mimeType = getContentType(Paths.get(fileData.path)); // Detect file MIME type
                if (mimeType == null) {
                    mimeType = "application/octet-stream"; // Default fallback MIME type
                }
                tempBuf.append("Content-Type: ").append(mimeType).append("\r\n\r\n");

                // Read the file content and append to the body
                byte[] fileBytes = Files.readAllBytes(Paths.get(fileData.path));

                // Write the file header and content
                outputStream.write(tempBuf.toString().getBytes(StandardCharsets.UTF_8));
                outputStream.write(fileBytes);

                // Write the boundary separator after each file part
                outputStream.write(("\r\n--" + separator).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw e;
        }

        // Write the final boundary to close the form data
        outputStream.write(("--\r\n").getBytes(StandardCharsets.UTF_8));

        return outputStream;
    }

    private class FileData {
        protected String name;
        protected String key;
        protected String path;

        public FileData(String key, String name, String path) {
            this.key = key;
            this.name = name;
            this.path = path;
        }
    }

    // Helper method to get content type of the file if needed
    private String getContentType(Path path) throws Exception {
        return Files.probeContentType(path); // This will get MIME type based on the file extension
    }
}
