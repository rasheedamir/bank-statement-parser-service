package com.ofg.bankstatement.parser;

public interface BankStatementParserFactory {

    BankStatementParser getParser(String fileName);

}
