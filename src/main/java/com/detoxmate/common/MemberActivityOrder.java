package com.detoxmate.common;

import java.text.Collator;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

public final class MemberActivityOrder {

    private MemberActivityOrder() {
    }

    public static <T> Comparator<T> latestCertifiedThenDisplayName(
            Function<T, LocalDateTime> certifiedAt,
            Function<T, String> displayName
    ) {
        return (first, second) -> compare(
                certifiedAt.apply(first),
                displayName.apply(first),
                certifiedAt.apply(second),
                displayName.apply(second)
        );
    }

    private static int compare(
            LocalDateTime firstCertifiedAt,
            String firstDisplayName,
            LocalDateTime secondCertifiedAt,
            String secondDisplayName
    ) {
        boolean firstCertified = firstCertifiedAt != null;
        boolean secondCertified = secondCertifiedAt != null;

        if (firstCertified != secondCertified) {
            return firstCertified ? -1 : 1;
        }

        if (firstCertified) {
            int certifiedAtOrder = secondCertifiedAt.compareTo(firstCertifiedAt);
            if (certifiedAtOrder != 0) {
                return certifiedAtOrder;
            }
        }

        return compareDisplayName(firstDisplayName, secondDisplayName);
    }

    private static int compareDisplayName(String first, String second) {
        if (first == null && second == null) {
            return 0;
        }
        if (first == null) {
            return 1;
        }
        if (second == null) {
            return -1;
        }

        return Collator.getInstance(Locale.KOREAN).compare(first, second);
    }
}
