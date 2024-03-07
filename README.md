# MergeLog4j2Plugins 

A tool that may be used in an Ant build script to merge an annotation-generated Log4j2Plugins.dat 
with multiple Log4j2Plugins.dat files found in Jars in the project library directory.

Useful when building a standalone Jar and supports custom Log4j plug-ins.

Reads the generated Log4j2Plugins.dat file then scans the project lib directory Jar files and
merges any additional Log4j2Plugins.dat files. Overwrites the generated file with the merged results.

## Sequence
  * Compile project with annotations
  * Run MergeLog4j2Plugins to merge Log4j2Plugins.dat files
  * Save a copy of the resulting merged Log4j2Plugins.dat
  * Unpack library Jars into build directory
  * Copy merged Log4j2Plugins.dat back to META-INF/org/apache/logging/log4j/core/config/plugins
  * Build Jar

See example Ant script below.

## Download

From the build/ directory:
* [MergeLog4j2Plugins.jar](https://github.com/Corionis/MergeLog4j2Plugins/raw/main/build/MergeLog4j2Plugins.jar?raw=true)

## Arguments

  ``-d | --dat [path]`` : Path to annotation-generated Log4j2Plugins.dat file.<br/>
  ``-l | --lib [path]`` : Path to project lib directory.

## Example

  ``java -jar artifacts/bin/MergeLog4j2Plugins.jar --dat "out/jar/META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat" --lib lib``

## Use in an Ant build script

```

<?xml version="1.0" encoding="UTF-8"?>
<project name="Acme build" default="All" basedir=".">
    <target name="init">
        <property file="acme.properties"/>
        <path id="class.path">
            <fileset dir="lib">
                <include name="*.jar"/>
                <include name="org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor"/>
            </fileset>
        </path>
    </target>

    <target name="clean" depends="init">
        <delete dir="out" failonerror="false"/>
        <mkdir dir="out"/>
    </target>

    <!-- Compile the project with annotations -->
    <target name="compile" depends="clean">
        <mkdir dir="out/jar"/>
        <javac destdir="out/jar" classpathref="class.path" includeantruntime="false" debug="on">
            <src path="src"/>
            <include name="**/*.java"/>
        </javac>

        <!-- Merge annotation-generated Log4j2Plugins.dat with any in library Jars -->
        <exec executable="/usr/bin/java">
            <arg value="-jar"/>
            <arg value="artifacts/bin/MergeLog4j2Plugins.jar"/>
            <arg value="--dat"/>
            <arg value="out/jar/META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat"/>
            <arg value="--lib"/>
            <arg value="lib"/>
        </exec>

        <!-- Save a copy of the merged Log4j2Plugins.dat -->
        <copy file="out/jar/META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat" tofile="out/Log4j2Plugins.dat" overwrite="true" preservelastmodified="true"/>
    </target>

    <target name="assemble" depends="compile">
        <!-- Unzip library Jars to create a single Jar -->
        <!-- Will overwrite Log4j2Plugins.dat if in any library Jar -->
        <unzip dest="out/jar">
            <fileset dir="lib">
                <include name="**/*.jar"/>
            </fileset>
        </unzip>

        <!-- Restore the merged Log4j2Plugins.dat -->
        <copy file="out/Log4j2Plugins.dat" tofile="out/jar/META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat" overwrite="true" preservelastmodified="true"/>
    </target>

    <target name="jar" depends="assemble">
        <manifest file="out/jar/META-INF/MANIFEST.MF">
            <attribute name="Main-Class" value="com.acme.Main"/>
        </manifest>
        <jar destfile="build/Acme.jar" manifest="out/jar/META-INF/MANIFEST.MF">
            <fileset dir="out/jar" includes="**/"/>
        </jar>
    </target>

    <target name="All" depends="jar" description="Build all"/>
</project>

```

## Related

Used in the [ELS Project](https://github.com/Corionis/ELS).

## Credits

This is a Java "port" of Riccardo Balbo's GitHub gist: [mergeLog4j2PluginsDat.gradle](https://gist.github.com/riccardobl/bdbbae9f2e8fefcd28dc5482ddc6b374)

