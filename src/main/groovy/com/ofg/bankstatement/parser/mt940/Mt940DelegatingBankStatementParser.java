package com.ofg.bankstatement.parser.mt940;

import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Lists.*;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.ofg.loans.api.beans.payment.PaymentInfo;
import com.ofg.loans.domain.util.InputStreamUtils;
import com.ofg.loans.pl.banks.BankStatementByteStore;

@Component
class Mt940DelegatingBankStatementParser extends Mt940BankStatementParser {

    private static final String ENCODING_WINDOWS_1250 = "Cp1250";

    @Autowired(required = false)
    private Collection<Mt940BankStatementParserDelegate> delegates = newArrayList();

    @Override
    public List<PaymentInfo> importBankStatement(InputStream inputStream) {

        BankStatementByteStore bankStatementByteStore = storeBankStatement(inputStream);

        List<Mt940Payment> mt940Payments = prepareMt940Payments(readLines(bankStatementByteStore));

        Preconditions.checkState(!mt940Payments.isEmpty(), "No Mt940 payments found");

        String companyBankAccount = getCompanyBankAccount(mt940Payments);

        Mt940BankStatementParserDelegate applicableDelegate = findApplicableDelegate(companyBankAccount);
        return applicableDelegate.importBankStatement(bankStatementByteStore.newInputStream());

    }

    private BankStatementByteStore storeBankStatement(InputStream inputStream) {
        return new BankStatementByteStore(inputStream);
    }

    private List<String> readLines(BankStatementByteStore bankStatementByteStore) {
        return InputStreamUtils.readLines(bankStatementByteStore.newInputStream(), ENCODING_WINDOWS_1250);
    }

    private String getCompanyBankAccount(List<Mt940Payment> payments) {
        Mt940Payment firstPayment = payments.get(0);
        return Preconditions.checkNotNull(firstPayment.getCompanyBankAccount(), "imported payment statement file not contain bank account");
    }

    private Mt940BankStatementParserDelegate findApplicableDelegate(String companyBankAccount) {
        return getOnlyElement(filter(delegates, new Mt940BankStatementParserDelegate.IsDelegateForPredicate(companyBankAccount)));
    }

    @Override
    public boolean isApplicableFor(String fileName) {
        return StringUtils.startsWithIgnoreCase(fileName, "mt940") && StringUtils.endsWithIgnoreCase(fileName, ".txt");
    }

    @VisibleForTesting
    void registerDelegate(Mt940BankStatementParserDelegate delegate) {
        delegates.add(delegate);
    }

}
