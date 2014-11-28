package com.ofg.bank.parser;

public interface BankStatementParserFactory {

    BankStatementParser getParser(String fileName);

}
