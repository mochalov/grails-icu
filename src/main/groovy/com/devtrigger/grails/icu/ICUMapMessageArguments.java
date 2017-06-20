package com.devtrigger.grails.icu;

import com.ibm.icu.text.MessageFormat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Map-based arguments implementation. Used with patterns which have named arguments
 */
public class ICUMapMessageArguments implements ICUMessageArguments {

    Map<String, Object> args;

    public ICUMapMessageArguments(Map<String, Object> args) {
        if (args == null) args = Collections.emptyMap();
        this.args = args;
    }

    @Override
    public boolean isEmpty() {
        return args.isEmpty();
    }

    @Override
    public ICUMapMessageArguments transform(Transformation transformation) {
        Map<String, Object> newArgs = new LinkedHashMap<String, Object>(args.size());
        for (Map.Entry<String, Object> item: args.entrySet())
            newArgs.put(item.getKey(), transformation.transform(item.getValue()));
        return new ICUMapMessageArguments(newArgs);
    }

    @Override
    public String formatWith(MessageFormat messageFormat) {
        return messageFormat.format(args);
    }
}
