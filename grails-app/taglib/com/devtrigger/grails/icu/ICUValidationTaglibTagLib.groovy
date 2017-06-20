package com.devtrigger.grails.icu

import groovy.transform.CompileStatic
import org.grails.encoder.Encoder
import org.grails.plugins.web.taglib.FormatTagLib
import org.grails.plugins.web.taglib.ValidationTagLib
import org.grails.taglib.GrailsTagException
import org.springframework.context.MessageSourceResolvable
import org.springframework.context.NoSuchMessageException
import org.springframework.context.support.DefaultMessageSourceResolvable

/**
 * Provides additional features to the g:message tag, such as
 * an ability to pass a map as message arguments
 */
class ICUValidationTaglibTagLib extends ValidationTagLib {

    static returnObjectForTags = ['message']

    @Override
    @CompileStatic
    def messageImpl(Map attrs) {
        Locale locale = FormatTagLib.resolveLocale(attrs.locale)

        def text
        Object error = attrs.error ?: attrs.message
        if (error) {
            if (!attrs.encodeAs && error instanceof MessageSourceResolvable) {
                MessageSourceResolvable errorResolvable = (MessageSourceResolvable)error
                if (errorResolvable.arguments) {
                    error = new DefaultMessageSourceResolvable(errorResolvable.codes, encodeArgumentsIfRequired(errorResolvable.arguments) as Object[], errorResolvable.defaultMessage)
                }
            }
            try {
                if (error instanceof MessageSourceResolvable) {
                    text = messageSource.getMessage((MessageSourceResolvable)error, locale)
                } else {
                    text = messageSource.getMessage(error.toString(), null, locale)
                }
            }
            catch (NoSuchMessageException e) {
                if (error instanceof MessageSourceResolvable) {
                    text = ((MessageSourceResolvable)error).codes[0]
                }
                else {
                    text = error?.toString()
                }
            }
        }
        else if (attrs.code) {
            String code = attrs.code?.toString()
            String defaultMessage
            if (attrs.containsKey('default')) {
                defaultMessage = attrs['default']?.toString()
            } else {
                defaultMessage = code
            }

            def message
            switch (attrs.args) {
                case null:
                    message = messageSource.getMessage(code, null, defaultMessage, locale)
                    break
                case Map:
                    if (!messageSource instanceof ICUMessageSource)
                        throw new GrailsTagException('Trying to pass the arguments as map, but the messageSource is not ICU compatible');
                    Map args = attrs.args as Map
                    if (!attrs.encodeAs) encodeArgumentsIfRequired(args)
                    message = ((ICUMessageSource)messageSource).getMessage(code, args, defaultMessage, locale)
                    break
                default:
                    List args = attrs.encodeAs ? attrs.args as List : encodeArgumentsIfRequired(attrs.args)
                    message = messageSource.getMessage(code, args.toArray(), defaultMessage, locale)
                    break
            }

            if (message != null) {
                text = message
            }
            else {
                text = defaultMessage
            }
        }
        if (text) {
            Encoder encoder = codecLookup.lookupEncoder(attrs.encodeAs?.toString() ?: 'raw')
            return encoder  ? encoder.encode(text) : text
        }
        ''
    }

    @CompileStatic
    private Map encodeArgumentsIfRequired(Map arguments) {
        arguments.collectEntries { key, value ->
            [key, encodeArgumentIfRequired(value)]
        }
    }

    @CompileStatic
    private List encodeArgumentsIfRequired(arguments) {
        arguments.collect { encodeArgumentIfRequired it }
    }

    @CompileStatic
    private def encodeArgumentIfRequired(value) {
        if (value == null || value instanceof Number || value instanceof Date) {
            value
        } else {
            Encoder encoder = codecLookup.lookupEncoder('HTML')
            encoder ? encoder.encode(value) : value
        }
    }

}
