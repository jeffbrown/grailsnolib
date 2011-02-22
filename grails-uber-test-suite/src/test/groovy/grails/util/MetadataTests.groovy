package grails.util

/**
 * @author Graeme Rocher
 * @since 1.2
 */
class MetadataTests extends GroovyTestCase {

    void testPluginMetadata() {
            def m = Metadata.getInstance(new ByteArrayInputStream('''
plugins.tomcat=1.1
plugins.hibernate=1.2
'''.bytes))

        assertEquals "1.1", m.getInstalledPlugins().tomcat
        assertEquals "1.2", m.getInstalledPlugins().hibernate
    }
}
