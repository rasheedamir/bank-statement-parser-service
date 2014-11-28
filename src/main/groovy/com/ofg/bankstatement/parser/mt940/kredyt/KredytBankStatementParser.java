package com.ofg.bankstatement.parser.mt940.kredyt;

import static org.apache.commons.lang.StringUtils.endsWith;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.startsWith;

import java.math.BigDecimal;
import java.util.List;

import com.ofg.bankstatement.parser.mt940.Mt940BankStatementParser;
import com.ofg.bankstatement.parser.mt940.Mt940TransactionType;
import com.ofg.bankstatement.util.BanksUtils;
import org.springframework.stereotype.Component;

import com.ofg.loans.api.beans.payment.PaymentInfo;
import com.ofg.loans.api.beans.payment.PaymentType;

@Component
class KredytBankStatementParser extends Mt940BankStatementParser {

    private static final String TITLE_SUB_FIELD = ">20";
    private static final String ACCOUNT_NAME_SUB_FIELD = ">31";
    private static final String NAME_AND_ADDRESS_SUB_FIELD = ">32";
    private static final String HEADER_LAST_FIELD_60M = ":60M:";

    @Override
    protected int fillPaymentType(PaymentInfo payment, String line, int parsingIndex) {
        // line example: :61:141025CN525,00B130NONREF
        // Debit/Credit mark – ‘D’ Debit, ‘C’ Credit
        int length = 1;
        String debitCreditMark = line.substring(parsingIndex, parsingIndex + length);
        PaymentType paymentType = PAYMENT_TYPE_CREDIT.equals(debitCreditMark)
                ? PaymentType.INCOMING : PaymentType.OUTGOING;
        payment.setType(paymentType);
        return parsingIndex + length;
    }

    @Override
    protected void fillOperation(PaymentInfo payment, String line, int parsingIndex) {
        // line example: :61:141025CN525,00B130NONREF
        int length = 4;
        String businessCode = line.substring(parsingIndex, parsingIndex + length);
        Mt940TransactionType type = BZWBKBusinessCodeUtil.toMt940TransactionType(businessCode);
        if (type != null) {
            payment.setBankOperationType(type.name());
            payment.setBankOperationName(type.getDescription());
        }
    }

    @Override
    protected int fillAmount(PaymentInfo payment, String line, int parsingIndex) {
        // Possible values of line from parsingIndex:
        // 48,20B130NONREF
        // 80,0056C1NONREF <- Tricky case. 56 is not a part of amount

        int endOfAmount = line.indexOf(',', parsingIndex) + 2;
        String amountValue = line.substring(parsingIndex, endOfAmount + 1);
        BigDecimal amount = new BigDecimal(amountValue.replace(",", "."));
        payment.setAmount(amount);
        return endOfAmount + 1;
    }

    @Override
    protected boolean isHeaderLastField(String line) {
        return line.startsWith(getHeaderLastFieldPrefix()) || line.startsWith(getHeaderLastFieldPrefix2());
    }

    private String getHeaderLastFieldPrefix2() {
        return HEADER_LAST_FIELD_60M;
    }

    @Override
    protected String readDetailsFields(List<String> lines) {
        return BanksUtils.getMultilineFieldValue(lines, TITLE_SUB_FIELD, ACCOUNT_NAME_SUB_FIELD);
    }

    @Override
    protected String readAccountNumber(List<String> lines) {
        String accountNumber = BanksUtils.getMultilineFieldValue(lines, ACCOUNT_NAME_SUB_FIELD,
                NAME_AND_ADDRESS_SUB_FIELD);
        if (isNotBlank(accountNumber) && !startsWith(accountNumber, BANK_ACCOUNT_PREFIX_PL)) {
            accountNumber = BANK_ACCOUNT_PREFIX_PL + accountNumber;
        }
        return accountNumber;
    }

    @Override
    protected String parsePayerDetails(List<String> lines) {
        return BanksUtils.getMultilineFieldValue(lines, NAME_AND_ADDRESS_SUB_FIELD, null);
    }

    @Override
    public boolean isApplicableFor(String fileName) {
        // Quotation from BZWBK documentation:
        // Nazwa pliku z ekstraktem z historii rachunku formatowana jest w następujący sposób:
        // RACHWALUTA-DATA.MT np.: 60005341PLN-20040311.MT przy czym:
        // RACH − pełny numer rachunku IBAN, dla którego generowany jest eksport;
        // WALUTA − rachunku jw.;
        // DATA w formacie: RRRRMMDD;
        // MT − rozszerzenie pliku identyfikujące typ jako ekstrakt z historii rachunku w formacie MT940
        String accountNumberSuffix = KREDYT_ACCOUNT.substring(20);
        return startsWith(fileName, accountNumberSuffix) && endsWith(fileName, ".MT");
    }
}
