package com.ofg.bankstatement.util;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.ofg.loans.api.beans.payment.PaymentInfo;
import com.ofg.loans.domain.Iban;
import com.ofg.loans.domain.model.client.ClientAddress;
import com.ofg.loans.domain.util.MD5Utils;
import com.ofg.loans.util.date.DateTimeUtils;
import com.ofg.loans.util.numeric.BigDecimalUtils;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BanksUtils {

    private static final String ONE_WHITE_SPACE = " ";

    private static final String TWO_WHITE_SPACES = "  ";

    private static final String COUNTRY_CODE = "PL";

    private static final String QUOTATION_MARK = "\"";

    private static final String ADDRESS_PREFIX_OS = "OS.";

    private static final String ADDRESS_PREFIX_WS = "WS.";

    private static final String ADDRESS_PREFIX_UL = "UL.";

    private static final String MR_TITLE = "PAN";

    private static final String MRS_TITLE = "PANI";

    public static String addValue(String val, String fieldSeparator, boolean addQotationMarks) {
        String additionalSymbol = addQotationMarks ? QUOTATION_MARK : "";
        return additionalSymbol + val + additionalSymbol + fieldSeparator;
    }

    public static String getBankSortCode(String accountNumber) {
        Iban iban = Iban.split(addCountryCodeToBankAccount(accountNumber));
        return iban.getBankCode();
    }

    public static String convertCurrencySumToCents(BigDecimal amount) {
        amount = amount.multiply(BigDecimalUtils.amount(100));
        Integer intAmount = amount.intValueExact();
        return intAmount.toString();
    }
    
    public static BigDecimal convertCentsToCurrencyAmount(String cents) {
        BigDecimal amount =  BigDecimalUtils.amount(cents);
        amount = amount.divide(BigDecimalUtils.amount(100));
        return amount;
    }

    public static String buildSubFieldBlock(List<String> subFields, String lineSeparator, int subFieldCount) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < subFieldCount; i++) {
            String string = subFields.size() > i ? subFields.get(i) : "";
            if (i > 0) {
                line.append(lineSeparator);
            }
            line.append(string);
        }
        return line.toString();
    }

    public static String removeSplitSymbols(String text, String splitSymbol, String replaceSymbol) {
        return text.replace(splitSymbol, replaceSymbol).trim();
    }

    public static String addCountryCodeToBankAccount(String bankAccount) {
        if (!StringUtils.isEmpty(bankAccount) && !COUNTRY_CODE.equals(bankAccount.substring(0, 2)) && bankAccount.length() != 28) {
            bankAccount = COUNTRY_CODE + bankAccount;
        }
        return bankAccount;
    }
    
    public static String removeCountryCodeFromBankAccount(String bankAccount) {
        if (COUNTRY_CODE.equals(bankAccount.substring(0, 2))) {
            bankAccount = bankAccount.substring(2);
        }
        return bankAccount;
    }

    public static int indexOfFirstLetter(String string, int startFromIndex) {
        return indexOfFirstOccurrence(string, startFromIndex, false, true);
    }

    public static int indexOfFirstOccurrence(String string, int startFromIndex, boolean findDigitChar, boolean findLetterChar) {
        if (string != null && startFromIndex > -1) {
            char[] chars = string.toCharArray();
            for (int i = startFromIndex; i < chars.length; i++) {
                if (findDigitChar && Character.isDigit(chars[i])) {
                    return i;
                } else if (findLetterChar && Character.isLetter(chars[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int indexOfSecondWhiteSpace(String payerDetails) {
        if (StringUtils.isNotEmpty(payerDetails)) {
            int indexOfFirstWhitespace = payerDetails.indexOf(' ');
            return payerDetails.indexOf(' ', indexOfFirstWhitespace + 1);
        } else {
            return 0;
        }
    }

    public static String parseFieldValue(String fieldPrefix, String source) {
        return StringUtils.removeStart(source, fieldPrefix);
    }

    public static String getLineIfExists(String linePrefix, List<String> lines, int currentIndex) {
        return lines != null && StringUtils.isNotEmpty(linePrefix) && currentIndex > -1 && currentIndex < lines.size() && lines.get(currentIndex).startsWith(linePrefix) ? parseFieldValue(
                linePrefix, lines.get(currentIndex)) : StringUtils.EMPTY;
    }

    public static int indexOfFirstLineWithPrefix(List<String> lines, String prefix) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(prefix)) {
                return i;
            }
        }

        return -1;
    }

    public static String joinFieldValuesFromTo(List<String> lines, String fieldPrefix, int prefixStartingLineIndex, int indexFrom, int linesCount) {
        StringBuilder details = new StringBuilder();

        int currentDetailLineIndex = prefixStartingLineIndex;
        for (int i = indexFrom; i < indexFrom + linesCount; i++) {
            String line = lines.get(i);
            String fieldName = fieldPrefix + currentDetailLineIndex;
            if (!line.startsWith(fieldName)) {
                break;
            }
            details.append(parseFieldValue(fieldName, line));
            currentDetailLineIndex++;
        }

        return details.toString();
    }

    public static String md5BankReference(PaymentInfo paymentInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(DateTimeUtils.toDateString(paymentInfo.getBookingDate()));
        sb.append(paymentInfo.getAmount().toString());
        String accountNumber = paymentInfo.getAccountNumber();
        if (StringUtils.isNotBlank(accountNumber)) {
            sb.append(accountNumber);
        }
        String companyBankAccount = paymentInfo.getCompanyBankAccount();
        if (StringUtils.isNotBlank(companyBankAccount)) {
            sb.append(companyBankAccount);
        }
        String details = paymentInfo.getDetails();
        if (StringUtils.isNotBlank(details)) {
            sb.append(details.toUpperCase().replaceAll("\\s", ""));
        }
        return MD5Utils.generateMD5(sb.toString());
    }

    public static boolean nextTwoCharsIsDigits(int index, String line) {
        return found(index) && line.length() >= index + 3 && Character.isDigit(line.charAt(index + 1)) && Character.isDigit(line.charAt(index + 2));
    }

    public static List<String> extractOneSubFieldInOneLine(List<String> lines, String subFieldPrefixSymbol) {
        List<String> oneFieldInOneRowLines = new ArrayList<String>();
        for (String line : lines) {
            int indexOfSubField = line.indexOf(subFieldPrefixSymbol, 1);
            boolean nextTwoSymbolsIsDigits = nextTwoCharsIsDigits(indexOfSubField, line);

            if (indexOfSubField != -1 && nextTwoSymbolsIsDigits) {
                String firstField = line.substring(0, indexOfSubField);
                oneFieldInOneRowLines.add(firstField);
                String secondField = line.substring(indexOfSubField, line.length());
                oneFieldInOneRowLines.add(secondField);
            } else {
                oneFieldInOneRowLines.add(line);
            }

        }
        return oneFieldInOneRowLines;
    }

    public static String getFieldValue(List<String> lines, String fieldName) {
        int index = indexOfFirstLineWithPrefix(lines, fieldName);
        if (index > -1) {
            return getLineIfExists(fieldName, lines, index);
        } else {
            return StringUtils.EMPTY;
        }
    }

    public static String getMultilineFieldValue(
            List<String> lines, String field, String nextField) {

        Preconditions.checkArgument(field != null, "field cannot be null");

        String joinedLines = Joiner.on("").join(lines);
        int begin = joinedLines.indexOf(field);
        if (begin < 0) {
            return StringUtils.EMPTY;
        }
        begin += field.length();

        int end = joinedLines.length();
        if (nextField != null) {
            int pos = joinedLines.indexOf(nextField, begin);
            if (pos >= 0) {
                end = pos;
            }
        }

        return joinedLines.substring(begin, end);
    }

    public static int indexOfText(String text, List<String> searchStrings) {
        if (searchStrings == null) {
            return -1;
        }
        for (String searchString : searchStrings) {
            int indexIgnoreCase = StringUtils.indexOfIgnoreCase(text, searchString);
            if (indexIgnoreCase > -1) {
                return indexIgnoreCase;
            }
        }
        return -1;
    }

    public static String parsePersonNameFromDetails(String details) {
        if (details == null) {
            return "";
        }

        if (potentiallyHasTitle(details)) {
            details = tryDropTitle(details);
        }

        int indexOfSecondWhiteSpace = BanksUtils.indexOfSecondWhiteSpace(details);
        int indexOfSpecAddressSymbols = BanksUtils.indexOfText(details, Arrays.asList(ADDRESS_PREFIX_UL, ADDRESS_PREFIX_WS, ADDRESS_PREFIX_OS));
        int indexOfTwoWhiteSpaces = details.indexOf(TWO_WHITE_SPACES);
        int indexOfDigitSymbol = indexOfFirstOccurrence(details, 0, true, false);

        int indexOfAddress = -1;
        if (indexOfDigitSymbol == -1 && indexOfSpecAddressSymbols == -1 && indexOfTwoWhiteSpaces == -1 && found(indexOfSecondWhiteSpace)) {
            indexOfAddress = indexOfSecondWhiteSpace;
        }

        indexOfDigitSymbol = indexOfDigitSymbol == -1 ? details.length() : indexOfDigitSymbol;
        indexOfSpecAddressSymbols = indexOfSpecAddressSymbols == -1 ? details.length() : indexOfSpecAddressSymbols;
        indexOfTwoWhiteSpaces = indexOfTwoWhiteSpaces == -1 ? details.length() : indexOfTwoWhiteSpaces;

        if (indexOfSpecAddressSymbols < indexOfTwoWhiteSpaces && indexOfSpecAddressSymbols < indexOfDigitSymbol) {
            indexOfAddress = indexOfSpecAddressSymbols;
        } else if (indexOfTwoWhiteSpaces < indexOfSpecAddressSymbols && indexOfTwoWhiteSpaces < indexOfDigitSymbol) {
            indexOfAddress = indexOfTwoWhiteSpaces;
        } else if (indexOfDigitSymbol < indexOfSpecAddressSymbols && indexOfDigitSymbol < indexOfTwoWhiteSpaces) {
            indexOfAddress = indexOfSecondWhiteSpace;
        }

        return details.substring(0, indexOfAddress == -1 ? details.length() : indexOfAddress).trim();
    }

    private static boolean potentiallyHasTitle(String text) {
        return text.toUpperCase().startsWith(MR_TITLE);
    }

    private static String tryDropTitle(String details) {
        int indexOfFirstSpace = details.indexOf(ONE_WHITE_SPACE);
        if (found(indexOfFirstSpace)) {
            String potentialTitle = extractPotentialTitle(details, indexOfFirstSpace);
            if (isTitle(potentialTitle)) {
                details = details.substring(indexOfFirstSpace).trim();
            }
        }
        return details;
    }

    private static String extractPotentialTitle(String details, int endIndex) {
        return details.substring(0, endIndex).trim();
    }

    private static boolean found(int index) {
        return index != -1;
    }

    private static boolean isTitle(String potentialTitle) {
        return MR_TITLE.equalsIgnoreCase(potentialTitle) || MRS_TITLE.equalsIgnoreCase(potentialTitle);
    }

    private static final String ADDRESS_FIELD_SEPARATOR = " ";

    public static String addressToSingleLine(ClientAddress address) {
        StringBuilder sb = new StringBuilder();
        appendToAddress(sb, address.getLocation1(), ADDRESS_FIELD_SEPARATOR);
        appendToAddress(sb, address.getLocation2(), ADDRESS_FIELD_SEPARATOR);
        appendToAddress(sb, address.getLocation3(), ADDRESS_FIELD_SEPARATOR);
        appendToAddress(sb, address.getLocation4(), ADDRESS_FIELD_SEPARATOR);
        appendToAddress(sb, address.getLocation5(), ADDRESS_FIELD_SEPARATOR);
        appendToAddress(sb, address.getLocation6(), ADDRESS_FIELD_SEPARATOR);
        appendToAddress(sb, address.getPostalCode(), ADDRESS_FIELD_SEPARATOR);
        return sb.toString();
    }

    private static void appendToAddress(StringBuilder sb, String value, String separator) {
        if (!StringUtils.isBlank(value)) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(value.trim());
        }
    }
}
