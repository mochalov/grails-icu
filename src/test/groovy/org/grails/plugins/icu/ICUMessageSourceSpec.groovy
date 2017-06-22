package org.grails.plugins.icu

import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.Resource
import spock.lang.Specification

class ICUMessageSourceSpec  extends Specification{
    ICUMessageSource messageSource
    void setup(){
        def messages = new TestResource('messages.properties','''\
            simple=bar
            simple_arg=Formatted simple arg: {foo}
            formatted_number_arg=Formatted number arg: >>{foo,number}<<
            formatted_date_arg=Formatted date arg: >>{foo,date}<<
            formatted_time_arg=Formatted time arg: >>{foo,time}<<   
            i18nmessage=I18n >>{0}<<
        '''.stripIndent().getBytes('UTF-8'))
        messageSource = new ICUReloadableResourceBundleMessageSource(
                resourceLoader: new DefaultResourceLoader(){
                    Resource getResourceByPath(String path){
                        messages
                    }
                }
        )
        messageSource.setBasenames('messages')
    }

    void 'Check ICU simple message'(){
        given:
            def locale = Locale.US        
        expect:
            messageSource.getMessage('simple',null,locale) == ('bar')
    }
    void 'Check ICU simple message with argument'(){
        given:
            def locale = Locale.US
        expect:
            messageSource.getMessage('simple_arg',['foo':'bar'],locale) == ('Formatted simple arg: bar')
    }

    void 'Check ICU formatted number message'(){
        given:
            def locale = Locale.US
        expect:
            messageSource.getMessage('formatted_number_arg',['foo':1],locale) == ('Formatted number arg: >>1<<')
    }
    void 'Check ICU formatted date message'(){
        given:
            def date = Calendar.getInstance().with{
                it.set(YEAR,2017)
                it.set(MONTH,5)
                it.set(DAY_OF_MONTH,21)
                it.time
            }
        
            def locale = Locale.US
        expect:
            messageSource.getMessage('formatted_date_arg',['foo':date],locale) == ('Formatted date arg: >>Jun 21, 2017<<')
    }

    void 'Check default i18n message'(){
        given:
            
            def locale = Locale.US
        expect:
            messageSource.getMessage('i18nmessage',['bar'] as Object[],locale) == ('I18n >>bar<<')
    }
    class TestResource extends ByteArrayResource{
        String filename

        long lastModified
        
        TestResource(String filename, byte[] byteArray) {
            super(byteArray)
            this.filename=filename
        }

        @Override
        long lastModified() throws IOException {
            return this.lastModified
        }
    }
}

