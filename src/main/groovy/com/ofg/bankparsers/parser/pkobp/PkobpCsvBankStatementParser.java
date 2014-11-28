package com.ofg.bankparsers.parser.pkobp;

import static org.apache.commons.lang.StringUtils.*;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.ofg.bankparsers.parser.BankStatementParser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ofg.loans.api.beans.AddressInfo;
import com.ofg.loans.api.beans.payment.PaymentInfo;
import com.ofg.loans.api.beans.payment.PaymentType;
import com.ofg.loans.domain.model.payment.OperationalBank;
import com.ofg.loans.domain.services.BankStatementParser;
import com.ofg.loans.domain.util.InputStreamUtils;
import com.ofg.loans.pl.banks.BankFinderPl;
import com.ofg.loans.pl.banks.BanksUtils;
import com.ofg.loans.pl.banks.ImmediateIdentificationPaymentInfoModifier;
import com.ofg.loans.util.date.DateTimeUtils;
import com.ofg.loans.util.numeric.BigDecimalUtils;

@Component
public class PkobpCsvBankStatementParser implements BankStatementParser {

    private static final Logger log = LoggerFactory.getLogger(PkobpCsvBankStatementParser.class);

    private static final String POINT_SYMBOL = ".";

    private static final String COMMA_SYMBOL = ",";

    private static final String CURRENCY_MUST_BE_EQUALS_TO_PLN = "Currency must be equals to PLN!";

    private static final String PKOBP_BANK_IBAN_CODE = "102";

    private static final String PAYER_ADDRESS_TEXT_PREFIX = "Dane adr. rach. przeciwst.:  ";

    private static final String PAYER_ACCOUNT_NUMBER_TEXT_PREFIX = "Nr rach. przeciwst.:";

    private static final String INCOMING_PAYMENT_OPERATION_TYPE1 = "Wpływ na rachunek";

    private static final String INCOMING_PAYMENT_OPERATION_TYPE2 = "Przelew na rachunek";

    private static final String OUTGOING_PAYMENT_OPERATION = "Przelew z rachunku";

    private static final String CORRECTION_ON_ACCOUNT_OPERATION = "Korekta";

    private static final String BANKING_FEE_OPERATION = "Opłata";

    private static final String LOAD_OPERATION = "Obciążenie";

    private static final String DETAILS_PREFIX = "Tytuł:";

    private static final String INCOMING_CASH_PAYMENT = "Wpłata gotówkowa w kasie";

    private static final String SHORT_TERM_PRODUCT_STATEMENT_FILE_PREFIX = "historia_120202010908_";
    
    private static final String LONG_TERM_PRODUCT_STATEMENT_FILE_PREFIX = "historia_160202354090_";

    @Autowired
    BankFinderPl bankFinder;

    ImmediateIdentificationPaymentInfoModifier paymentInfoModifier = new ImmediateIdentificationPaymentInfoModifier();

    private static final String ENCODING = "ISO-8859-2";

    private static final String ACCOUNT_NUMBER_COUNTRY_PREFIX = "PL";

    private static final String CURRENCY_UNIT_PLN = "PLN";

    private static final String BOOKING_DATE_FORMAT = "yyyy-MM-dd";

    private static final String CELL_DOUBLE_QUOTES = "\"";

    @Override
    public List<PaymentInfo> importBankStatement(InputStream is) {
        List<String> lines = InputStreamUtils.readLines(is, ENCODING);
        return getPaymentsInfo(lines, false);
    }

    public PaymentType parsePaymentType(String fieldValue) {
        if (OUTGOING_PAYMENT_OPERATION.equals(fieldValue)) {
            return PaymentType.OUTGOING;
        } else if (INCOMING_PAYMENT_OPERATION_TYPE1.equals(fieldValue) || INCOMING_PAYMENT_OPERATION_TYPE2.equals(fieldValue)) {
            return PaymentType.INCOMING;
        } else if (INCOMING_CASH_PAYMENT.equals(fieldValue)) {
            return PaymentType.INCOMING;
        } else {
            return null;
        }
    }

