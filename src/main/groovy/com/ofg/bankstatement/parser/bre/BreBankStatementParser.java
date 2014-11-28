package cv;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ofg.bankstatement.parser.BankStatementParser;
import com.ofg.bankstatement.util.BanksUtils;
import com.ofg.bankstatement.util.InputStreamUtils;
import com.ofg.loans.api.beans.AddressInfo;
import com.ofg.loans.api.beans.payment.PaymentInfo;
import com.ofg.loans.api.beans.payment.PaymentType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class BreBankStatementParser implements BankStatementParser {

    private static final Logger log = LoggerFactory.getLogger(BreBankStatementParser.class.getName());
    
    private static final String ENCODING_CP852 = "Cp852";

    private static final String BOOKING_DATE_FORMAT = "dd/MM/yy";

    private static final String AMOUNT_FIELD_START_PREFIX_MINUS = "-";

    private static final String BANK_REFERENCE_FIELD_START_PREFIX_TNR = "TNR: ";

    private final static int BANK_REFERENCE_LENGTH = 22;

    private static final String ACCOUNT_NUMBER_FIELD_START_PREFIX_Z_RACH = "z rach.: ";

    private static final String ACCOUNT_NUMBER_FIELD_START_PREFIX_NA_RACH = "na rach.: ";

    private static final String DETAILS_START_FIELD_PREFIX_TYT = "tyt.: ";

    private static final String ACCOUNT_HOLDER_DATA_START_FIELD_PREFIX_OD = "od: ";

    private static final String ACCOUNT_HOLDER_DATA_START_FIELD_PREFIX_DLA = "dla: ";

    private static final String BANK_ACCOUNT_PREFIX_PL = "PL";

    private static final String EMPTY = "";

    private static final String SEMICOLOMN = ";";

    @Override
    public List<PaymentInfo> importBankStatement(InputStream is) {
        List<String> rows = InputStreamUtils.readLines(is, getImportFileEncoding());
        return getPaymentsInfo(rows);
    }

    private List<PaymentInfo> getPaymentsInfo(List<String> rows) {
        List<PaymentInfo> payments = new ArrayList<PaymentInfo>();

        for (String row : rows) {
            PaymentInfo payment = new PaymentInfo();

            List<String> lines = splitRow(row);

            payment.setAccountNumber(getAccountNumber(lines.get(5)));
            payment.setDetails(getDetails(lines.get(5)));

            String accountHolderData = getAccountHolderData(lines.get(5));
            String accountHolderName = BanksUtils.parsePersonNameFromDetails(accountHolderData);
            AddressInfo addressInfo = new AddressInfo();
            addressInfo.setLocation6(StringUtils.removeStart(accountHolderData, accountHolderName).trim());

            payment.setAccountHolderAddress(addressInfo);
            payment.setAccountHolderName(accountHolderName);

            payment.setCompanyBankAccount(createPlAccountNumber(lines.get(2)));
            payment.setAmount(getAmount(lines.get(3)));
            payment.setUnit(lines.get(4));

            payment.setBookingDate(formatBookingDate(lines.get(1)));

            payment.setBankReference(getBankReference(lines.get(5)));
            payment.setType(getPaymentType(lines.get(3)));

            payments.add(payment);
        }
        return payments;
    }

    private String getDetails(String line) {
        if (getAccountNumber(line) != null) {
            int startIndex = line.indexOf(DETAILS_START_FIELD_PREFIX_TYT) + DETAILS_START_FIELD_PREFIX_TYT.length();
            return (line.indexOf(DETAILS_START_FIELD_PREFIX_TYT) > 0) ? line.substring(startIndex, line.indexOf(SEMICOLOMN, startIndex)) : EMPTY;
        }
        return EMPTY;
    }

    private String getAccountHolderData(String line) {
        if (getAccountNumber(line) != null) {
            int startIndex =
                    !line.contains(ACCOUNT_HOLDER_DATA_START_FIELD_PREFIX_OD) ? (line.indexOf(ACCOUNT_HOLDER_DATA_START_FIELD_PREFIX_DLA) + ACCOUNT_HOLDER_DATA_START_FIELD_PREFIX_DLA
                            .length()) : (line.indexOf(ACCOUNT_HOLDER_DATA_START_FIELD_PREFIX_OD) + ACCOUNT_HOLDER_DATA_START_FIELD_PREFIX_OD.length());

            return (line.indexOf(ACCOUNT_HOLDER_DATA_START_FIELD_PREFIX_OD) > 0 || line.indexOf(ACCOUNT_HOLDER_DATA_START_FIELD_PREFIX_DLA) > 0) ? line.substring(startIndex,
                    line.indexOf(SEMICOLOMN, startIndex)) : EMPTY;
        }
        return EMPTY;
    }

    private String getAccountNumber(String line) {
        int startIndex =
                !line.contains(ACCOUNT_NUMBER_FIELD_START_PREFIX_Z_RACH) ? (line.indexOf(ACCOUNT_NUMBER_FIELD_START_PREFIX_NA_RACH) + ACCOUNT_NUMBER_FIELD_START_PREFIX_NA_RACH
                        .length()) : (line.indexOf(ACCOUNT_NUMBER_FIELD_START_PREFIX_Z_RACH) + ACCOUNT_NUMBER_FIELD_START_PREFIX_Z_RACH.length());

        return (line.indexOf(ACCOUNT_NUMBER_FIELD_START_PREFIX_Z_RACH) > 0 || line.indexOf(ACCOUNT_NUMBER_FIELD_START_PREFIX_NA_RACH) > 0) ? BANK_ACCOUNT_PREFIX_PL
                + line.substring(startIndex, line.indexOf(SEMICOLOMN, startIndex)) : null;
    }

    private String getBankReference(String line) {
        int index = StringUtils.indexOf(line, BANK_REFERENCE_FIELD_START_PREFIX_TNR);
        int bankRefStartIndex = index + BANK_REFERENCE_FIELD_START_PREFIX_TNR.length();

        return line.substring(bankRefStartIndex, bankRefStartIndex + BANK_REFERENCE_LENGTH);
    }

    private String createPlAccountNumber(String accountNumber) {
        return ((accountNumber != null) && (accountNumber.length() > 1)) ? BANK_ACCOUNT_PREFIX_PL + accountNumber : EMPTY;
    }

    private PaymentType getPaymentType(String amountValue) {
        if (amountValue.startsWith(AMOUNT_FIELD_START_PREFIX_MINUS)) {
            return PaymentType.OUTGOING;
        }
        return PaymentType.INCOMING;
    }

    private BigDecimal getAmount(String amountValue) {
        if (amountValue.startsWith(AMOUNT_FIELD_START_PREFIX_MINUS)) {
            return new BigDecimal(amountValue.substring(1).replace(",", "."));
        }
        return new BigDecimal(amountValue.replace(",", "."));
    }

    private Date formatBookingDate(String unformattedDate) {
        DateFormat formatter = new SimpleDateFormat(BOOKING_DATE_FORMAT);
        Date date = null;
        try {
            date = formatter.parse(unformattedDate);
        } catch (ParseException e) {
            log.warn("Error formating booking date", e);
        }
        return date;
    }

    private List<String> splitRow(String row) {
        if (StringUtils.isBlank(row)) {
            return Lists.newArrayList();
        }

        List<String> lines = Lists.newArrayList();
        Iterable<String> cells = Splitter.on("|").split(row);

        for (String cell : cells) {
            cell = StringUtils.trim(cell);
            lines.add(cell);
        }
        return lines;
    }

    private String getImportFileEncoding() {
        return ENCODING_CP852;
    }

    @Override
    public boolean isApplicableFor(String fileName) {
        return StringUtils.startsWithIgnoreCase(fileName, "historia") && StringUtils.endsWithIgnoreCase(fileName, ".dat");
    }
}
