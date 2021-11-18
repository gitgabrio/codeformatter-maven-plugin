
package net.cardosi.maven.codeformatter.plugin;

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
