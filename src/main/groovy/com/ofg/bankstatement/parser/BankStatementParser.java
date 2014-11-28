package com.ofg.bankstatement.parser;

import com.google.common.base.Predicate;
import com.ofg.loans.api.beans.payment.PaymentInfo;

import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.List;

public interface BankStatementParser {

    List<PaymentInfo> importBankStatement(@NotNull InputStream is);

    boolean isApplicableFor(String fileName);

    public static Predicate<BankStatementParser> applicableFor(String fileName) {
        return p -> p.isApplicableFor(fileName);
    }

}
