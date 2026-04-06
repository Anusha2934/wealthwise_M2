package com.wealthwise.parser;

import com.wealthwise.entity.NavData;
import com.wealthwise.entity.SchemeMaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

/**
 * Parses the AMFI NAVAll.txt file.
 *
 * File format:
 * ─────────────────────────────────────────────────────────────────────
 * Aditya Birla Sun Life Mutual Fund          ← AMC header (no semicolons)
 *
 * Scheme Code;ISIN Div Payout;ISIN Div Reinvestment;Scheme Name;NAV;Date
 * 119551;INF209K01YO3;INF209K01YP0;ABSL Bluechip Fund - Direct;52.34;03-Apr-2026
 * ─────────────────────────────────────────────────────────────────────
 */
@Component
@Slf4j
public class NavAllTxtParser {

    // Locale.ENGLISH is REQUIRED — prevents parse failures on non-English OS locales
    private static final DateTimeFormatter NAV_DATE_FMT =
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    public static class ParseResult {
        public final List<SchemeMaster> schemes = new ArrayList<>();
        public final List<NavData>      navList = new ArrayList<>();
    }

    public ParseResult parse(InputStream inputStream) throws IOException {
        ParseResult result    = new ParseResult();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(inputStream, "UTF-8"));

        String currentAmc = "";
        String line;
        int lineNo = 0;

        while ((line = reader.readLine()) != null) {
            lineNo++;
            line = line.trim();
            if (line.isEmpty()) continue;

            // AMC header lines contain no semicolons
            if (!line.contains(";")) {
                currentAmc = line;
                continue;
            }

            // Skip the column-header row
            if (line.toLowerCase().startsWith("scheme code")) continue;

            String[] parts = line.split(";", -1);
            if (parts.length < 6) continue;

            try {
                // ── SchemeMaster record ───────────────────────────────
                SchemeMaster s = new SchemeMaster();
                s.setAmfiCode(parts[0].trim());
                s.setIsinGrowth(blank(parts[1]));
                s.setIsinIdcw(blank(parts[2]));
                s.setSchemeName(parts[3].trim());
                s.setAmcName(currentAmc);

                // Determine active: NAV must be a valid number
                String navStr = parts[4].trim();
                boolean active = isValidNav(navStr);
                s.setIsActive(active);

                // Enrich with parsed name fields
                SchemeNameParser.enrich(s);
                result.schemes.add(s);

                // ── NavData record ────────────────────────────────────
                if (active && parts.length >= 6) {
                    String dateStr = parts[5].trim();
                    try {
                        NavData nav = new NavData();
                        nav.setAmfiCode(parts[0].trim());
                        nav.setNavValue(new BigDecimal(navStr));
                        nav.setNavDate(LocalDate.parse(dateStr, NAV_DATE_FMT));
                        result.navList.add(nav);
                    } catch (Exception ignored) { /* skip bad dates */ }
                }

            } catch (Exception e) {
                log.warn("Skipping malformed line {}: {}", lineNo, e.getMessage());
            }
        }

        log.info("Parsed {} schemes and {} NAV records from NAVAll.txt",
                 result.schemes.size(), result.navList.size());
        return result;
    }

    private String blank(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private boolean isValidNav(String navStr) {
        if (navStr == null || navStr.equalsIgnoreCase("N.A.") || navStr.isBlank())
            return false;
        try {
            new BigDecimal(navStr);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
