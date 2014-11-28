package com.ofg.bankstatement.util;

import com.google.common.base.Predicate;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class InputStreamUtils {
    @SuppressWarnings("unchecked")
    public static List<String> readLines(InputStream is, String encoding) {
        try {
            List<String> lines = IOUtils.readLines(is, encoding);
            return newArrayList(filter(lines, new NotEmptyLinePredicate()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private static final class NotEmptyLinePredicate implements Predicate<String> {
        @Override
        public boolean apply(String input) {
            return input != null && !isEmpty(input.trim());
        }
    }
}