    private List<PaymentInfo> getPaymentsInfo(List<String> lines, boolean oldVersoin) {
        List<PaymentInfo> payments = new ArrayList<PaymentInfo>();
        OperationalBank bank = bankFinder.findBankByIbanCode(PKOBP_BANK_IBAN_CODE);
        String bankAccountNumber = bank != null && bank.getBankAccounts().size() > 0 ? bank.getBankAccounts().get(0).getAccountNumber() : "";
        Preconditions.checkState(!StringUtils.isEmpty(bankAccountNumber), "Bank Account for bank Iban code " + PKOBP_BANK_IBAN_CODE + " not found!");

        for (String currentLine : lines) {
            if (lines.get(0).equals(currentLine)) {
                // skip CSV header row
                continue;
            }
            String[] tokens = splitRow(currentLine);
            if (tokens.length < 7) {
                log.error("Skiping payment, unsupported line: {}", currentLine);
                continue;
            }

            PaymentInfo currentPayment = new PaymentInfo();
            currentPayment.setCompanyBankAccount(bankAccountNumber);

            Date bookingDate = DateTimeUtils.date(tokens[0], BOOKING_DATE_FORMAT);
            currentPayment.setBookingDate(bookingDate);

            String operationType = tokens[2];
            if (CORRECTION_ON_ACCOUNT_OPERATION.equals(operationType) || BANKING_FEE_OPERATION.equals(operationType) || LOAD_OPERATION.equals(operationType)) {

                BigDecimal amount = new BigDecimal(tokens[4].replaceAll(COMMA_SYMBOL, POINT_SYMBOL));
                currentPayment.setAmount(amount.abs());
                currentPayment.setType(BigDecimalUtils.isNegativeAmount(amount) ? PaymentType.OUTGOING : PaymentType.INCOMING);

                currentPayment.setDetails(parseDetails(tokens[3] + ", " + tokens[6]));
                Preconditions.checkState(CURRENCY_UNIT_PLN.equals(tokens[5]), CURRENCY_MUST_BE_EQUALS_TO_PLN);

                currentPayment.setBankReference(BanksUtils.md5BankReference(currentPayment));
            } else if (INCOMING_PAYMENT_OPERATION_TYPE1.equals(operationType) || INCOMING_PAYMENT_OPERATION_TYPE2.equals(operationType)
                    || OUTGOING_PAYMENT_OPERATION.equals(operationType) || INCOMING_CASH_PAYMENT.equals(operationType)) {
                PaymentType paymentType = parsePaymentType(operationType);
                currentPayment.setType(paymentType);

                if (INCOMING_CASH_PAYMENT.equals(operationType)) {
                    fillStandartPaymentData(currentPayment, tokens, 3);
                } else {
                    if (!StringUtils.isEmpty(tokens[3])) {
                        String senderAccountNumber = ACCOUNT_NUMBER_COUNTRY_PREFIX + tokens[3].substring(
                                PAYER_ACCOUNT_NUMBER_TEXT_PREFIX.length(), tokens[3].length()).replaceAll(" ", "");
                        currentPayment.setAccountNumber(senderAccountNumber);                       
                    }
                    fillStandartPaymentData(currentPayment, tokens, 4);
                    paymentInfoModifier.alterPaymentTypeIfSentFromImmediateIdentificationProvider(currentPayment);
                }
            } else {
                continue;
            }
            currentPayment.setUnit(CURRENCY_UNIT_PLN);

            payments.add(currentPayment);
        }

        return payments;
    }

    private void fillStandartPaymentData(PaymentInfo currentPayment, String[] tokens, int startIndex) {

        if (!StringUtils.isEmpty(tokens[startIndex])) {
            String payerDetails = tokens[startIndex];
            if (payerDetails.contains(PAYER_ADDRESS_TEXT_PREFIX)) {
                payerDetails = payerDetails.substring(PAYER_ADDRESS_TEXT_PREFIX.length(), payerDetails.length()).replaceAll("  ", " ");
            }

            String accountHolderName = BanksUtils.parsePersonNameFromDetails(payerDetails);
            currentPayment.setAccountHolderName(accountHolderName);

            AddressInfo addressInfo = new AddressInfo();
            addressInfo.setLocation6(payerDetails.substring(accountHolderName.length(), payerDetails.length()).trim());
            currentPayment.setAccountHolderAddress(addressInfo);
        }
        startIndex++;
        if (!StringUtils.isEmpty(tokens[startIndex])) {
            currentPayment.setDetails(parseDetails(tokens[startIndex]));
        }
        startIndex++;
        if (!StringUtils.isEmpty(tokens[startIndex])) {
            BigDecimal amount = new BigDecimal(tokens[startIndex].replaceAll(COMMA_SYMBOL, POINT_SYMBOL));
            currentPayment.setAmount(amount.abs());
        }
        startIndex++;
        Preconditions.checkState(CURRENCY_UNIT_PLN.equals(tokens[startIndex]), CURRENCY_MUST_BE_EQUALS_TO_PLN);
        currentPayment.setBankReference(BanksUtils.md5BankReference(currentPayment));
    }

    protected String parseDetails(String detailsUnparsed) {
        String details = detailsUnparsed.replaceAll("  ", " ").trim();
        if (StringUtils.startsWith(details, DETAILS_PREFIX)) {
            details = details.replaceFirst(DETAILS_PREFIX, "").trim();
        }
        return details.trim();
    }

    private String[] splitRow(String row) {

        if (StringUtils.isBlank(row)) {
            return new String[] {};
        }

        LinkedList<String> list = Lists.newLinkedList();
        Iterable<String> cells = Splitter.on("\",\"").split(row);
        for (String cell : cells) {
            cell = StringUtils.removeStart(cell, ",");
            cell = StringUtils.removeEnd(cell, ",");
            cell = StringUtils.removeEnd(cell, CELL_DOUBLE_QUOTES);
            cell = StringUtils.removeStart(cell, CELL_DOUBLE_QUOTES);
            list.add(cell);
        }
        return list.toArray(new String[list.size()]);
    }

    @Override
    public boolean isApplicableFor(String fileName) {
        return startsWithAdequatePrefix(fileName) && endsWithIgnoreCase(fileName, ".csv");
    }

    private boolean startsWithAdequatePrefix(String fileName) {
        return StringUtils.startsWithIgnoreCase(fileName, SHORT_TERM_PRODUCT_STATEMENT_FILE_PREFIX) || StringUtils.startsWithIgnoreCase(fileName, LONG_TERM_PRODUCT_STATEMENT_FILE_PREFIX); 
    }

}
