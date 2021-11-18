
package net.cardosi.maven.codeformatter.plugin;

import net.revelc.code.formatter.LineEnding;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.*;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.resource.ResourceManager;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static net.cardosi.maven.codeformatter.plugin.ConfigurationHelper.*;
import static net.cardosi.maven.codeformatter.plugin.FormatterHelper.formatFiles;
import static net.cardosi.maven.codeformatter.plugin.ImpsortHelper.fixImports;
import static net.cardosi.maven.codeformatter.plugin.SCMHelper.getModifiedFiles;

@Mojo(name = "format", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = true, threadSafe = true)
public class FormatMojo extends AbstractMojo {

    // Formatter-plugin properties

    /**
     * ResourceManager for retrieving the configFile resource.
     */
    @Component(role = ResourceManager.class)
    private ResourceManager resourceManager;

    /**
     * Project's target directory as specified in the POM.
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private File targetDirectory;

    /**
     * Project's base directory as specified in the POM.
     */
    @Parameter(defaultValue = "${project.basedir}", property = "baseDirectory", readonly = true, required = true)
    private File basedir;

    /**
     * Projects cache directory.
     *
     * <p>
     * This file is a hash cache of the files in the project source. It can be preserved in source code such that it
     * ensures builds are always fast by not unnecessarily writing files constantly. It can also be added to gitignore
     * in case startup is not necessary. It further can be redirected to another location.
     *
     * <p>
     * When stored in the repository, the cache if run on cross platforms will display the files multiple times due to
     * line ending differences on the platform.
     *
     * <p>
     * The cache itself has been part of formatter plugin for a long time but was hidden in target directory and did not
     * survive clean phase when it should. This is not intended to be clean in that way as one would want as close to a
     * no-op as possible when files are already all formatted and/or have not been otherwise touched. This is used based
     * off the files in the project so it is as much part of the source as any other file is.
     *
     * <p>
     * The cache can become invalid for any number of reasons that this plugin can't reasonably detect automatically. If
     * you rely on the cache and make any changes to the project that could conceivably make the cache invalid, or if
     * you notice that files aren't being reformatted when they should, just delete the cache and it will be rebuilt.
     *
     * @since 2.12.1
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "formatter.cachedir")
    private File cachedir;

    /**
     * Java compiler source version.
     */
    @Parameter(defaultValue = "1.8", property = "maven.compiler.source", required = true)
    private String compilerSource;

    /**
     * Java compiler compliance version.
     */
    @Parameter(defaultValue = "1.8", property = "maven.compiler.source", required = true)
    private String compilerCompliance;

    /**
     * Java compiler target version.
     */
    @Parameter(defaultValue = "1.8", property = "maven.compiler.target", required = true)
    private String compilerTargetPlatform;

    /**
     * The file encoding used to read and write source files. When not specified and sourceEncoding also not set,
     * default is platform file encoding.
     *
     * @since 0.3
     */
    @Parameter(property = "project.build.sourceEncoding", required = true)
    private String encoding;

    /**
     * File or classpath location of an Eclipse code formatter configuration xml file to use in formatting.
     */
    @Parameter(defaultValue = "formatter-maven-plugin/eclipse/java.xml", property = "configfile", required = true)
    private String configFile;

    /**
     * File or classpath location of an Eclipse code formatter configuration xml file to use in formatting.
     */
    @Parameter(defaultValue = "formatter-maven-plugin/eclipse/javascript.xml", property = "configjsfile", required = true)
    private String configJsFile;

    /**
     * File or classpath location of a properties file to use in html formatting.
     */
    @Parameter(defaultValue = "formatter-maven-plugin/jsoup/html.properties", property = "confightmlfile", required = true)
    private String configHtmlFile;

    /**
     * File or classpath location of a properties file to use in xml formatting.
     */
    @Parameter(defaultValue = "formatter-maven-plugin/eclipse/xml.properties", property = "configxmlfile", required = true)
    private String configXmlFile;

    /**
     * File or classpath location of a properties file to use in json formatting.
     */
    @Parameter(defaultValue = "formatter-maven-plugin/jackson/json.properties", property = "configjsonfile", required = true)
    private String configJsonFile;

    /**
     * File or classpath location of a properties file to use in css formatting.
     */
    @Parameter(defaultValue = "formatter-maven-plugin/ph-css/css.properties", property = "configcssfile", required = true)
    private String configCssFile;

    /**
     * Whether the java formatting is skipped.
     */
    @Parameter(defaultValue = "false", property = "formatter.java.skip")
    private boolean skipJavaFormatting;

    /**
     * Whether the javascript formatting is skipped.
     */
    @Parameter(defaultValue = "false", property = "formatter.js.skip")
    private boolean skipJsFormatting;

