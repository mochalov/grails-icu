package com.devtrigger.grails.icu;

import com.ibm.icu.text.MessageFormat;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

import java.util.*;

/**
 * ICU4j MessageFormat aware {@link org.springframework.context.support.AbstractMessageSource} drop-in
 * @see com.ibm.icu.text.MessageFormat
 */
public abstract class ICUAbstractMessageSource extends ICUMessageSourceSupport implements ICUMessageSource {

    private MessageSource parentMessageSource;

    private Properties commonMessages;

    private boolean useCodeAsDefaultMessage = false;


    @Override
    public void setParentMessageSource(MessageSource parent) {
        this.parentMessageSource = parent;
    }

    @Override
    public MessageSource getParentMessageSource() {
        return this.parentMessageSource;
    }

    /**
     * Specify locale-independent common messages, with the message code as key
     * and the full message String (may contain argument placeholders) as value.
     * <p>May also link to an externally defined Properties object, e.g. defined
     * through a {@link org.springframework.beans.factory.config.PropertiesFactoryBean}.
     */
    public void setCommonMessages(Properties commonMessages) {
        this.commonMessages = commonMessages;
    }

    /**
     * Return a Properties object defining locale-independent common messages, if any.
     */
    protected Properties getCommonMessages() {
        return this.commonMessages;
    }

    /**
     * Set whether to use the message code as default message instead of
     * throwing a NoSuchMessageException. Useful for development and debugging.
     * Default is "false".
     * <p>Note: In case of a MessageSourceResolvable with multiple codes
     * (like a FieldError) and a MessageSource that has a parent MessageSource,
     * do <i>not</i> activate "useCodeAsDefaultMessage" in the <i>parent</i>:
     * Else, you'll get the first code returned as message by the parent,
     * without attempts to check further codes.
     * <p>To be able to work with "useCodeAsDefaultMessage" turned on in the parent,
     * AbstractMessageSource and AbstractApplicationContext contain special checks
     * to delegate to the internal {@link #getMessageInternal} method if available.
     * In general, it is recommended to just use "useCodeAsDefaultMessage" during
     * development and not rely on it in production in the first place, though.
     * @see #getMessage(String, Object[], java.util.Locale)
     * @see org.springframework.validation.FieldError
     */
    public void setUseCodeAsDefaultMessage(boolean useCodeAsDefaultMessage) {
        this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
    }

    /**
     * Return whether to use the message code as default message instead of
     * throwing a NoSuchMessageException. Useful for development and debugging.
     * Default is "false".
     * <p>Alternatively, consider overriding the {@link #getDefaultMessage}
     * method to return a custom fallback message for an unresolvable code.
     * @see #getDefaultMessage(String)
     */
    protected boolean isUseCodeAsDefaultMessage() {
        return this.useCodeAsDefaultMessage;
    }


