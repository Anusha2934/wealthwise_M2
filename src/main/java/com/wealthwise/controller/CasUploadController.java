package com.wealthwise.controller;

import com.wealthwise.service.CasPdfParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * M04 — CAS Upload Controller
 *
 * Accepts a CAS PDF from the frontend and triggers the parse pipeline.
 *
 * Base URL: /api/v1/portfolio
 */
@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CasUploadController {

    private final CasPdfParserService parserService;

    /**
     * POST /api/v1/portfolio/upload-cas
     *
     * Multipart form:
     *   file   = the CAS PDF file
     *   userId = the logged-in user's ID
     *
     * Returns JSON with totalFolios, totalTransactions, uploadId
     */
    @PostMapping("/upload-cas")
    public ResponseEntity<Map<String, Object>> uploadCas(
            @RequestParam("file")   MultipartFile file,
            @RequestParam("userId") Long userId) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "File is empty"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Only PDF files are accepted"));
        }

        Map<String, Object> result = parserService.parseCas(file, userId);
        return ResponseEntity.ok(result);
    }
}
