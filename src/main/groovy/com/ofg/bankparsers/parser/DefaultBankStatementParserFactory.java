package com.ofg.bankparsers.parser;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Lists.newArrayList;

@Component
class DefaultBankStatementParserFactory implements BankStatementParserFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultBankStatementParserFactory.class);

    @Autowired(required = false)
    public Collection<BankStatementParser> bankStatementParsers = newArrayList();

    @Override
    public BankStatementParser getParser(String fileName) {

        log.debug("Looking up bank statement parser for file name {}", fileName);

        Iterable<BankStatementParser> iterable = filter(bankStatementParsers, new BankStatementParser.ApplicableForPredicate(fileName));
        if (isEmpty(iterable)) {
            throw new RuntimeException("No bank statement parser found. Please check bank statement file name and file extension");
        }
        return getOnlyElement(iterable);

    }

    @VisibleForTesting
    void addBankStatementParser(BankStatementParser bankStatementParser) {
        bankStatementParsers.add(bankStatementParser);
    }

}
