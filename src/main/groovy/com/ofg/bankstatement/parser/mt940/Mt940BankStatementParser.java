package com.ofg.bankstatement.parser.mt940;

import static com.ofg.loans.pl.banks.BanksUtils.*;
import static org.apache.commons.lang.StringUtils.*;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;
import com.ofg.loans.api.beans.AddressInfo;
import com.ofg.loans.api.beans.payment.PaymentInfo;
import com.ofg.loans.api.beans.payment.PaymentType;
import com.ofg.loans.domain.Iban;
import com.ofg.loans.domain.services.BankStatementParser;
import com.ofg.loans.domain.util.InputStreamUtils;
import com.ofg.loans.pl.banks.BanksUtils;
import com.ofg.loans.util.date.DateTimeUtils;

public abstract class Mt940BankStatementParser implements BankStatementParser {

    protected static final String TRANSACTION_TYPE_IDENTIFICATION_CODE_N = "N";

    protected static final String TRANSACTION_TYPE_IDENTIFICATION_CODE_F = "F";

    protected static final String PAYMENT_TYPE_CREDIT = "C";

    protected static final String PAYMENT_TYPE_REVERSE_DEBIT = "RD";

    protected static final String PAYMENT_TYPE_REVERSE_CREDIT = "RC";

    protected static final String CURRENCY_MARK = "N";

    protected static final String BOOKING_DATE_FORMAT = "yyMMdd";

    protected static final String BANK_ACCOUNT_PREFIX_PL = "PL";

    protected static final String BANK_ACCOUNT_PREFIX_PL00 = "PL00";

    private static final String SUB_FIELD_PREFIX = "<";

    private static final String DETAILS_START_FIELD_PREFIX_20 = "<20";

    protected static final String TRANSACTION_PAYER_DETAILS1_FIELD_PREFIX_27 = "<27";

    protected static final String TRANSACTION_PAYER_DETAILS2_FIELD_PREFIX_28 = "<28";

    protected static final String TRANSACTION_PAYER_DETAILS3_FIELD_PREFIX_29 = "<29";

    private static final String ACCOUNT_NUMBER_FIRST_FIELD_PREFIX_30 = "<30";

    private static final String ACCOUNT_NUMBER_SECOND_FIELD_PREFIX_31 = "<31";

    private static final String PAYMENT_REFERENCE_NUMBER_FIELD_PREFIX_63 = "<63";

    private static final String ACCOUNT_IDENTIFICATION_FIELD_PREFIX_25 = ":25:";

    private static final String HEADER_LAST_FIELD_60F = ":60F:";

    private static final String CLOSING_BALANCE_FIELD_F = ":62F:";

    private static final String CLOSING_BALANCE_FIELD_M = ":62M:";

    private static final String TRANSACTION_FIELD_PREFIX_61 = ":61:";

    private static final String ENCODING_WINDOWS_1250 = "windows-1250";

    protected static final String CURRENCY_UNIT = "PLN";

    final static class Mt940Payment {
        private List<String> headerLines;

        private List<String> statementLines;

        private String companyBankAccount;

        private Mt940Payment(String companyBankAccount, List<String> headerLines, List<String> statementLines) {
            this.headerLines = headerLines;
            this.statementLines = statementLines;
            this.companyBankAccount = companyBankAccount;
        }

        public List<String> getStatementLines() {
            return statementLines;
        }

        public List<String> getHeaderLines() {
            return headerLines;
        }

        public String getCompanyBankAccount() {
            return companyBankAccount;
        }
    }

    protected String getImportFileEncoding() {
        return ENCODING_WINDOWS_1250;
    }

    @Override
    public List<PaymentInfo> importBankStatement(InputStream inputStream) {
        List<String> lines = InputStreamUtils.readLines(inputStream, getImportFileEncoding());
        List<Mt940Payment> mt940Payments = prepareMt940Payments(lines);
        return getPaymentsInfo(mt940Payments);
    }

    protected void fillPaymentStatementFieldsData(PaymentInfo payment, String line) {
        int parsingIndex = 4;
        parsingIndex = fillBookingDate(payment, line, parsingIndex);
        parsingIndex = fillPaymentType(payment, line, parsingIndex);
        parsingIndex = fillCurrency(payment, line, parsingIndex);
        parsingIndex = fillAmount(payment, line, parsingIndex);
        fillOperation(payment, line, parsingIndex);
    }

    protected int fillBookingDate(PaymentInfo payment, String line, int parsingIndex) {
        int length = BOOKING_DATE_FORMAT.length();
        String datePart = line.substring(parsingIndex, parsingIndex + length);
        Date bookingDate = DateTimeUtils.date(datePart, BOOKING_DATE_FORMAT);
        payment.setBookingDate(bookingDate);
        return parsingIndex + length;
    }

