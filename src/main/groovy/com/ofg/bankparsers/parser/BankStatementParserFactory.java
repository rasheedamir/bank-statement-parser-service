package com.ofg.bankparsers.parser;

import com.ofg.bankparsers.parser.BankStatementParser;

public interface BankStatementParserFactory {

    BankStatementParser getParser(String fileName);

}
