/*
 * Copyright (C) 2013 Digital Barista, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dbi.getdown.plugin;

import com.threerings.getdown.tools.Digester;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;


/**
 * This Mojo can take a variety of inputs and generate a getdown.txt with appropriate
 * 'code = ' entries, either from scratch, or from a default getdown.txt template.
 * It will also create a basic project structure that can be deployed directly
 * to the web server where the appbase points, ready and configured for use.
 *
 * This project structure can either be zipped up or not.
 *
 * All output goes into the target/getdown directory.
 * @author Falken
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class GenerateGetdownPackage
    extends AbstractMojo
{
    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "false")
    private boolean stripVersions;

    @Parameter
    private JvmLocation[] jvmLocations;

    /**
     * This parameter a map of configuration properties to be placed into the getdown.txt
     * file.
     */
    @Parameter(property = "configProps")
    private Map<String,String> configProps;

    /**
     * This parameter is a template getdown.txt config file which will be copied
     * and appended to in the final product.
     */
    @Parameter
    private File configFile;

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    /**
     * This flag when 'true' will zip up the contents of the target/getdown
     * directory into a specified output file.  Defaults to 'false'
     */
    @Parameter(defaultValue = "false")
    private boolean zipContents;

    @Component(role = Archiver.class, hint = "zip")
    private ZipArchiver zipArchiver;

    /**
     * When the 'zipContents' flag is true, this is the name of the outputted
     * ZIP file.
     */
    @Parameter(property = "zipFileName", defaultValue = "getdown-project.zip")
    private String zipFileName;

    @Override
    public void execute() throws MojoExecutionException
    {
        File appDir = new File(outputDirectory, "getdown");
        ensureDirectory(appDir);

        generateGetdownConfiguration(appDir);
        generateDigest(appDir);
        createZipArchive(appDir);
    }

    private void generateGetdownConfiguration(File applicationDirectory)
            throws MojoExecutionException {

        File config = new File(applicationDirectory, "getdown.txt");
        try (Writer configWriter = new BufferedWriter(new FileWriter(config))) {

            copyBaseConfigurationFile(configWriter);
            writePomConfigurationEntries(configWriter);
            writeCodeEntries(applicationDirectory, configWriter);
            writeJVMLocations(configWriter);

        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Error creating getdown configuration file.", e);
        }
    }

    private void generateDigest(File appDir) throws MojoExecutionException
    {
        try {
            Digester.createDigest(appDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Error generating digest file.", e);
        }
    }

    private void createZipArchive(File applicationDirectory)
            throws MojoExecutionException
    {
        if (!zipContents) {
            return;
        }

        try {
            zipArchiver.addDirectory(applicationDirectory);
            zipArchiver.setDestFile(
                    new File(applicationDirectory, this.zipFileName));
            zipArchiver.createArchive();
        } catch (IOException e) {
            throw new MojoExecutionException("Error zipping the getdown project.", e);
        }
    }

    private void copyBaseConfigurationFile(Writer configWriter)
            throws IOException
    {
        if (configFile != null) {
            IOUtil.copy(new FileReader(configFile), configWriter);
        }
    }

    private void writePomConfigurationEntries(Writer configWriter)
            throws IOException
    {
        configWriter.write("\n\n# Pom-configured properties\n");
        for (Entry<String, String> entry : configProps.entrySet()) {
            configWriter.write(entry.getKey());
            configWriter.write(" = ");
            configWriter.write(entry.getValue());
            configWriter.write("\n");
        }
    }

    private void writeCodeEntries(File appDirectory, Writer configWriter)
            throws IOException, MojoExecutionException
    {
        File codeDirectory = new File(appDirectory, "code");
        ensureDirectory(codeDirectory);

        configWriter.write("\n# Auto-generated 'code' entries\n");

        // Dependencies
        for (ArtifactResult result : resolveDependencies()) {
            Artifact artifact = result.getArtifact();
            File d = artifact.getFile();
            String fileName = d.getName();
            if (stripVersions) {
                fileName = artifact.getArtifactId() + ".jar";
            }

            FileUtils.copyFile(d, new File(codeDirectory, fileName));
            configWriter.write("code = code/" + fileName + "\n");
        }

        // Main artifact
        String fileName = project.getArtifact().getFile().getName();
        if (stripVersions) {
            fileName = project.getArtifactId() + ".jar";
        }
        FileUtils.copyFile(project.getArtifact().getFile(), new File(codeDirectory, fileName));
        configWriter.write("code = code/" + fileName + "\n");
    }

    /**
     * Gererates custom jvm_location entries according to the supplied
     * configuration.
     *
     * @param configWriter writer to the generated getdown.txt file.
     */
    private void writeJVMLocations(Writer configWriter) throws IOException
    {
        if (jvmLocations == null || jvmLocations.length == 0) {
            return;
        }

        configWriter.write("\n# Auto-generated 'java_location' entries\n");
        for (JvmLocation jvm : jvmLocations) {
            configWriter.write("java_location = ");
            String platform = jvm.getPlatform();
            if (platform != null && !platform.isEmpty()) {
                configWriter.write("["  + platform + "] ");
            }
            configWriter.write(jvm.getPath() + "\n");
        }
    }

    private void ensureDirectory(File directory) throws MojoExecutionException
    {
        boolean ok = directory.exists() || directory.mkdirs();
        if (!ok) {
            throw new MojoExecutionException("Cannot create directory \""
                    + directory.getAbsolutePath() + "\"");
        }
    }

    private List<ArtifactResult> resolveDependencies()
            throws MojoExecutionException
    {
        DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(
                JavaScopes.RUNTIME);

        CollectRequest collectRequest = new CollectRequest();
        org.apache.maven.artifact.Artifact artifact = project.getArtifact();
        Artifact defaultArtifact = new DefaultArtifact(artifact.getGroupId(),
                artifact.getArtifactId(), artifact.getType(), artifact.getVersion());
        collectRequest.setRoot(new Dependency(defaultArtifact, JavaScopes.RUNTIME));
        collectRequest.setRepositories(remoteRepos);

        DependencyRequest dependencyRequest =
                new DependencyRequest(collectRequest, classpathFlter);

        try {
            List<ArtifactResult> artifactResults = repoSystem.resolveDependencies(
                    repoSession, dependencyRequest).getArtifactResults();
            return artifactResults;
        } catch (DependencyResolutionException ex) {
            throw new MojoExecutionException("Error resolving dependencies.", ex);
        }
    }

}
