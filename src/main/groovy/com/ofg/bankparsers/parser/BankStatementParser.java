package com.ofg.bankparsers.parser;

import com.google.common.base.Predicate;
import com.ofg.loans.api.beans.payment.PaymentInfo;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.List;

public interface BankStatementParser {

    List<PaymentInfo> importBankStatement(@CheckForNull InputStream is);

    boolean isApplicableFor(String fileName);

    public class ApplicableForPredicate implements Predicate<BankStatementParser> {

        private final String fileName;

        public ApplicableForPredicate(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public boolean apply(@Nullable BankStatementParser bankStatementParser) {
            return bankStatementParser.isApplicableFor(fileName);
        }

    }
}
