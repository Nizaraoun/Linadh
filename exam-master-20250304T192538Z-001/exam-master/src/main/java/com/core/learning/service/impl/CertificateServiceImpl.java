package com.core.learning.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.core.learning.model.Certificate;
import com.core.learning.repository.CertificationReapository;
import com.core.learning.service.CertificateGenerationService;
import com.core.learning.service.CertificationService;
import com.core.learning.util.GenerateId;

@Service
public class CertificateServiceImpl implements CertificationService {
    private final CertificationReapository certificateRepository;
    private final CertificateGenerationService certificateGenerationService;

    @Autowired
    public CertificateServiceImpl(CertificationReapository repository, CertificateGenerationService certificateGenerationService) {
        this.certificateRepository = repository;
        this.certificateGenerationService = certificateGenerationService;
    }

    @Override
    public Certificate createCertificate(Long userId, Long examId, String username, 
                                         String examTitle, int score, int passPercentage) {
        // Create certificate entity
        Certificate certificate = Certificate.builder()
        .id(GenerateId.generateUniqueId())
            .userId(userId)
            .examId(examId)
            .username(username)
            .examTitle(examTitle)
            .score(score)
            .passPercentage(passPercentage)
            .build();

        // Save certificate to database
        certificate = certificateRepository.save(certificate);

        // Generate PDF certificate
        Certificate certificateBytes = certificateGenerationService.generateAndSaveCertificate(
            certificate.getId(), 
             userId,  examId,  examTitle,  username,  score,  passPercentage
        );

        // Save certificate file path (optional)
        certificate.setCertificatePath("certificates/certificate_" + certificate.getId() + ".pdf");
        return certificateRepository.save(certificate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Certificate> getUserCertificates(Long userId) {
        return certificateRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Certificate> getCertificate(Long userId, Long examId) {
        return certificateRepository.findByUserIdAndExamId(userId, examId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getValidCertificateCount(Long userId) {
        return certificateRepository.countByUserIdAndIsValidTrue(userId);
    }

    @Override
    public Certificate getCertificateById(String certificateId) {
        try {
            return certificateRepository.findById(certificateId).orElseThrow();
        } catch (Exception e) {
            System.out.println("Certificate not found");
            return null;
        }

    }
}
