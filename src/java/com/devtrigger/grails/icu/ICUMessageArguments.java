package com.devtrigger.grails.icu;

import com.ibm.icu.text.MessageFormat;

/**
 * Describes common behaviour of message arguments, both list- and map-based
 */
public interface ICUMessageArguments {

    public ICUMessageArguments transform(Transformation transformation);

    public boolean isEmpty();

    public String formatWith(MessageFormat messageFormat);

    public static interface Transformation {
        public Object transform(Object item);
    }
}
