package com.wealthwise.service;

import com.wealthwise.parser.NavAllTxtParser;
import com.wealthwise.repository.NavDataRepository;
import com.wealthwise.repository.SchemeMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * F02.1 — AMFI Scheme Database Seeder
 *
 * On first startup : reads NAVAll.txt and populates scheme_master + nav_data.
 * Every night      : re-downloads NAVAll.txt to refresh NAVs and active flags.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchemeSeederService {

    private final SchemeMasterRepository schemeRepo;
    private final NavDataRepository      navRepo;
    private final NavAllTxtParser        parser;

    @Value("${app.navall.file.path}")
    private String navAllFilePath;

    // ── Run ONCE on first startup ─────────────────────────────────────────
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (schemeRepo.count() > 0) {
            log.info("scheme_master already seeded ({} records). Skipping initial seed.",
                     schemeRepo.count());
            return;
        }
        log.info("First startup detected — seeding scheme_master from NAVAll.txt ...");
        try {
            seed();
        } catch (IOException e) {
            log.error("Initial seed failed: {}", e.getMessage(), e);
        }
    }

    // ── Daily sync at midnight ────────────────────────────────────────────
    @Scheduled(cron = "0 0 0 * * *")
    public void dailySync() {
        log.info("Daily NAV sync started ...");
        try {
            seed();
        } catch (IOException e) {
            log.error("Daily sync failed: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void seed() throws IOException {
        try (InputStream is = new FileInputStream(navAllFilePath)) {

            NavAllTxtParser.ParseResult result = parser.parse(is);

            // Upsert schemes (save or update by amfiCode)
            result.schemes.forEach(s -> {
                schemeRepo.findByAmfiCode(s.getAmfiCode()).ifPresentOrElse(
                    existing -> {
                        existing.setIsActive(s.getIsActive());
                        existing.setUpdatedAt(java.time.LocalDateTime.now());
                        schemeRepo.save(existing);
                    },
                    () -> schemeRepo.save(s)
                );
            });

            // Upsert NAV records (ignore duplicates for same date)
            result.navList.forEach(nav -> {
                try {
                    navRepo.save(nav);
                } catch (Exception ignored) {
                    // unique constraint on (amfi_code, nav_date) — skip duplicates
                }
            });

            log.info("Seed complete — schemes: {}, NAV records: {}",
                     result.schemes.size(), result.navList.size());
        }
    }
}