    /**
     * Whether the html formatting is skipped.
     */
    @Parameter(defaultValue = "false", property = "formatter.html.skip")
    private boolean skipHtmlFormatting;

    /**
     * Whether the xml formatting is skipped.
     */
    @Parameter(defaultValue = "false", property = "formatter.xml.skip")
    private boolean skipXmlFormatting;

    /**
     * Whether the json formatting is skipped.
     */
    @Parameter(defaultValue = "false", property = "formatter.json.skip")
    private boolean skipJsonFormatting;

    /**
     * Whether the css formatting is skipped.
     */
    @Parameter(defaultValue = "false", property = "formatter.css.skip")
    private boolean skipCssFormatting;

    /**
     * Whether the formatting is skipped.
     *
     * @since 0.5
     */
    @Parameter(defaultValue = "false", alias = "skip", property = "formatter.skip")
    private boolean skipFormatting;

    /**
     * Use eclipse defaults when set to true for java and javascript.
     */
    @Parameter(defaultValue = "false", property = "formatter.useEclipseDefaults")
    private boolean useEclipseDefaults;

    /**
     * A java regular expression pattern that can be used to exclude some portions of the java code from being
     * reformatted.
     * <p>
     * This can be useful when using DSL that embeds some kind of semantic hierarchy, where users can use various
     * indentation level to increase the readability of the code. Those semantics are ignored by the formatter, so this
     * regex pattern can be used to match certain portions of the code so that they will not be reformatted.
     * <p>
     * An example is the Apache Camel java DSL which can be used in the following way: <code><pre>
     * 	from("seda:a").routeId("a")
     * 			.log("routing at ${routeId}")
     * 			.multicast()
     * 				.to("seda:b")
     * 				.to("seda:c")
     * 			.end()
     * 			.log("End of routing");
     * </pre></code> In the above example, the exercept can be skipped by the formatter by defining the following
     * property in the formatter xml configuration: <code>
     * &lt;javaExclusionPattern>\b(from\([^;]*\.end[^;]*?\)\));&lt;/javaExclusionPattern>
     * </code>
     *
     * @since 2.13
     */
    @Parameter(property = "formatter.java.exclusion_pattern")
    private String javaExclusionPattern;

    // Impsort-plugin properties

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    protected PluginDescriptor plugin;

    @Parameter(defaultValue = "${project.build.sourceEncoding}", readonly = true)
    protected String sourceEncoding = StandardCharsets.UTF_8.name();

