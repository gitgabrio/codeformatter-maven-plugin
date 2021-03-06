
package net.cardosi.maven.codeformatter.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Class responsible of SCM invocation
 */
public class SCMHelper {

    private SCMHelper() {
    }

    private static final String ORG_APACHE_MAVEN_PLUGINS = "org.apache.maven.plugins";
    private static final String SCM_PLUGIN = "maven-scm-plugin";
    private static final String CONNECTIONTYPE_CONFIG = "connectionType";
    private static final String DEVELOPERCONNECTION = "developerConnection";
    private static final String DIFF_GOAL = "diff";
    private static final String[] FILE_IDENTIFIERS = {"--- a/", "diff --git a/"};

    private static final String[] TO_REMOVE = {
            String.format("src%1$smain%1$sjava%1$s", File.separatorChar),
            String.format("src%1$smain%1$sresources%1$s", File.separatorChar),
            String.format("src%1$stest%1$sjava%1$s", File.separatorChar),
            String.format("src%1$stest%1$sresources%1$s", File.separatorChar)};


    public static List<File> getModifiedFiles(final MavenProject mavenProject,
                                              final MavenSession mavenSession,
                                              final BuildPluginManager pluginManager,
                                              final String scmPluginVersion,
                                              final Log log) throws MojoExecutionException {
        log.info("Invoking " + SCM_PLUGIN);
        executeMojo(plugin(groupId(ORG_APACHE_MAVEN_PLUGINS), artifactId(SCM_PLUGIN), version(scmPluginVersion)),
                goal(DIFF_GOAL), configuration(element(name(CONNECTIONTYPE_CONFIG), DEVELOPERCONNECTION)),
                executionEnvironment(mavenProject, mavenSession, pluginManager));
        String diffFileName = mavenProject.getArtifactId() + ".diff";
        String fullDiffPath = mavenProject.getBasedir().getPath() + File.separator + diffFileName;
        File diffFile = new File(fullDiffPath);
        return new ArrayList<>(getModifiedFiles(diffFile, log));

    }

    private static Set<File> getModifiedFiles(final File diffFile,
                                              final Log log) {
        log.info("Reading file " + diffFile);
        Set<File> toReturn = new HashSet<>();
        try (Stream<String> stream = Files.lines(diffFile.toPath(), StandardCharsets.UTF_8)) {
            stream.forEach(s -> parseLine(s, log).ifPresent(toReturn::add));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    private static Optional<File> parseLine(final String line,
                                            final Log log) {
        log.debug("Parsing line " + line);
        for (String fileIndentifier : FILE_IDENTIFIERS) {
            if (line.startsWith(fileIndentifier)) {
                String toParse = line.replace(fileIndentifier, "");
                String filePart = toParse.split(" ")[0];
                for (String toRemove : TO_REMOVE) {
                    filePart = filePart.replace(toRemove, "");
                }
                return Optional.of(new File(filePart));
            }
        }
        return Optional.empty();
    }
}
