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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Class responsible of Impsort invocation
 */
public class ImpsortHelper {

    private ImpsortHelper() {
    }

    private static final String NET_REVELC_CODE = "net.revelc.code";
    private static final String IMPSORT_MAVEN_PLUGIN = "impsort-maven-plugin";
    private static final String IMPSORT_GOAL = "sort";

    public static void fixImports(final MavenProject mavenProject,
                                   final MavenSession mavenSession,
                                   final BuildPluginManager pluginManager,
                                   final Element[] configurationElements,
                                   final String impsortPluginVersion,
                                   final Log log) throws MojoExecutionException {
        log.info("Invoking " + IMPSORT_MAVEN_PLUGIN);
        executeMojo(plugin(groupId(NET_REVELC_CODE), artifactId(IMPSORT_MAVEN_PLUGIN), version(impsortPluginVersion)),
                goal(IMPSORT_GOAL), configuration(configurationElements),
                executionEnvironment(mavenProject, mavenSession, pluginManager));

    }

}
