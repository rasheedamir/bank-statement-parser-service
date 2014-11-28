package com.ofg.bankstatement.parser.mt940.alior;

import static com.ofg.loans.pl.banks.BanksUtils.*;
import static org.apache.commons.lang.StringUtils.*;

import java.util.List;

import com.ofg.bankstatement.parser.mt940.Mt940BankStatementParser;
import com.ofg.bankstatement.util.BanksUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import com.ofg.loans.pl.banks.BanksUtils;
import com.ofg.loans.pl.banks.mt940.Mt940BankStatementParser;

@Component
class AliorBankStatementParser extends Mt940BankStatementParser {

    private static final String TRANSACTION_BLOC_LAST_FIELD1_90D = ":90D:";

    private static final String TRANSACTION_BLOC_LAST_FIELD2_90C = ":90C:";

    private static final String HEADER_LAST_FIELD_13D = ":13D:";

    private static final String ACCOUNT_NUMBER_FIELD_PREFIX_38 = "<38";

    @Override
    protected String getHeaderLastFieldPrefix() {
        return HEADER_LAST_FIELD_13D;
    }

    @Override
    protected String getTransactionBlockLastField1() {
        return TRANSACTION_BLOC_LAST_FIELD1_90D;
    }

    @Override
    protected String getTransactionBlockLastField2() {
        return TRANSACTION_BLOC_LAST_FIELD2_90C;
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
    protected String readAccountNumber(List<String> lines) {
        int payerAccountNumberLine = indexOfFirstLineWithPrefix(lines, ACCOUNT_NUMBER_FIELD_PREFIX_38);

        return payerAccountNumberLine > 0 ? BANK_ACCOUNT_PREFIX_PL + getLineIfExists(ACCOUNT_NUMBER_FIELD_PREFIX_38, lines, payerAccountNumberLine).trim() : EMPTY;
    }

    @Override
    public boolean isApplicableFor(String fileName) {
        return StringUtils.startsWithIgnoreCase(fileName, "export20") && StringUtils.endsWithIgnoreCase(fileName, ".sta");
    }

}
