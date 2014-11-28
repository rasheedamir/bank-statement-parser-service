package com.ofg.bankstatement.parser.mt940.kredyt;

import com.google.common.collect.ImmutableMap;
import com.ofg.bankstatement.parser.mt940.Mt940TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

// For details please search in Google:
// "MT940- Pelen opis mapowania kodow PS i MB na kody biznesowe oraz 10-cyfrowe kody wewnetrzne"
public class BZWBKBusinessCodeUtil {

    private static final Logger log = LoggerFactory.getLogger(BZWBKBusinessCodeUtil.class);

    private static final Map<String, String> BUSINESS_CODE_2_SWIFT;

    static {
        BUSINESS_CODE_2_SWIFT = ImmutableMap.<String, String>builder()
                .put("0011", "NTRF")
                .put("0013", "FCHG")
                .put("0016", "FCHG")
                .put("51CL", "FCHG")
                .put("51CT", "FCHG")
                .put("54CG", "FMSC")
                .put("56C1", "NTRF")
                .put("5BCG", "FCHG")
                .put("ATMD", "FCHG")
                .put("B101", "FCHG")
                .put("B102", "FCHG")
                .put("B103", "FCHG")
                .put("B104", "FCHG")
                .put("B105", "FCHG")
                .put("B106", "NTRF")
                .put("B109", "NTRF")
                .put("B110", "FCHG")
                .put("B112", "FCHG")
                .put("B113", "FCHG")
                .put("B114", "FCHG")
                .put("B115", "FCHG")
                .put("B116", "FCHG")
                .put("B117", "FCHG")
                .put("B118", "FCHG")
                .put("B120", "FCHG")
                .put("B121", "FCHG")
                .put("B125", "FCHG")
                .put("B126", "FCHG")
                .put("B127", "FCHG")
                .put("B130", "FCHG")
                .put("B131", "FCHG")
                .put("B2CG", "FCHG")
                .put("B343", "NTRF")
                .put("B353", "FMSC")
                .put("B5LC", "NTRF")
                .put("B8CT", "FCHG")
                .put("B9CD", "NTRF")
                .put("BHC5", "FCHG")
                .put("BHC7", "FCHG")
                .put("BHC9", "FCHG")
                .put("BHCC", "FCHG")
                .put("BHCL", "FCHG")
                .put("BHCS", "FCHG")
                .put("BHCU", "FCHG")
                .put("BHCW", "FCHG")
                .put("BHCZ", "FCHG")
                .put("BHLC", "NTRF")
                .put("BMCC", "FCHG")
                .put("BMCT", "FCHG")
                .put("BOC2", "FCHG")
                .put("BOC3", "FCHG")
                .put("BOC4", "FCHG")
                .put("BOC5", "FCHG")
                .put("BOC6", "FCHG")
                .put("BOC7", "FCHG")
                .put("BRAK", "FMSC")
                .put("BXCG", "FCHG")
                .put("BZ10", "NTRF")
                .put("BZ11", "FCHG")
                .put("BZ12", "FCHG")
                .put("BZ13", "FCHG")
                .put("BZ14", "FCHG")
                .put("BZ15", "FCHG")
                .put("BZ16", "FCHG")
                .put("BZ17", "FCHG")
                .put("BZ18", "FCHG")
                .put("BZ19", "FCHG")
                .put("BZ28", "NTRF")
                .put("CCRA", "NTRF")
                .put("CWHC", "FMSC")
                .put("EC20", "NTRF")
                .put("EC2D", "NTRF")
                .put("EC2G", "NTRF")
                .put("EC2M", "NTRF")
                .put("EC2N", "NTRF")
                .put("EC2O", "NTRF")
                .put("EC2X", "NTRF")
                .put("EE20", "NTRF")
                .put("EF12", "NTRF")
                .put("EF1C", "FCHG")
                .put("EF1P", "NTRF")
                .put("EF4P", "NTRF")
                .put("EFDB", "NTRF")
                .put("EFDC", "FCHG")
                .put("EXKB", "FMSC")
                .put("M108", "FCHG")
                .put("M128", "FCHG")
                .put("M129", "FCHG")
                .put("M240", "FCHG")
                .put("M243", "NTRF")
                .put("M244", "FCHG")
                .put("M245", "FCHG")
                .put("M246", "NTRF")
                .put("M350", "FCHG")
                .put("M401", "NTRF")
                .put("M402", "FCHG")
                .put("M403", "FCHG")
                .put("M406", "FCHG")
                .put("M410", "NTRF")
                .put("M412", "FCHG")
                .put("M413", "FCHG")
                .put("M420", "FCHG")
                .put("S141", "FCHG")
                .put("S146", "FCHG")
                .put("S152", "NTRF")
                .put("SF20", "FCHG")
                .put("TC20", "NTRF")
                .put("TC2B", "NTRF")
                .put("W_01", "FCHG")
                .put("W_02", "FCHG")
                .put("WT42", "NTRF")
                .put("X_01", "FCHG")
                .put("X_02", "FCHG")
                .put("X_03", "FCHG")
                .put("X_04", "FCHG")
                .put("X_05", "FCHG")
                .put("X_06", "FCHG")
                .put("X_07", "FCHG")
                .put("X_08", "FCHG")
                .put("X_09", "NTRF")
                .put("X_BR", "FMSC")
                .build();
    }

    private BZWBKBusinessCodeUtil() {
        // No constructor for util class
    }

    public static final Mt940TransactionType toMt940TransactionType(String businessCode) {
        String swiftCode = BUSINESS_CODE_2_SWIFT.get(businessCode);
        if (swiftCode == null) {
            log.error("Cannot find such business code: {}", businessCode);
            return null;
        } else {
            String mt940Code = swiftCode.substring(1);
            return Mt940TransactionType.findByCode(mt940Code);
        }
    }
}