    /**
     * Allows skipping execution of this plugin.
     *
     * @since 1.0.0
     */
    @Parameter(alias = "skip", property = "impsort.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Configures the grouping of static imports. Groups are defined with comma-separated package name
     * prefixes. The special "*" group refers to imports not matching any other group, and is implied
     * after all other groups, if not specified. More specific groups are prioritized over less
     * specific ones. All groups are sorted.
     *
     * @since 1.0.0
     */
    @Parameter(alias = "staticGroups", property = "impsort.staticGroups", defaultValue = "*")
    protected String staticGroups;

    /**
     * Configures the grouping of non-static imports. Groups are defined with comma-separated package
     * name prefixes. The special "*" group refers to imports not matching any other group, and is
     * implied after all other groups, if not specified. More specific groups are prioritized over
     * less specific ones. All groups are sorted.
     *
     * @since 1.0.0
     */
    @Parameter(alias = "groups", property = "impsort.groups", defaultValue = "*")
    protected String groups;

    /**
     * Configures whether static groups will appear after non-static groups.
     *
     * @since 1.0.0
     */
    @Parameter(alias = "staticAfter", property = "impsort.staticAfter", defaultValue = "false")
    protected boolean staticAfter;

    /**
     * Allows omitting the blank line between the static and non-static sections.
     *
     * @since 1.0.0
     */
    @Parameter(alias = "joinStaticWithNonStatic", property = "impsort.joinStaticWithNonStatic",
            defaultValue = "false")
    protected boolean joinStaticWithNonStatic;


    /**
     * Configures whether to remove unused imports.
     *
     * @since 1.1.0
     */
    @Parameter(alias = "removeUnused", property = "impsort.removeUnused", defaultValue = "false")
    private boolean removeUnused;

    /**
     * Configures whether to treat imports in the current package as unused and subject to removal
     * along with other unused imports.
     *
     * @since 1.2.0
     */
    @Parameter(alias = "treatSamePackageAsUnused", property = "impsort.treatSamePackageAsUnused",
            defaultValue = "true")
    private boolean treatSamePackageAsUnused;

    /**
     * Configures whether to use a breadth first comparator for sorting static imports. This will
     * ensure all static imports from one class are grouped together before any static imports from an
     * inner-class.
     *
     * @since 1.3.0
     */
    @Parameter(alias = "breadthFirstComparator", property = "impsort.breadthFirstComparator",
            defaultValue = "true")
    private boolean breadthFirstComparator;

    /**
     * Sets the Java source compliance level (e.g. 1.0, 1.5, 1.7, 8, 9, 11, etc.)
     *
     * @since 1.5.0
     */
    @Parameter(alias = "compliance", property = "impsort.compliance",
            defaultValue = "${maven.compiler.release}")
    private String compliance;

    // Common properties

    /**
     * Project's main source directory as specified in the POM. Used by default if
     * <code>directories</code> is not set.
     *
     * @since 1.0.0
     */
    @Parameter(alias = "sourceDirectory", defaultValue = "${project.build.sourceDirectory}",
            readonly = true)
    private File sourceDirectory;

    /**
     * Project's test source directory as specified in the POM. Used by default if
     * <code>directories</code> is not set.
     *
     * @since 1.0.0
     */
    @Parameter(alias = "testSourceDirectory", defaultValue = "${project.build.testSourceDirectory}",
            readonly = true)
    private File testSourceDirectory;

    /**
     * Location of the Java source files to process. Defaults to source main and test directories if
     * not set.
     *
     * @since 1.0.0
     */
    @Parameter(alias = "directories", property = "directories")
    private File[] directories = null;


    /**
     * List of fileset patterns for Java source locations to include. Patterns are relative to the
     * directories selected. When not specified, the default include is <code>**&#47;*.java</code>
     *
     * @since 1.0.0
     */
    @Parameter(alias = "includes", property = "includes")
    private String[] includes;

    /**
     * List of fileset patterns for Java source locations to exclude. Patterns are relative to the
     * directories selected. When not specified, there is no default exclude.
     *
     * @since 1.0.0
     */
    @Parameter(alias = "excludes", property = "excludes")
    private String[] excludes;

    /**
     * Sets the line-ending of files after formatting. Valid values are:
     * <ul>
     * <li><b>"AUTO"</b> - Use line endings of current system</li>
     * <li><b>"KEEP"</b> - Preserve line endings of files, default to AUTO if ambiguous</li>
     * <li><b>"LF"</b> - Use Unix and Mac style line endings</li>
     * <li><b>"CRLF"</b> - Use DOS and Windows style line endings</li>
     * <li><b>"CR"</b> - Use early Mac style line endings</li>
     * </ul>
     *
     * @since 0.2.0
     */
    @Parameter(defaultValue = "AUTO", property = "lineending", required = true)
    private LineEnding lineEnding;

    // Plugin versions

    /**
     * The <code>maven-scm-plugin</code> version to use.
     * Default to <b>1.12.0</b>
     */
    @Parameter(defaultValue = "1.12.0", property = "scmPluginVersion")
    private String scmPluginVersion;


    /**
     * The <code>formatter-maven-plugin</code> version to use.
     * Default to <b>2.16.0</b>
     */
    @Parameter(defaultValue = "2.16.0", property = "formatterPluginVersion")
    private String formatterPluginVersion;

    /**
     * The <code>impsort-maven-plugin</code> version to use.
     * Default to <b>1.5.0</b>
     */
    @Parameter(defaultValue = "1.5.0", property = "impsortPluginVersion")
    private String impsortPluginVersion;

    // FormatMojo properties

    @Component
    private MavenProject mavenProject;

    @Component
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Parameter(defaultValue = "${mojoExecution}")
    protected MojoExecution mojoExecution;

    /**
     * Execute.
     *
     * @throws MojoExecutionException the mojo execution exception
     * @throws MojoFailureException   the mojo failure exception
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Begin execution....");
        final List<File> files = getModifiedFiles(mavenProject,
                mavenSession,
                pluginManager,
                scmPluginVersion,
                getLog());
        try {
            final PlexusConfiguration pomConfiguration = new XmlPlexusConfiguration(mojoExecution.getConfiguration());
            final PluginParameterExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(mavenSession, mojoExecution);
            final MojoExecutor.Element includes = getIncludesElement(files, getLog());
            final MojoExecutor.Element[] formatterConfigurationElements = getFormatterConfigurationElements(includes,
                    pomConfiguration,
                    expressionEvaluator,
                    getLog());
            formatFiles(mavenProject,
                    mavenSession,
                    pluginManager,
                    formatterConfigurationElements,
                    formatterPluginVersion,
                    getLog());
            final MojoExecutor.Element[] impsortConfigurationElements = getImpsortConfigurationElements(includes,
                    pomConfiguration,
                    expressionEvaluator,
                    getLog());
            fixImports(mavenProject,
                    mavenSession,
                    pluginManager,
                    impsortConfigurationElements,
                    impsortPluginVersion,
                    getLog());
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoFailureException(e.getMessage());
        }
        getLog().info("....done!");
    }

}
