/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.maven.codeformatter.plugin;

import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

public class ConfigurationHelper {

    private static final String INCLUDES = "includes";

    private static final List<String> FORMATTER_PARAMETERS = Arrays.asList("sourceDirectory",
            "testSourceDirectory",
            "targetDirectory",
            "basedir",
            "cachedir",
            "directories",
            INCLUDES,
            "excludes",
            "compilerSource",
            "compilerCompliance",
            "compilerTargetPlatform",
            "encoding",
            "lineEnding",
            "configFile",
            "configJsFile",
            "configHtmlFile",
            "configXmlFile",
            "configJsonFile",
            "configCssFile",
            "skipJavaFormatting",
            "skipJsFormatting",
            "skipHtmlFormatting",
            "skipXmlFormatting",
            "skipJsonFormatting",
            "skipCssFormatting",
            "skipFormatting",
            "useEclipseDefaults",
            "javaExclusionPattern");

    private static final List<String> IMPSORT_PARAMETERS = Arrays.asList("sourceEncoding",
            "skip",
            "staticGroups",
            "groups",
            "staticAfter",
            "joinStaticWithNonStatic",
            "sourceDirectory",
            "testSourceDirectory",
            "directories",
            "includes",
            "excludes",
            "removeUnused",
            "treatSamePackageAsUnused",
            "breadthFirstComparator",
            "lineEnding",
            "compliance"
    );


    private ConfigurationHelper() {
    }

    public static Element[] getFormatterConfigurationElements(final MojoExecutor.Element includes,
                                                              final PlexusConfiguration pomConfiguration,
                                                              final PluginParameterExpressionEvaluator expressionEvaluator,
                                                              final Log log) throws ExpressionEvaluationException {
        log.info("Executing getFormatterConfigurationElements");
        final List<PlexusConfiguration> plexusConfigurations = getPlexusConfigurations(pomConfiguration, FORMATTER_PARAMETERS, log);
        return getConfigurationElements(includes, plexusConfigurations, expressionEvaluator, log);
    }

    public static Element[] getImpsortConfigurationElements(final MojoExecutor.Element includes,
                                                            final PlexusConfiguration pomConfiguration,
                                                            final PluginParameterExpressionEvaluator expressionEvaluator,
                                                            final Log log) throws ExpressionEvaluationException {
        log.info("Executing getImpsortConfigurationElements");
        final List<PlexusConfiguration> plexusConfigurations = getPlexusConfigurations(pomConfiguration, IMPSORT_PARAMETERS, log);
        return getConfigurationElements(includes, plexusConfigurations, expressionEvaluator, log);
    }

    private static Element[] getConfigurationElements(final MojoExecutor.Element includes,
                                                      final List<PlexusConfiguration> plexusConfigurations,
                                                      final PluginParameterExpressionEvaluator expressionEvaluator,
                                                      final Log log) throws ExpressionEvaluationException {
        log.info("Executing getConfigurationElements");
        MojoExecutor.Element[] toReturn = new MojoExecutor.Element[plexusConfigurations.size()];
        for (int i = 0; i < toReturn.length; i++) {
            PlexusConfiguration plexusConfiguration = plexusConfigurations.get(i);
            if (plexusConfiguration.getName().equals(INCLUDES)) {
                toReturn[i] = includes;
            } else {
                toReturn[i] = getElement(plexusConfiguration, expressionEvaluator, log);
            }
        }
        return toReturn;
    }

    public static Element getIncludesElement(final List<File> files,
                                             final Log log) {
        log.debug("getIncludesElement " + files);
        MojoExecutor.Element[] children = new MojoExecutor.Element[files.size()];
        for (int i = 0; i < children.length; i++) {
            children[i] = new MojoExecutor.Element("", files.get(i).toString());
        }
        return element(name(INCLUDES), children);
    }

    private static Element getElement(final PlexusConfiguration plexusConfiguration,
                                      final PluginParameterExpressionEvaluator expressionEvaluator,
                                      final Log log) throws ExpressionEvaluationException {
        log.debug("getElement " + plexusConfiguration);
        String value = plexusConfiguration.getValue();
        String defaultValue = plexusConfiguration.getAttribute("default-value");
        String configurationName = plexusConfiguration.getName();
        String evaluated = defaultIfNull(expressionEvaluator.evaluate(defaultIfBlank(value, defaultValue)), "").toString();
        return element(name(configurationName), evaluated);
    }

    private static List<PlexusConfiguration> getPlexusConfigurations(final PlexusConfiguration pomConfiguration,
                                                                     final List<String> parameters,
                                                                     final Log log) {
        log.debug("getPlexusConfigurations");
        return Arrays.stream(pomConfiguration.getChildren())
                .filter(plexusConfiguration -> parameters.contains(plexusConfiguration.getName()))
                .collect(Collectors.toList());

    }
}
