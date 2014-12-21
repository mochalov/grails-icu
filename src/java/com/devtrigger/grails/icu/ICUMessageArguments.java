package com.devtrigger.grails.icu;

import com.ibm.icu.text.MessageFormat;

/**
 * Describes common behaviour of message arguments, both list- and map-based
 */
public interface ICUMessageArguments {

    ICUMessageArguments transform(Transformation transformation);

    boolean isEmpty();

    String formatWith(MessageFormat messageFormat);

    interface Transformation {
        Object transform(Object item);
    }
}