    protected int fillPaymentType(PaymentInfo payment, String line, int parsingIndex) {
        // Skip 4 characters
        int position = parsingIndex + 4;

        // Debit/Credit mark – ‘D’ Debit, ‘C’ Credit, ‘RD’ Reverse Debit, ‘RC’ Reverse Credit)
        int length = 2;
        String debitCreditMark = line.substring(position, position + length);
        PaymentType type = debitCreditMark.startsWith(PAYMENT_TYPE_CREDIT) || debitCreditMark.equals(
                PAYMENT_TYPE_REVERSE_DEBIT) ? PaymentType.INCOMING : PaymentType.OUTGOING;
        payment.setType(type);
        // Funds code payment currency
        length = debitCreditMark.equals(PAYMENT_TYPE_REVERSE_DEBIT) || debitCreditMark.equals(PAYMENT_TYPE_REVERSE_CREDIT) ? 2 : 1;
        return position + length;
    }

    protected int fillCurrency(PaymentInfo payment, String line, int parsingIndex) {
        int length = 0;
        if (isCheckCurrencyRequired()) {
            length = 1;
            String currencyMark = line.substring(parsingIndex, parsingIndex + length);
            Preconditions.checkState(CURRENCY_MARK.equals(currencyMark.trim()), "Currency must be equals to PLN!");
        }
        payment.setUnit(CURRENCY_UNIT);
        return parsingIndex + length;
    }

    protected int fillAmount(PaymentInfo payment, String line, int parsingIndex) {
        String amountValue = parseAmount(line, parsingIndex);
        payment.setAmount(new BigDecimal(amountValue.replace(",", ".")));
        return BanksUtils.indexOfFirstLetter(line, parsingIndex);
    }

    protected void fillOperation(PaymentInfo payment, String line, int parsingIndex) {
        String transactionTypeIdentificationCode = line.substring(parsingIndex, parsingIndex + 1);
        if (Arrays.asList(TRANSACTION_TYPE_IDENTIFICATION_CODE_N, TRANSACTION_TYPE_IDENTIFICATION_CODE_F)
                .contains(transactionTypeIdentificationCode)) {
            String transactionTypeCode = line.substring(parsingIndex + 1, parsingIndex + 4);
            Mt940TransactionType type = Mt940TransactionType.findByCode(transactionTypeCode);
            if (type != null) {
                payment.setBankOperationType(type.name());
                payment.setBankOperationName(type.getDescription());
            }
        }
    }

    protected String parseAmount(String statementLine, int transactionAmountStartPosition) {
        int transactionAmountEndPosition = BanksUtils.indexOfFirstLetter(statementLine, transactionAmountStartPosition + 1);
        return statementLine.substring(transactionAmountStartPosition, transactionAmountEndPosition);
    }

    protected boolean isCheckCurrencyRequired() {
        return true;
    }

    protected String parseBankReference(List<String> statementLines) {
        int bankReferenceLineIndex = indexOfFirstLineWithPrefix(statementLines, getBankReferenceFieldPrefix());
        if (bankReferenceLineIndex > -1) {
            String bankReferenceLine = getLineIfExists(getBankReferenceFieldPrefix(), statementLines, bankReferenceLineIndex);
            return parseFieldValue(getBankReferenceFieldPrefix(), bankReferenceLine);
        }
        return null;
    }

    protected String parsePayerName(List<String> statementLines) {
        String payedDetails = parsePayerDetails(statementLines);
        return preparePayerName(payedDetails);
    }

    protected void fillPaymentOtherFieldsData(PaymentInfo payment, List<String> statementLines) {
        String payerNameAndAddress = parsePayerDetails(statementLines);
        String payerName = parsePayerName(statementLines);
        AddressInfo addressInfo = new AddressInfo();
        addressInfo.setLocation6(StringUtils.removeStart(payerNameAndAddress, payerName).trim());
        payment.setAccountHolderAddress(addressInfo);
        payment.setAccountNumber(readAccountNumber(statementLines));
        payment.setAccountHolderName(payerName);
        String bankReference = parseBankReference(statementLines);
        payment.setBankReference(prepareBankReference(bankReference, payment));
    }

    protected String preparePayerName(String payerDetails) {
        return BanksUtils.parsePersonNameFromDetails(payerDetails);
    }

    protected String prepareAccountNumber(String payerIbanBranchCode, String payerClientAccountNumber) {
        if (StringUtils.isNotEmpty(payerIbanBranchCode) && StringUtils.isNotEmpty(payerClientAccountNumber)) {
            String checkDigits = Iban.generateIbanCheckDigits(BANK_ACCOUNT_PREFIX_PL00 + payerIbanBranchCode + payerClientAccountNumber);
            return BANK_ACCOUNT_PREFIX_PL + checkDigits + payerIbanBranchCode + payerClientAccountNumber;
        } else {
            return StringUtils.EMPTY;
        }
    }

    protected String parsePayerDetails(List<String> lines) {
        return StringUtils.join(
                new String[] { BanksUtils.getFieldValue(lines, TRANSACTION_PAYER_DETAILS1_FIELD_PREFIX_27),
                        BanksUtils.getFieldValue(lines, TRANSACTION_PAYER_DETAILS2_FIELD_PREFIX_28), BanksUtils.getFieldValue(lines, TRANSACTION_PAYER_DETAILS3_FIELD_PREFIX_29) })
                .trim();
    }

