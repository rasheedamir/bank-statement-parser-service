package com.ofg.bank.parser.mt940.ing;

import static com.ofg.loans.pl.banks.BanksUtils.*;

import java.util.List;

import com.ofg.bank.parser.mt940.Mt940BankStatementParser;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import com.ofg.loans.api.beans.AddressInfo;
import com.ofg.loans.api.beans.payment.PaymentInfo;
import com.ofg.loans.pl.banks.BanksUtils;
import com.ofg.loans.pl.banks.mt940.Mt940BankStatementParser;

@Component
class IngBankStatementParser extends Mt940BankStatementParser {

    private static final String SUB_FIELD_PREFIX_SYMBOL = "~";

    private static final String ACCOUNT_NUMBER_FIELD_29 = "~29";

    private static final String PAYER_DETAILS_FIELD_33 = "~33";

    private static final String PAYER_DETAILS_FIELD_32 = "~32";

    private static final String PAYER_DETAILS_FIELD_31 = "~31";

    private static final String DETAILS_FIELD_20 = "~20";

    private static final String DETAILS_FIELD_21 = "~21";

    private static final String DETAILS_FIELD_22 = "~22";

    private static final String DETAILS_FIELD_23 = "~23";

    private static final String DETAILS_FIELD_24 = "~24";

    private static final int INDEX_OF_AMOUNT_START = 15;

    private static final String HEADER_LAST_FIELD_13 = ":13:";

    private static final String ENCODING_CP852 = "Cp852";

    private static final String TRANSACTION_BLOC_LAST_FIELD = "-";

    @Override
    protected String getImportFileEncoding() {
        return ENCODING_CP852;
    }

    @Override
    protected String getHeaderLastFieldPrefix() {
        return HEADER_LAST_FIELD_13;
    }

    @Override
    protected String getTransactionBlockLastField1() {
        return TRANSACTION_BLOC_LAST_FIELD;
    }

    @Override
    protected String getTransactionBlockLastField2() {
        return TRANSACTION_BLOC_LAST_FIELD;
    }

    @Override
    protected boolean isCheckCurrencyRequired() {
        return false;
    }

    @Override
    protected String parseAmount(String statementLine, int transactionAmountStartPosition) {
        transactionAmountStartPosition = INDEX_OF_AMOUNT_START;
        int transactionAmountEndPosition = BanksUtils.indexOfFirstLetter(statementLine, INDEX_OF_AMOUNT_START + 1);
        return statementLine.substring(transactionAmountStartPosition, transactionAmountEndPosition);
    }

    @Override
    protected String readDetailsFields(List<String> lines) {
        List<String> separatedFieldsLines = BanksUtils.extractOneSubFieldInOneLine(lines, SUB_FIELD_PREFIX_SYMBOL);
        return StringUtils.join(
                new String[] { BanksUtils.getFieldValue(separatedFieldsLines, DETAILS_FIELD_20), BanksUtils.getFieldValue(separatedFieldsLines, DETAILS_FIELD_21),
                        BanksUtils.getFieldValue(separatedFieldsLines, DETAILS_FIELD_22), BanksUtils.getFieldValue(separatedFieldsLines, DETAILS_FIELD_23),
                        BanksUtils.getFieldValue(separatedFieldsLines, DETAILS_FIELD_24) }).trim();
    }

    @Override
    protected void fillPaymentOtherFieldsData(PaymentInfo payment, List<String> statementLines) {
        AddressInfo addressInfo = new AddressInfo();
        List<String> lines = BanksUtils.extractOneSubFieldInOneLine(statementLines, SUB_FIELD_PREFIX_SYMBOL);

        payment.setAccountNumber(readAccountNumber(lines));

        int payerDetailsLineIndex = indexOfFirstLineWithPrefix(lines, PAYER_DETAILS_FIELD_31);
        if (payerDetailsLineIndex > 0) {
            String payerDetails =
                    StringUtils.join(new String[] { BanksUtils.getFieldValue(lines, PAYER_DETAILS_FIELD_32), BanksUtils.getFieldValue(lines, PAYER_DETAILS_FIELD_33) });
            String payerName = BanksUtils.parsePersonNameFromDetails(payerDetails);
            payment.setAccountHolderName(payerName);

            String payerAddress = payerDetails.substring(payerName.length(), payerDetails.length()).trim();
            addressInfo.setLocation6(payerAddress);
        }

        payment.setAccountHolderAddress(addressInfo);
        payment.setCompanyBankAccount(payment.getCompanyBankAccount().replace("/", ""));

        if (payment.getBankReference() == null) {
            payment.setBankReference(prepareBankReference("", payment));
        }
    }

    @Override
    protected String readAccountNumber(List<String> lines) {
        return BANK_ACCOUNT_PREFIX_PL + BanksUtils.getFieldValue(lines, ACCOUNT_NUMBER_FIELD_29);
    }

    @Override
    public boolean isApplicableFor(String fileName) {
        return StringUtils.startsWithIgnoreCase(fileName, "MT942_M_20") && StringUtils.endsWithIgnoreCase(fileName, ".txt");
    }

}
