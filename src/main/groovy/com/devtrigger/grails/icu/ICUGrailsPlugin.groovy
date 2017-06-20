package com.devtrigger.grails.icu

import grails.config.Config
import grails.config.Settings
import grails.core.GrailsApplication
import grails.util.BuildSettings
import grails.util.Environment
import org.apache.commons.logging.LogFactory
import org.grails.plugins.i18n.I18nGrailsPlugin
import org.springframework.core.io.Resource

import java.nio.file.Files

class ICUGrailsPlugin extends I18nGrailsPlugin {

    private static LOG = LogFactory.getLog(this)

    String version = "0.1.1"
    def grailsVersion = "3.2 > *"
    def title = "ICU Support Plugin"
    def author = "Andrey Mochalov"
    def authorEmail = "andrey.s.mochalov@gmail.com"
    def description = "Provides the ICU4J's message formatting features, such as named arguments support, " +
            'flexible plural formatting, rule based number format, date interval formats, etc.'
    def documentation = "https://github.com/mochalov/grails-icu/blob/master/README.md"
    def license = "APACHE"
    def issueManagement = [ url: "https://github.com/mochalov/grails-icu/issues" ]
    def scm = [ url: "https://github.com/mochalov/grails-icu/" ]

    @Override
    Closure doWithSpring() {{->
        GrailsApplication application = grailsApplication
        Config config = application.config
        boolean gspEnableReload = config.getProperty(Settings.GSP_ENABLE_RELOAD, Boolean, false)
        String encoding = config.getProperty(Settings.GSP_VIEW_ENCODING, 'UTF-8')

        messageSource(ICUPluginAwareResourceBundleMessageSource, application, pluginManager) {
            fallbackToSystemLocale = false
            if (Environment.current.isReloadEnabled() || gspEnableReload) {
                cacheSeconds = config.getProperty(I18N_CACHE_SECONDS, Integer, 5)
                fileCacheSeconds = config.getProperty(I18N_FILE_CACHE_SECONDS, Integer, 5)
            }
            defaultEncoding = encoding
        }
    }}

    @Override
    void onChange(Map<String, Object> event) {
        def ctx = applicationContext
        def application = grailsApplication
        if (!ctx) {
            log.debug("Application context not found. Can't reload")
            return
        }

        boolean nativeascii = application.config.getProperty('grails.enable.native2ascii', Boolean, true)
        def resourcesDir = BuildSettings.RESOURCES_DIR
        def classesDir = BuildSettings.CLASSES_DIR

        if (resourcesDir.exists() && event.source instanceof Resource) {
            File eventFile = event.source.file.canonicalFile
            File i18nDir = eventFile.parentFile
            if (isChildOfFile(eventFile, i18nDir)) {
                if( i18nDir.name == 'i18n' && i18nDir.parentFile.name == 'grails-app') {
                    def appDir = i18nDir.parentFile.parentFile
                    resourcesDir = new File(appDir, BuildSettings.BUILD_RESOURCES_PATH)
                    classesDir = new File(appDir, BuildSettings.BUILD_CLASSES_PATH)
                }

                if(nativeascii) {
                    // if native2ascii is enabled then read the properties and write them out again
                    // so that unicode escaping is applied
                    def properties = new Properties()
                    eventFile.withReader {
                        properties.load(it)
                    }
                    // by using an OutputStream the unicode characters will be escaped
                    new File(resourcesDir, eventFile.name).withOutputStream {
                        properties.store(it, "")
                    }
                    new File(classesDir, eventFile.name).withOutputStream {
                        properties.store(it, "")
                    }
                }
                else {
                    // otherwise just copy the file as is
                    Files.copy( eventFile.toPath(),new File(resourcesDir, eventFile.name).toPath() )
                    Files.copy( eventFile.toPath(),new File(classesDir, eventFile.name).toPath() )
                }

            }
        }

        def messageSource = ctx.getBean('messageSource')
        if (messageSource instanceof ReloadableResourceBundleMessageSource) {
            messageSource.clearCache()
        }
    }

    protected boolean isChildOfFile(File child, File parent) {
        def currentFile = child.canonicalFile
        while(currentFile != null) {
            if (currentFile == parent) {
                return true
            }
            currentFile = currentFile.parentFile
        }
        return false
    }
}
