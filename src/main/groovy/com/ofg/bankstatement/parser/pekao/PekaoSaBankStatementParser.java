package com.ofg.bankstatement.parser.pekao;


import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Preconditions;
import com.ofg.bankstatement.parser.BankStatementParser;
import com.ofg.loans.api.beans.AddressInfo;
import com.ofg.loans.api.beans.payment.PaymentInfo;
import com.ofg.loans.api.beans.payment.PaymentType;
import com.ofg.loans.domain.services.BankStatementParser;
import com.ofg.loans.pl.banks.BanksUtils;
import com.ofg.loans.util.date.DateTimeUtils;
import com.ofg.loans.util.numeric.BigDecimalUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static au.com.bytecode.opencsv.CSVParser.DEFAULT_QUOTE_CHARACTER;

@Component
public class PekaoSaBankStatementParser implements BankStatementParser {

    private static final Logger log = LoggerFactory.getLogger(PekaoSaBankStatementParser.class.getName());

    private static final String POINT_SYMBOL = ".";

    private static final String COMMA_SYMBOL = ",";

    private static final char FIELD_SEPARATOR_SYMBOL = ';';

    private static final String ENCODING = "ISO-8859-2";

    private static final String ACCOUNT_NUMBER_COUNTRY_PREFIX = "PL";

    private static final String CURRENCY_UNIT_PLN = "PLN";

    private static final String BOOKING_DATE_FORMAT = "yyyy-MM-dd";

    @Override
    public List<PaymentInfo> importBankStatement(InputStream is) {
        Preconditions.checkNotNull(is, "can't import bank statement from null stream");
        List<PaymentInfo> paymentInfoList = new ArrayList<PaymentInfo>();

        CSVReader reader = null;
        try {
            reader = new CSVReader(new InputStreamReader(is, ENCODING), FIELD_SEPARATOR_SYMBOL, DEFAULT_QUOTE_CHARACTER, '\0');

            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {

                final PaymentInfo paymentInfo = buildPaymentInfo(nextLine);
                paymentInfoList.add(paymentInfo);
            }

        } catch (IOException e) {
            log.warn("Exception during importing PEKAO SA bank statement.", e);
            throw new IllegalStateException(e);
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // do nothing
                    log.error("Exception occurred while trying to close the reader", e);
                }
            }
        }

        return paymentInfoList;
    }

    private PaymentInfo buildPaymentInfo(String[] tokens) {
        Assert.notNull(tokens);

        PaymentInfo paymentInfo = new PaymentInfo();

        Date bookingDate = DateTimeUtils.date(tokens[2], BOOKING_DATE_FORMAT);
        paymentInfo.setBookingDate(bookingDate);

        BigDecimal amount = new BigDecimal(tokens[3].replaceAll(COMMA_SYMBOL, POINT_SYMBOL));
        paymentInfo.setAmount(amount.abs());
        paymentInfo.setType(BigDecimalUtils.isNegativeAmount(amount) ? PaymentType.OUTGOING : PaymentType.INCOMING);

        paymentInfo.setUnit(CURRENCY_UNIT_PLN);

        if (StringUtils.isNotEmpty(tokens[5])) {
            paymentInfo.setAccountNumber(ACCOUNT_NUMBER_COUNTRY_PREFIX + tokens[5]);
        }

        if (StringUtils.isNotEmpty(tokens[4])) {
            String payerDetails = (tokens[4] + tokens[0]).replaceAll("  ", " ");

            String accountHolderName = BanksUtils.parsePersonNameFromDetails(payerDetails);
            paymentInfo.setAccountHolderName(accountHolderName);

            AddressInfo addressInfo = new AddressInfo();
            addressInfo.setLocation6(payerDetails.substring(accountHolderName.length(), payerDetails.length()).trim());
            paymentInfo.setAccountHolderAddress(addressInfo);
        }
        if (StringUtils.isNotEmpty(tokens[7])) {
            paymentInfo.setCompanyBankAccount(ACCOUNT_NUMBER_COUNTRY_PREFIX + tokens[7]);
        }

        if (StringUtils.isNotEmpty(tokens[11])) {
            paymentInfo.setDetails(tokens[11].replaceAll("  ", " ").trim());
        }

        paymentInfo.setBankReference(prepareBankReference(paymentInfo));

        return paymentInfo;
    }

    protected String prepareBankReference(PaymentInfo payment) {
        return BanksUtils.md5BankReference(payment);
    }

    @Override
    public boolean isApplicableFor(String fileName) {
        return StringUtils.startsWithIgnoreCase(fileName, "export20") && StringUtils.endsWithIgnoreCase(fileName, ".csv");
    }
}