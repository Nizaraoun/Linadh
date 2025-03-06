package com.core.learning.service;


import com.core.learning.model.Certificate;
import com.core.learning.repository.CertificationReapository;
import com.core.learning.util.GenerateId;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
public class CertificateGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(CertificateGenerationService.class);

    private final CertificationReapository certificationRepository;

    @Value("${certificate.storage.path:certificates}")
    private String certificateStoragePath;
    
    @Value("${certificate.template.path:templates/template_certifcate.png}")
    private String certificateTemplatePath;
    
    @Value("${certificate.width:842}")  
    private float certificateWidth;
    
    @Value("${certificate.height:595}")
    private float certificateHeight;

    public CertificateGenerationService(CertificationReapository repository) {
        this.certificationRepository = repository;
    }

    public Certificate generateAndSaveCertificate(
        String id, Long userId, Long examId, String examTitle, String username, int score, int passPercentage) {
        try {
            byte[] certificateBytes = generateCertificatePdf(examId, examTitle, username, score, id, passPercentage);

            String certificatePath = saveCertificateToFile(certificateBytes, id, userId, examId);

            Certificate certification = Certificate.builder()
                .id(id)
                .userId(userId)
                .examId(examId)
                .examTitle(examTitle)
                .username(username)
                .score(score)
                .passPercentage(passPercentage)
                .certificatePath(certificatePath)
                .issuedDate(LocalDateTime.now())
                .isValid(score >= passPercentage)
                .build();

            return certificationRepository.save(certification);

        } catch (Exception e) {
            logger.error("Error generating and saving certificate", e);
            throw new RuntimeException("Failed to generate and save certificate", e);
        }
    }

    private byte[] generateCertificatePdf(Long examId, String examTitle, String username, int score, String id, int passPercentage) {
        Document document = new Document(new Rectangle(certificateWidth, certificateHeight));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, byteArrayOutputStream);
            document.open();
            
            PdfContentByte canvas = writer.getDirectContentUnder();
            
            // Add template background image if exists
            try {
                Path templatePath = Paths.get(certificateTemplatePath);
                if (Files.exists(templatePath)) {
                    Image backgroundImage = Image.getInstance(templatePath.toAbsolutePath().toString());
                    backgroundImage.setAbsolutePosition(0, 0);
                    backgroundImage.scaleToFit(certificateWidth, certificateHeight);
                    canvas.addImage(backgroundImage);
                } else {
                    // If no template exists, create a simple border and background
                    canvas.setColorFill(new BaseColor(252, 252, 250));
                    canvas.rectangle(20, 20, certificateWidth - 40, certificateHeight - 40);
                    canvas.fill();
                    
                    canvas.setColorStroke(new BaseColor(44, 62, 80));
                    canvas.setLineWidth(3);
                    canvas.rectangle(25, 25, certificateWidth - 50, certificateHeight - 50);
                    canvas.stroke();
                }
            } catch (Exception e) {
                logger.warn("Could not load certificate template, using default style", e);
            }
            
            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, BaseColor.DARK_GRAY);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, 
                    new Phrase("Certificate of Completion", titleFont),
                    certificateWidth / 2, certificateHeight - 150, 0);
            
            // Intro text
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 16);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, 
                    new Phrase("This is to certify that", infoFont),
                    certificateWidth / 2, certificateHeight - 210, 0);
            
            // Username - more prominent
            Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BaseColor.BLUE);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, 
                    new Phrase(username, nameFont),
                    certificateWidth / 2, certificateHeight - 250, 0);
            
            // Completion text
            Font completionFont = FontFactory.getFont(FontFactory.HELVETICA, 16);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, 
                    new Phrase("has successfully completed the exam", completionFont),
                    certificateWidth / 2, certificateHeight - 280, 0);
            
            // Exam title
            Font examFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, 
                    new Phrase(examTitle, examFont),
                    certificateWidth / 2, certificateHeight - 310, 0);
            
            // Score information
            Font scoreFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new BaseColor(39, 174, 96));
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, 
                    new Phrase(String.format("with a score of %d%%", score), scoreFont),
                    certificateWidth / 2, certificateHeight - 350, 0);
            
            // Date information at the bottom left
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
            String formattedDate = LocalDateTime.now().format(formatter);
            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, 
                    new Phrase("Issued on: " + formattedDate, dateFont),
                    80, 90, 0);
            
            // Certificate ID at the bottom right
            Font idFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, 
                    new Phrase("Certificate ID: " + id, idFont),
                    certificateWidth - 80, 90, 0);
            
            // Signature Section - Move Up
            Font signatureFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, 
                    new Phrase("______________________", signatureFont),
                    certificateWidth / 2, 170, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, 
                    new Phrase("Authorized Signature", dateFont),
                    certificateWidth / 2, 150, 0);

            document.close();
            return byteArrayOutputStream.toByteArray();

        } catch (DocumentException e) {
            logger.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate certificate PDF", e);
        }
    }

    /**
     * Save certificate PDF file to disk
     * Updated to use certificate ID in filename and create absolute paths
     */
    private String saveCertificateToFile(byte[] certificateBytes, String certificateId, Long userId, Long examId) {
        try {
            // Create an absolute path for the certificate storage directory
            Path directoryPath = Paths.get(certificateStoragePath).toAbsolutePath();
            Files.createDirectories(directoryPath);

            // Use certificate ID in the filename for better uniqueness
            String filename = String.format("certificate_%s.pdf", certificateId);
            Path filePath = directoryPath.resolve(filename);

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(certificateBytes);
            }

            logger.info("Certificate saved at: {}", filePath);
            return filePath.toString();

        } catch (IOException e) {
            logger.error("Error saving certificate file", e);
            throw new RuntimeException("Failed to save certificate file", e);
        }
    }

    /**
     * Get certificate PDF bytes
     * Enhanced with on-demand generation if file is missing
     */
    public byte[] getCertificatePdfBytes(Long userId, Long examId) {
        try {
            Certificate certificate = certificationRepository.findByUserIdAndExamId(userId, examId)
                .orElseThrow(() -> new RuntimeException("Certificate not found for user " + userId + " and exam " + examId));
            
            Path certificatePath = Paths.get(certificate.getCertificatePath());
            
            // Check if the certificate file exists
            if (Files.exists(certificatePath)) {
                logger.info("Using existing certificate file: {}", certificatePath);
                return Files.readAllBytes(certificatePath);
            } else {
                // If file doesn't exist, regenerate it
                logger.warn("Certificate file not found at: {}. Regenerating...", certificatePath);
                byte[] regeneratedPdf = generateCertificatePdf(
                    certificate.getExamId(), 
                    certificate.getExamTitle(), 
                    certificate.getUsername(), 
                    certificate.getScore(),
                    certificate.getId(),
                    certificate.getPassPercentage()
                );
                
                // Save regenerated file
                try (FileOutputStream fos = new FileOutputStream(certificatePath.toFile())) {
                    fos.write(regeneratedPdf);
                    logger.info("Regenerated certificate file saved at: {}", certificatePath);
                }
                
                return regeneratedPdf;
            }
        } catch (IOException e) {
            logger.error("Error accessing certificate file", e);
            throw new RuntimeException("Failed to read or regenerate certificate file", e);
        }
    }
    
    /**
     * Generate PDF bytes directly without saving
     * Useful for preview or when storage isn't needed
     */
    public byte[] generateCertificatePdfBytes(Certificate certificate) {
        return generateCertificatePdf(
            certificate.getExamId(),
            certificate.getExamTitle(),
            certificate.getUsername(),
            certificate.getScore(),
            certificate.getId(),
            certificate.getPassPercentage()
        );
    }
}