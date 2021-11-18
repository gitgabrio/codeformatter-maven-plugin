# codeformatter-maven-plugin

Maven plugin to automatically format changed code.

This plugin make use of the `org.twdata.maven:mojo-executor` one to group and chain features of different plugins,
namely:

1. `org.apache.maven.plugins:maven-scm-plugin`
2. `net.revelc.code.formatter:formatter-maven-plugin`
3. `net.revelc.code:impsort-maven-plugin`

The default goal for the current plugin is `format`, that, in turns:

1. invokes `org.apache.maven.plugins:maven-scm-plugin` to retrieve the list of locally modified files
2. invokes `net.revelc.code.formatter:formatter-maven-plugin` to format the modified files
3. invokes `net.revelc.code:impsort-maven-plugin` to fix imports of the modified files

The plugin is executed, by default, at `process-resource` phase, so soon before actual compilation.
Ratio behind that is that developers usually compile at least once before actually pushing the code, and in that way during compilation the format may be executed but only for actually modified files.

To preserve original configuration of `net.revelc.code.formatter:formatter-maven-plugin`
and `net.revelc.code:impsort-maven-plugin`, their parameters have been copied inside the `FormatterMojo`, so that their
behavior could be driven exactly as in their stand-alone usage.

An example pom configuration could be:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>groupId</groupId>
    <artifactId>maven_tester_project</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <formatter.skip>false</formatter.skip>
        <formatter.goal>format</formatter.goal>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>


    <name>Code-formatter Sample Project</name>
    <url>http://somecompany.com</url>
    <scm>
        <connection>(PROJECT_SPECIFIC)</connection>
        <developerConnection>(PROJECT_SPECIFIC)</developerConnection>
        <url>(PROJECT_SPECIFIC)</url>
    </scm>

    <build>
        <plugins>
            <plugin>
                <groupId>net.cardosi</groupId>
                <artifactId>codeformatter-maven-plugin</artifactId>
                <version>1.0-FINAL</version>
                <configuration>
                    <!-- Formatter -->
                    <configFile>eclipse-format.xml</configFile>
                    <lineEnding>LF</lineEnding>
                    <skip>${formatter.skip}</skip>
                    <configFile>${project.basedir}/src/main/resources/eclipse-format.xml</configFile>
                    <!-- Impsort -->
                    <!-- keep in sync with kogito-build/kogito-ide-config/src/main/resources/eclipse.importorder -->
                    <groups>java.,javax.,org.,com.,io.</groups>
                    <staticGroups>*</staticGroups>
                    <staticAfter>true</staticAfter>
                    <!-- keep in sync with the formatter-maven-plugin -->
                    <skip>${formatter.skip}</skip>
                    <removeUnused>true</removeUnused>
                </configuration>
                <executions>
                    <execution>
                        <id>code-formatting</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>format</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
```

The `org.apache.maven.plugins:maven-scm-plugin` configuration is done inside the `scm` tag.
See [here](https://maven.apache.org/scm/maven-scm-plugin/) for more detail.

E.g.

```xml

<scm>
    <connection>scm:git:http://github.com:(USERNAME)/(PROJECT_NAME)</connection>
    <developerConnection>scm:git:http://github.com:(USERNAME)/(PROJECT_NAME)</developerConnection>
</scm>
```

Give it a try
============

1. locally checkout the codeformatter-maven-plugin repo
2. switch to `main` branch
3. `mvn clean install` it
4. create a "testing" project with configuration similar to the one provided above
5. add some files to the "tester" project
6. add and push the "tester" project inside github
7. fix the `scm` tag to match values of the "target" project 
8. modify/add other files to project
9. issue `mvn clean install` on the "tester" project
10. the modified files should have been formatted/impsorted soon before actual compilation.



