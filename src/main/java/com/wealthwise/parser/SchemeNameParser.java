package com.wealthwise.parser;

import com.wealthwise.entity.SchemeMaster;
import com.wealthwise.entity.SchemeMaster.FundType;
import com.wealthwise.entity.SchemeMaster.OptionType;
import com.wealthwise.entity.SchemeMaster.PlanType;

/**
 * Parses a mutual fund scheme name and enriches the SchemeMaster entity
 * with plan type, option type, fund type and fund family name.
 *
 * Example input:
 *   "HDFC Bluechip Fund - Direct Plan - Growth"
 *
 * Output fields populated:
 *   planType       = DIRECT
 *   optionType     = GROWTH
 *   fundType       = OPEN_ENDED
 *   fundFamilyName = "Bluechip Fund"
 */
public class SchemeNameParser {

    public static void enrich(SchemeMaster scheme) {
        String name = scheme.getSchemeName().toUpperCase();

        // ── Plan Type ────────────────────────────────────────────────────
        if (name.contains("DIRECT"))
            scheme.setPlanType(PlanType.DIRECT);
        else
            scheme.setPlanType(PlanType.REGULAR);

        // ── Option Type ──────────────────────────────────────────────────
        if (name.contains("IDCW") && name.contains("REINVEST"))
            scheme.setOptionType(OptionType.IDCW_REINVESTMENT);
        else if (name.contains("IDCW") || name.contains("DIVIDEND"))
            scheme.setOptionType(OptionType.IDCW_PAYOUT);
        else
            scheme.setOptionType(OptionType.GROWTH);

        // ── Fund Type ────────────────────────────────────────────────────
        if (name.contains("CLOSE ENDED") || name.contains("CLOSE-ENDED"))
            scheme.setFundType(FundType.CLOSE_ENDED);
        else if (name.contains("INTERVAL"))
            scheme.setFundType(FundType.INTERVAL);
        else
            scheme.setFundType(FundType.OPEN_ENDED);

        // ── Fund Family Name ─────────────────────────────────────────────
        scheme.setFundFamilyName(
            extractFundFamily(scheme.getSchemeName(), scheme.getAmcName()));
    }

    private static String extractFundFamily(String schemeName, String amcName) {
        String family = schemeName;

        // Remove AMC prefix (case-insensitive)
        if (amcName != null && !amcName.isBlank()) {
            String lowerScheme = schemeName.toLowerCase();
            String lowerAmc    = amcName.toLowerCase();
            if (lowerScheme.startsWith(lowerAmc)) {
                family = schemeName.substring(amcName.length()).trim();
            }
        }

        // Remove leading dashes or spaces
        family = family.replaceAll("^[-\\s]+", "");

        // Remove plan/option/type suffixes
        family = family.replaceAll("(?i)\\s*-?\\s*(direct|regular).*", "").trim();
        family = family.replaceAll("(?i)\\s*-?\\s*(growth|idcw|dividend).*", "").trim();
        family = family.replaceAll("(?i)\\s*-?\\s*(plan).*", "").trim();

        return family.isBlank() ? schemeName : family;
    }
}
