
/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import groovy.xml.MarkupBuilder
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoaderHolder
import grails.util.GrailsNameUtils
import org.apache.commons.io.FilenameUtils

/**
 * Gant script that deals with those tasks required for plugin developers
 * (as opposed to plugin users).
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsPackage")

pluginIncludes = [
	metadataFile.name,
	"*GrailsPlugin.groovy",
    "plugin.xml",
	"grails-app/**",
	"lib/**",
    "scripts/**",
	"web-app/**",
	"src/**",
	"docs/api/**",
	"docs/gapi/**"
]

pluginExcludes = [
	"web-app/WEB-INF/**",
	"web-app/plugins/**",
    "grails-app/conf/spring/resources.groovy",
	"grails-app/conf/*DataSource.groovy",
    "grails-app/conf/BootStrap.groovy",
    "grails-app/conf/Config.groovy",
    "grails-app/conf/BuildConfig.groovy",
    "grails-app/conf/UrlMappings.groovy",
	"**/.svn/**",
	"test/**",
	"**/CVS/**"
]

target(packagePlugin:"Implementation target") {
    depends (checkVersion, packageApp)

    def pluginFile
    new File("${basedir}").eachFile {
        if(it.name.endsWith("GrailsPlugin.groovy")) {
            pluginFile = it
        }
    }

    if(!pluginFile) ant.fail("Plugin file not found for plugin project")
    plugin = generatePluginXml(pluginFile)

	event("PackagePluginStart", [pluginName])

    // Package plugin's zip distribution
    pluginZip = "${basedir}/grails-${pluginName}-${plugin.version}.zip"
    ant.delete(file:pluginZip)

    def plugin = loadBasePlugin()
    if(plugin?.pluginExcludes) {
        pluginExcludes.addAll(plugin?.pluginExcludes)
    }
    
    def includesList = pluginIncludes.join(",")
    def excludesList = pluginExcludes.join(",")
    ant.zip(basedir:"${basedir}", destfile:pluginZip, includes:includesList, excludes:excludesList, filesonly:true)

	event("PackagePluginEnd", [pluginName])

}

private def loadBasePlugin() {
		pluginManager?.allPlugins?.find { it.basePlugin }
}
