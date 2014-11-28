package com.ofg.bankparsers.parser.getin;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import com.ofg.loans.api.beans.AddressInfo;
import com.ofg.loans.api.beans.payment.PaymentInfo;
import com.ofg.loans.api.beans.payment.PaymentType;
import com.ofg.loans.domain.services.BankStatementParser;
import com.ofg.loans.infrastructure.jaxb.JaxbUnmarshaller;
import com.ofg.loans.pl.banks.BanksUtils;
import com.ofg.loans.pl.banks.ImmediateIdentificationPaymentInfoModifier;
import com.ofg.loans.pl.banks.getin.statement.month.generated.Wychagi;
import com.ofg.loans.pl.banks.getin.statement.month.generated.Wyciag;

@Component
public class GetinBankMonthStatementParser implements BankStatementParser {

    private static final String BANK_ACCOUNT_PREFIX_PL = "PL";

    private static final String PAYMENT_TYPE_CREDIT = "C";

    private static final String PAYMENT_TYPE_REVERSE_DEBIT = "RD";

    private static final String EMPTY_STRING = "";

    private static final JaxbUnmarshaller unmarshaller = new JaxbUnmarshaller(Wychagi.class.getPackage().getName());

    private static final ImmediateIdentificationPaymentInfoModifier paymentInfoModifier = new ImmediateIdentificationPaymentInfoModifier();
    
    @Override
    public List<PaymentInfo> importBankStatement(InputStream getinBankData) {
        Wychagi getinUnmarshalledBankPayments = null;
        try {
            getinUnmarshalledBankPayments = unmarshaller.unmarshall(IOUtils.toString(getinBankData, "ISO8859_2"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Wychagi getinBankPayments = getinUnmarshalledBankPayments;
        List<PaymentInfo> payments = new ArrayList<PaymentInfo>();

        if (getinBankPayments.getWYCIAG() != null) {
            List<Wyciag.OPERACJA> paymentOperations = getinBankPayments.getWYCIAG().getOPERACJA();

            for (Wyciag.OPERACJA operation : paymentOperations) {
                PaymentInfo payment = new PaymentInfo();

                payment.setDetails(getPaymentDetails(putIfNotNull(operation.getTRESC1()), putIfNotNull(operation.getTRESC2()), putIfNotNull(operation.getTRESC3()),
                        putIfNotNull(operation.getTRESC4())));
                payment.setCompanyBankAccount(createPlAccountNumber(getinBankPayments.getRachunek()));
                payment.setAccountHolderAddress(getAccountHolderAddress(putIfNotNull(operation.getNAZWA1()), putIfNotNull(operation.getNAZWA2()),
                        putIfNotNull(operation.getNAZWA3()), putIfNotNull(operation.getNAZWA4())));
                payment.setAccountHolderName(getAccountHolderName(putIfNotNull(operation.getNAZWA1()), putIfNotNull(operation.getNAZWA2()), putIfNotNull(operation.getNAZWA3()),
                        putIfNotNull(operation.getNAZWA4())));
                payment.setAccountNumber(createPlAccountNumber(operation.getRACHUNEK()));
                payment.setAmount(getAmount(operation.getKWOTA()));
                payment.setBookingDate(formatBookingDate(operation.getDATAWALUTY()));
                payment.setType(getPaymentType(operation.getSTRONA()));
                payment.setUnit(getinBankPayments.getWaluta());
                payment.setBankReference(BanksUtils.md5BankReference(payment));
                paymentInfoModifier.alterPaymentTypeIfSentFromImmediateIdentificationProvider(payment);
                payments.add(payment);
            }
        }
        return payments;
    }

    private String putIfNotNull(String value) {
        return (value == null ? EMPTY_STRING : value);
    }

    private BigDecimal getAmount(String amount) {
        return new BigDecimal(amount.replace(",", ".").replace(" ", ""));
    }

    private String getAccountHolderName(String part1, String part2, String part3, String part4) {
        String accountHolderNameAndAddress = part1 + " " + part2 + " " + part3 + " " + part4;
        return BanksUtils.parsePersonNameFromDetails(accountHolderNameAndAddress.trim());
    }

    private AddressInfo getAccountHolderAddress(String part1, String part2, String part3, String part4) {
        String accountHolderNameAndAddress = part1 + " " + part2 + " " + part3 + " " + part4;

        String accountHolderName = BanksUtils.parsePersonNameFromDetails(accountHolderNameAndAddress.trim());
        String accountHolderAddress = StringUtils.removeStart(accountHolderNameAndAddress, accountHolderName).trim();

        AddressInfo addressInfo = new AddressInfo();
        addressInfo.setLocation6(accountHolderAddress);
        return addressInfo;
    }

    private String getPaymentDetails(String detail1, String detail2, String detail3, String detail4) {
        return (detail1 + " " + detail2 + " " + detail3 + " " + detail4).trim();
    }

    private String createPlAccountNumber(String accountNumber) {
        if (StringUtils.length(accountNumber) < 2) {
            return null;
        } else if (accountNumber.startsWith(BANK_ACCOUNT_PREFIX_PL)) {
            return accountNumber;
        } else {
            return BANK_ACCOUNT_PREFIX_PL + accountNumber;
        }
    }

    private Date formatBookingDate(XMLGregorianCalendar date) {
        return (date != null ? date.toGregorianCalendar().getTime() : null);
    }

    private PaymentType getPaymentType(String type) {
        return (type.startsWith(PAYMENT_TYPE_CREDIT) || type.startsWith(PAYMENT_TYPE_REVERSE_DEBIT)) ? PaymentType.INCOMING : PaymentType.OUTGOING;
    }

    @Override
    public boolean isApplicableFor(String fileName) {
        return (StringUtils.startsWithIgnoreCase(fileName, "statement_20") || StringUtils.startsWithIgnoreCase(fileName, "wyciagi_20"))
                && StringUtils.endsWithIgnoreCase(fileName, ".xml");
    }

}
