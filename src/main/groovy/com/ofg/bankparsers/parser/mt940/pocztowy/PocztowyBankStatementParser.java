package com.ofg.bankparsers.parser.mt940.pocztowy;

import static com.ofg.loans.pl.banks.BanksUtils.*;

import java.math.BigDecimal;
import java.util.List;
import javax.annotation.CheckForNull;

import org.springframework.stereotype.Component;

import com.ofg.loans.api.beans.payment.PaymentInfo;
import com.ofg.loans.pl.banks.mt940.Mt940BankStatementParserDelegate;
import com.ofg.loans.pl.db.MiniCreditProductBankAccountsFixture;

@Component
class PocztowyBankStatementParser extends Mt940BankStatementParserDelegate {

    private static final String EMPTY = "";

    private static final String ACCOUNT_NUMBER_FIELD_PREFIX_31 = "<31";

    private static final String ENCODING_UTF8 = "UTF8";

    @Override
    protected boolean isCheckCurrencyRequired() {
        return false;
    }

    @Override
    protected int fillAmount(PaymentInfo payment, String line, int parsingIndex) {
        String amountValue = line.substring(parsingIndex, line.length());
        payment.setAmount(new BigDecimal(amountValue.replace(",", ".")));
        return line.length();
    }

    @Override
    protected void fillOperation(PaymentInfo payment, String line, int parsingIndex) {
        // No operation data
    }

    @Override
    protected String prepareCompanyBankAccount(String companyBankAccount) {
        return ((companyBankAccount != null) && (companyBankAccount.length() > 1)) ? BANK_ACCOUNT_PREFIX_PL + companyBankAccount : EMPTY;
    }

    @Override
    protected String readAccountNumber(List<String> lines) {
        int payerAccountNumberLine = indexOfFirstLineWithPrefix(lines, ACCOUNT_NUMBER_FIELD_PREFIX_31);
        String accountNumber = null;
        if (payerAccountNumberLine > 0) {
            accountNumber = getLineIfExists(ACCOUNT_NUMBER_FIELD_PREFIX_31, lines, payerAccountNumberLine).trim();
        }
        return ((accountNumber != null) && !accountNumber.matches("^[A-I].*$")) ? BANK_ACCOUNT_PREFIX_PL + accountNumber : null;
    }

    @Override
    protected String getImportFileEncoding() {
        return ENCODING_UTF8;
    }

    @Override
    protected boolean isDelegateFor(@CheckForNull String companyBankAccount) {
        if (companyBankAccount == null) {
            return false;
        }
        return companyBankAccount.equals(MiniCreditProductBankAccountsFixture.POCZTOWY_ACCOUNT.substring(2));
    }
}
