package com.ofg.bank.parser.mt940;

import com.google.common.base.Predicate;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public abstract class Mt940BankStatementParserDelegate extends Mt940BankStatementParser {

    protected abstract boolean isDelegateFor(@CheckForNull String companyBankAccount);

    @Override
    public final boolean isApplicableFor(String fileName) {
        return false;
    }

    public static class IsDelegateForPredicate implements Predicate<Mt940BankStatementParserDelegate> {

        private final String companyBankAccount;

        public IsDelegateForPredicate(String companyBankAccount) {
            this.companyBankAccount = companyBankAccount;
        }

        @Override
        public boolean apply(@Nonnull Mt940BankStatementParserDelegate delegate) {
            return delegate.isDelegateFor(companyBankAccount);
        }

    }

}