    protected List<Mt940Payment> prepareMt940Payments(List<String> lines) {
        List<String> headerLines = new ArrayList<String>();
        List<String> statementLines = new ArrayList<String>();
        String companyBankAccount = null;
        List<Mt940Payment> mt940Payments = new ArrayList<Mt940Payment>();

        boolean processHeaderCollection = true;

        for (String currentLine : lines) {
            if (processHeaderCollection) {
                if (currentLine.startsWith(ACCOUNT_IDENTIFICATION_FIELD_PREFIX_25)) {
                    companyBankAccount = parseFieldValue(ACCOUNT_IDENTIFICATION_FIELD_PREFIX_25, currentLine);
                }
                headerLines.add(currentLine);
                if (isHeaderLastField(currentLine)) {
                    headerLines.add(currentLine);
                    processHeaderCollection = false;
                }
            } else {
                if (currentLine.startsWith(TRANSACTION_FIELD_PREFIX_61) && !statementLines.isEmpty()) {
                    mt940Payments.add(new Mt940Payment(companyBankAccount, headerLines, statementLines));
                    statementLines = new ArrayList<String>();
                    statementLines.add(currentLine);
                } else if (isTransactionBlockLastField(currentLine)) {
                    mt940Payments.add(new Mt940Payment(companyBankAccount, headerLines, statementLines));
                    headerLines = new ArrayList<String>();
                    statementLines = new ArrayList<String>();
                    companyBankAccount = null;
                    processHeaderCollection = true;
                } else {
                    statementLines.add(currentLine);
                }
            }

        }
        return mt940Payments;
    }

    protected boolean isHeaderLastField(String line) {
        return line.startsWith(getHeaderLastFieldPrefix());
    }

    protected boolean isTransactionBlockLastField(String line) {
        return line.startsWith(getTransactionBlockLastField1()) || line.startsWith(getTransactionBlockLastField2());
    }

    protected String getHeaderLastFieldPrefix() {
        return HEADER_LAST_FIELD_60F;
    }

    protected String getTransactionBlockLastField1() {
        return CLOSING_BALANCE_FIELD_F;
    }

    protected String getTransactionBlockLastField2() {
        return CLOSING_BALANCE_FIELD_M;
    }

    protected List<PaymentInfo> getPaymentsInfo(List<Mt940Payment> mt940Payments) {
        List<PaymentInfo> payments = new ArrayList<PaymentInfo>();
        for (Mt940Payment mt940Payment : mt940Payments) {
            PaymentInfo payment = new PaymentInfo();

            payment.setDetails(prepareDetails(readDetailsFields(mt940Payment.getStatementLines())).trim());
            payment.setCompanyBankAccount(prepareCompanyBankAccount(mt940Payment.getCompanyBankAccount()));

            fillPaymentStatementFieldsData(payment, mt940Payment.getStatementLines().get(0));
            fillPaymentOtherFieldsData(payment, mt940Payment.getStatementLines());

            payments.add(payment);
        }

        return payments;
    }

    protected String readAccountNumber(List<String> statementLines) {
        int payerIbanBranchCodeLine = indexOfFirstLineWithPrefix(statementLines, ACCOUNT_NUMBER_FIRST_FIELD_PREFIX_30);
        int payerClientAccountNumberLine = indexOfFirstLineWithPrefix(statementLines, ACCOUNT_NUMBER_SECOND_FIELD_PREFIX_31);
        String payerIbanBranchCode = getLineIfExists(ACCOUNT_NUMBER_FIRST_FIELD_PREFIX_30, statementLines, payerIbanBranchCodeLine);
        String payerClientAccountNumber = getLineIfExists(ACCOUNT_NUMBER_SECOND_FIELD_PREFIX_31, statementLines, payerClientAccountNumberLine);
        return prepareAccountNumber(payerIbanBranchCode, payerClientAccountNumber);
    }

    protected String readDetailsFields(List<String> lines) {
        int detailsPrefixFirstLine = indexOfFirstLineWithPrefix(lines, DETAILS_START_FIELD_PREFIX_20);

        return detailsPrefixFirstLine >= 0 ? joinFieldValuesFromTo(lines, SUB_FIELD_PREFIX, 20, detailsPrefixFirstLine, 7) : EMPTY;
    }

    protected String prepareDetails(String unparsedDetails) {
        return unparsedDetails;
    }

    protected String prepareBankReference(String bankReference, PaymentInfo payment) {
        if (StringUtils.isEmpty(bankReference)) {
            return BanksUtils.md5BankReference(payment);
        } else {
            return bankReference;
        }
    }

    private String getBankReferenceFieldPrefix() {
        return PAYMENT_REFERENCE_NUMBER_FIELD_PREFIX_63;
    }

    protected String prepareCompanyBankAccount(String companyBankAccount) {
        return companyBankAccount;
    }

}
