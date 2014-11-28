package com.ofg.bankparsers.parser.mt940.millenium;

import static com.ofg.loans.pl.banks.BanksUtils.*;
import static org.apache.commons.lang.StringUtils.*;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.ofg.loans.pl.banks.BanksUtils;
import com.ofg.loans.pl.banks.mt940.Mt940BankStatementParser;

@Component
class MilleniumBankStatementParser extends Mt940BankStatementParser {
    private static final String DETAILS_FIELD_PREFIX_22 = "<22";

    private static final String DETAILS_FIELD_PREFIX_23 = "<23";

    private static final String DETAILS_FIELD_PREFIX_24 = "<24";

    private static final String DETAILS_FIELD_PREFIX_25 = "<25";

    private static final String DETAILS_FIELD_PREFIX_26 = "<26";

    private static final String ACCOUNT_NUMBER_FIELD_PREFIX_38 = "<38";

    @Override
    protected String readDetailsFields(List<String> lines) {
        int detailsPrefixFirstLine = indexOfFirstLineWithPrefix(lines, DETAILS_FIELD_PREFIX_22);
        final List<String> result = Lists.newArrayList();
        if (detailsPrefixFirstLine >= 0) {
            result.addAll(ImmutableList.of(getLineIfExists(DETAILS_FIELD_PREFIX_22, lines, detailsPrefixFirstLine),
                    getLineIfExists(DETAILS_FIELD_PREFIX_23, lines, detailsPrefixFirstLine + 1), getLineIfExists(DETAILS_FIELD_PREFIX_24, lines, detailsPrefixFirstLine + 2),
                    getLineIfExists(DETAILS_FIELD_PREFIX_25, lines, detailsPrefixFirstLine + 3), getLineIfExists(DETAILS_FIELD_PREFIX_26, lines, detailsPrefixFirstLine + 4)));
        }

        return StringUtils.join(result.toArray());
    }

    @Override
    protected String readAccountNumber(List<String> lines) {
        int payerAccountNumberLine = indexOfFirstLineWithPrefix(lines, ACCOUNT_NUMBER_FIELD_PREFIX_38);

        return payerAccountNumberLine > 0 ? BANK_ACCOUNT_PREFIX_PL + getLineIfExists(ACCOUNT_NUMBER_FIELD_PREFIX_38, lines, payerAccountNumberLine).trim() : EMPTY;
    }

    @Override
    protected String parsePayerDetails(List<String> lines) {
        String firstLine = BanksUtils.getFieldValue(lines, TRANSACTION_PAYER_DETAILS1_FIELD_PREFIX_27);
        firstLine = firstLine.endsWith(" ") ? firstLine : firstLine + " ";

        String details =
                StringUtils.join(
                        new String[] { firstLine, BanksUtils.getFieldValue(lines, TRANSACTION_PAYER_DETAILS2_FIELD_PREFIX_28),
                                BanksUtils.getFieldValue(lines, TRANSACTION_PAYER_DETAILS3_FIELD_PREFIX_29) }).trim();

        return details;
    }

    @Override
    public boolean isApplicableFor(String fileName) {
        return StringUtils.startsWithIgnoreCase(fileName, "Historia_transakcji_") && StringUtils.endsWithIgnoreCase(fileName, ".sta");
    }

}