    @Override
    public final String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        return getMessage(code, new ICUListMessageArguments(args), defaultMessage, locale);
    }

    @Override
    public final String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        return getMessage(code, new ICUListMessageArguments(args), locale);
    }

    @Override
    public final String getMessage(String code, Map<String, Object> args, String defaultMessage, Locale locale) {
        return getMessage(code, new ICUMapMessageArguments(args), defaultMessage, locale);
    }

    @Override
    public final String getMessage(String code, Map<String, Object> args, Locale locale) throws NoSuchMessageException {
        return getMessage(code, new ICUMapMessageArguments(args), locale);
    }

    private String getMessage(String code, ICUMessageArguments args, String defaultMessage, Locale locale) {
        String msg = getMessageInternal(code, args, locale);
        if (msg != null) {
            return msg;
        }
        if (defaultMessage == null) {
            String fallback = getDefaultMessage(code);
            if (fallback != null) {
                return fallback;
            }
        }
        return renderDefaultMessage(defaultMessage, args, locale);
    }

    private String getMessage(String code, ICUMessageArguments args, Locale locale) throws NoSuchMessageException {
        String msg = getMessageInternal(code, args, locale);
        if (msg != null) {
            return msg;
        }
        String fallback = getDefaultMessage(code);
        if (fallback != null) {
            return fallback;
        }
        throw new NoSuchMessageException(code, locale);
    }

    @Override
    public final String getMessage(MessageSourceResolvable resolvable, Locale locale)
            throws NoSuchMessageException {

        String[] codes = resolvable.getCodes();
        if (codes == null) {
            codes = new String[0];
        }
        ICUMessageArguments args = new ICUListMessageArguments(resolvable.getArguments());
        for (String code : codes) {
            String msg = getMessageInternal(code, args, locale);
            if (msg != null) {
                return msg;
            }
        }
        String defaultMessage = resolvable.getDefaultMessage();
        if (defaultMessage != null) {
            return renderDefaultMessage(defaultMessage, args, locale);
        }
        if (codes.length > 0) {
            String fallback = getDefaultMessage(codes[0]);
            if (fallback != null) {
                return fallback;
            }
        }
        throw new NoSuchMessageException(codes.length > 0 ? codes[codes.length - 1] : null, locale);
    }


    /**
     * Resolve the given code and arguments as message in the given Locale,
     * returning {@code null} if not found. Does <i>not</i> fall back to
     * the code as default message. Invoked by {@code getMessage} methods.
     * @param code the code to lookup up, such as 'calculator.noRateSet'
     * @param args arguments that will be filled in for params within the message
     * @param locale the Locale in which to do the lookup
     * @return the resolved message, or {@code null} if not found
     * @see #getMessage(String, Object[], String, java.util.Locale)
     * @see #getMessage(String, Object[], java.util.Locale)
     * @see #getMessage(org.springframework.context.MessageSourceResolvable, java.util.Locale)
     * @see #setUseCodeAsDefaultMessage
     */
    protected String getMessageInternal(String code, ICUMessageArguments args, Locale locale) {
        if (code == null) {
            return null;
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }

        ICUMessageArguments argsToUse = args;

        if (!isAlwaysUseMessageFormat() && args.isEmpty()) {
            // Optimized resolution: no arguments to apply,
            // therefore no MessageFormat needs to be involved.
            // Note that the default implementation still uses MessageFormat;
            // this can be overridden in specific subclasses.
            String message = resolveCodeWithoutArguments(code, locale);
            if (message != null) {
                return message;
            }
        }

        else {
            // Resolve arguments eagerly, for the case where the message
            // is defined in a parent MessageSource but resolvable arguments
            // are defined in the child MessageSource.
            argsToUse = resolveArguments(args, locale);

            MessageFormat messageFormat = resolveCode(code, locale);
            if (messageFormat != null) {
                synchronized (messageFormat) {
                    return argsToUse.formatWith(messageFormat);
                }
            }
        }

        // Check locale-independent common messages for the given message code.
        Properties commonMessages = getCommonMessages();
        if (commonMessages != null) {
            String commonMessage = commonMessages.getProperty(code);
            if (commonMessage != null) {
                return formatMessage(commonMessage, args, locale);
            }
        }

        // Not found -> check parent, if any.
        return getMessageFromParent(code, argsToUse, locale);
    }

    /**
     * Try to retrieve the given message from the parent MessageSource, if any.
     * @param code the code to lookup up, such as 'calculator.noRateSet'
     * @param args arguments that will be filled in for params
     * within the message
     * @param locale the Locale in which to do the lookup
     * @return the resolved message, or {@code null} if not found
     * @see #getParentMessageSource()
     */
    protected String getMessageFromParent(String code, ICUMessageArguments args, Locale locale) {
        MessageSource parent = getParentMessageSource();
        if (parent != null) {
            if (parent instanceof ICUAbstractMessageSource) {
                // Call internal method to avoid getting the default code back
                // in case of "useCodeAsDefaultMessage" being activated.
                return ((ICUAbstractMessageSource) parent).getMessageInternal(code, args, locale);
            }
            else {
                if (args instanceof ICUListMessageArguments) {
                    // Check parent MessageSource, returning null if not found there.
                    ICUListMessageArguments listArgs = (ICUListMessageArguments)args;
                    return parent.getMessage(code, listArgs.toArray(), null, locale);
                } else {
                    // returning a null because the parent is not an instance of ICUAbstractMessageSource and
                    // therefore doesn't support arguments as map
                    return null;
                }
            }
        }
        // Not found in parent either.
        return null;
    }

    /**
     * Return a fallback default message for the given code, if any.
     * <p>Default is to return the code itself if "useCodeAsDefaultMessage" is activated,
     * or return no fallback else. In case of no fallback, the caller will usually
     * receive a NoSuchMessageException from {@code getMessage}.
     * @param code the message code that we couldn't resolve
     * and that we didn't receive an explicit default message for
     * @return the default message to use, or {@code null} if none
     * @see #setUseCodeAsDefaultMessage
     */
    protected String getDefaultMessage(String code) {
        if (isUseCodeAsDefaultMessage()) {
            return code;
        }
        return null;
    }


    /**
     * Searches through the given arguments, finds any MessageSourceResolvable
     * objects and resolves them.
     * <p>Allows for messages to have MessageSourceResolvables as arguments.
     * @param args arguments for a message
     * @param locale the locale to resolve through
     */
    @Override
    protected ICUMessageArguments resolveArguments(ICUMessageArguments args, final Locale locale) {
        return args.transform(new ICUMessageArguments.Transformation() {
            @Override
            public Object transform(Object item) {
                if (item instanceof MessageSourceResolvable)
                    return getMessage((MessageSourceResolvable) item, locale);
                return item;
            }
        });
    }

    /**
     * Subclasses can override this method to resolve a message without arguments
     * in an optimized fashion, i.e. to resolve without involving a MessageFormat.
     * <p>The default implementation <i>does</i> use MessageFormat, through
     * delegating to the {@link #resolveCode} method. Subclasses are encouraged
     * to replace this with optimized resolution.
     * <p>Unfortunately, {@code java.text.MessageFormat} is not implemented
     * in an efficient fashion. In particular, it does not detect that a message
     * pattern doesn't contain argument placeholders in the first place. Therefore,
     * it is advisable to circumvent MessageFormat for messages without arguments.
     * @param code the code of the message to resolve
     * @param locale the Locale to resolve the code for
     * (subclasses are encouraged to support internationalization)
     * @return the message String, or {@code null} if not found
     * @see #resolveCode
     * @see java.text.MessageFormat
     */
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        MessageFormat messageFormat = resolveCode(code, locale);
        if (messageFormat != null) {
            synchronized (messageFormat) {
                return messageFormat.format(new Object[0]);
            }
        }
        return null;
    }

    /**
     * Subclasses must implement this method to resolve a message.
     * <p>Returns a MessageFormat instance rather than a message String,
     * to allow for appropriate caching of MessageFormats in subclasses.
     * <p><b>Subclasses are encouraged to provide optimized resolution
     * for messages without arguments, not involving MessageFormat.</b>
     * See the {@link #resolveCodeWithoutArguments} javadoc for details.
     * @param code the code of the message to resolve
     * @param locale the Locale to resolve the code for
     * (subclasses are encouraged to support internationalization)
     * @return the MessageFormat for the message, or {@code null} if not found
     * @see #resolveCodeWithoutArguments(String, java.util.Locale)
     */
    protected abstract MessageFormat resolveCode(String code, Locale locale);

}