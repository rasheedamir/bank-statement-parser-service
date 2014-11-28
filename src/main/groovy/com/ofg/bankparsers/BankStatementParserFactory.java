package com.ofg.bankparsers;

public interface BankStatementParserFactory {

    BankStatementParser getParser(String fileName);

}
