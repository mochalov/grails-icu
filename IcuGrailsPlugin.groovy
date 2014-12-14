import com.devtrigger.grails.icu.ICUPluginAwareResourceBundleMessageSource
import com.devtrigger.grails.icu.ICUReloadableResourceBundleMessageSource
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin
import grails.util.Environment
import org.codehaus.groovy.grails.web.context.GrailsConfigUtils
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine

class IcuGrailsPlugin extends I18nGrailsPlugin {

    private static LOG = LogFactory.getLog(this)

    String version = "0.1"

    def grailsVersion = "2.4 > *"

    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "ICU Support Plugin"

    def author = "Andrey Mochalov"

    def authorEmail = "andrey.s.mochalov@gmail.com"

    def description = 'Provides the ICU4J\'s message formatting features, such as named arguments support, ' +
            'flexible plural formatting, rule based number format, date interval formats, etc.'

    // URL to the plugin's documentation
    //def documentation = "http://grails.org/plugin/icu"

    def license = "APACHE"

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    def doWithSpring = { ctx ->
        Set baseNames = []

        def messageResources
        if (application.warDeployed) {
            messageResources = parentCtx?.getResources("**/WEB-INF/${baseDir}/**/*.properties")?.toList()
        }
        else {
            messageResources = plugin.watchedResources
        }

        calculateBaseNamesFromMessageSources(messageResources, baseNames)

        messageSource(ICUPluginAwareResourceBundleMessageSource) {
            basenames = baseNames.toArray()
            fallbackToSystemLocale = false
            pluginManager = manager
            if (Environment.current.isReloadEnabled() || GrailsConfigUtils.isConfigTrue(application, GroovyPagesTemplateEngine.CONFIG_PROPERTY_GSP_ENABLE_RELOAD)) {
                def cacheSecondsSetting = application?.flatConfig?.get('grails.i18n.cache.seconds')
                cacheSeconds = cacheSecondsSetting == null ? 5 : cacheSecondsSetting as Integer
                def fileCacheSecondsSetting = application?.flatConfig?.get('grails.i18n.filecache.seconds')
                fileCacheSeconds = fileCacheSecondsSetting == null ? 5 : fileCacheSecondsSetting as Integer
            }
            if (Environment.isWarDeployed()) {
                resourceResolver = ref('servletContextResourceResolver')
            }
        }
    }

    def onChange = { event ->
        def ctx = event.ctx
        if (!ctx) {
            LOG.debug("Application context not found. Can't reload")
            return
        }

        def messageSource = ctx.messageSource
        if (messageSource instanceof ICUReloadableResourceBundleMessageSource) {
            messageSource.clearCache()
        }
        else {
            LOG.warn "Bean messageSource is not an instance of ${ICUReloadableResourceBundleMessageSource.name}. Can't reload"
        }
    }
}