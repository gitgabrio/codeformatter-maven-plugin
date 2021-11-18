
package net.cardosi.maven.codeformatter.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Class responsible of Formatter invocation
 */
public class FormatterHelper {

    private FormatterHelper() {
    }

    private static final String NET_REVELC_CODE_FORMATTER = "net.revelc.code.formatter";
    private static final String FORMATTER_MAVEN_PLUGIN = "formatter-maven-plugin";
    private static final String FORMAT_GOAL = "format";

    public static void formatFiles(final MavenProject mavenProject,
                                   final MavenSession mavenSession,
                                   final BuildPluginManager pluginManager,
                                   final Element[] configurationElements,
                                   final String formatterPluginVersion,
                                   final Log log) throws MojoExecutionException {
        log.info("Invoking " + FORMATTER_MAVEN_PLUGIN);
        executeMojo(plugin(groupId(NET_REVELC_CODE_FORMATTER), artifactId(FORMATTER_MAVEN_PLUGIN), version(formatterPluginVersion)),
                goal(FORMAT_GOAL), configuration(configurationElements),
                executionEnvironment(mavenProject, mavenSession, pluginManager));

    }

}
