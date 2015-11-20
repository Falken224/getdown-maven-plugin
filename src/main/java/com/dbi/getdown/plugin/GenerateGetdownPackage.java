package ru.open.getdown.plugin;

import com.threerings.getdown.tools.Digester;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * This Mojo can take a variety of inputs and generate a getdown.txt with appropriate
 * 'code = ' entries, either from scratch, or from a default getdown.txt template.
 * It will also create a basic project structure that can be deployed directly
 * to the web server where the appbase points, ready and configured for use.
 * <br>
 * All output goes into the target directory.
 *
 * @author Falken
 * @author leontiev
 * @author Ivan Zemlyanskiy
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class GenerateGetdownPackage extends AbstractMojo {

    private final static String GETDOWN_CONFIG_FILE = "getdown.txt";

    private final static String GETDOWN_ARTIFACTS_DIRECTORY = "bin";

    private final static String GETDOWN_JVMARG_PREFIX = "jvmarg";

    private final static String GETDOWN_APPARG_PREFIX = "apparg";

    private final static String GETDOWN_CODE_PREFIX = "code = " + GETDOWN_ARTIFACTS_DIRECTORY + "/";

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> projectRepos;

    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

    /**
     * This parameter a map of configuration properties to be placed into the getdown.txt
     * file.
     */
    @Parameter(property = "configProps")
    private Map<String, String> configProps;

    /**
     * This parameter is a template getdown.txt config file which will be copied
     * and appended to in the final product.
     */
    @Parameter(property = "configFile")
    private File configFile;

    @Parameter(property = "jvmargs")
    private List<String> jvmargs;

    @Parameter(property = "appargs")
    private List<String> appargs;

    @Parameter(property = "copyThis")
    private boolean copyThis = true;

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter
    private JvmLocation[] jvmLocations;

    public void execute() throws MojoExecutionException {
        File appDir = outputDirectory;
        File codeDir = new File(appDir, GETDOWN_ARTIFACTS_DIRECTORY);

        if (!appDir.exists()) {
            appDir.mkdirs();
        }

        if (!codeDir.exists()) {
            codeDir.mkdirs();
        }

        File config = new File(appDir, GETDOWN_CONFIG_FILE);

        FileWriter configWriter = null;
        try {
            configWriter = new FileWriter(config);
            if (configFile != null) {
                copyTemplate(configWriter);
            }

            writeJvmargs(configWriter);

            writeAppargs(configWriter);

            writeProperties(configWriter);

            writeJVMLocations(configWriter);

            Set<String> artifactNames = copyDependencies(codeDir);

            writeCodes(artifactNames, configWriter);

            // Copy project's artifact is needed
            if (copyThis && project.getArtifact().getFile() != null) {
                FileUtils.copyFile(project.getArtifact().getFile(),
                        new File(codeDir, project.getArtifact().getFile().getName()));
                configWriter.write(GETDOWN_CODE_PREFIX + project.getArtifact().getFile().getName());
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Error creating file.", e);
        } catch (DependencyResolutionException e) {
            throw new MojoExecutionException("Error resolving dependency.", e);
        } finally {
            if (configWriter != null) {
                try {
                    configWriter.close();
                } catch (IOException e) {
                    // ignore
                    getLog().warn("Got IOException while close configWriter: ", e);
                }
            }
        }

        //This should be the LAST thing we do!  Make the digest!
        digest(appDir);
    }

    private void writeAppargs(FileWriter configWriter) throws IOException {
        StringBuilder appargsBuilder = new StringBuilder();
        for (String apparg : appargs) {
            appargsBuilder.append(GETDOWN_APPARG_PREFIX).append(" = ").append(apparg).append("\n");
        }
        configWriter.write(appargsBuilder.toString());
    }

    private void writeJvmargs(FileWriter configWriter) throws IOException {
        StringBuilder jvmargsBuilder = new StringBuilder();
        for (String jvmarg : jvmargs) {
            jvmargsBuilder.append(GETDOWN_JVMARG_PREFIX).append(" = ").append(jvmarg).append("\n");
        }
        configWriter.write(jvmargsBuilder.toString());
    }

    private void writeCodes(Set<String> artifactNames, FileWriter configWriter) throws IOException {
        StringBuilder codeBuilder = new StringBuilder();
        for (String artifactName : artifactNames) {
            codeBuilder.append(GETDOWN_CODE_PREFIX).append(artifactName).append("\n");
        }
        configWriter.write(codeBuilder.toString());
    }

    private void writeProperties(FileWriter configWriter) throws IOException {
        StringBuilder propStringBuilder = new StringBuilder();
        for (Entry<String, String> entry : configProps.entrySet()) {
            propStringBuilder.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        configWriter.write(propStringBuilder.toString());
    }

    /**
     * Gererates custom jvm_location entries according to the supplied
     * configuration.
     *
     * @param configWriter writer to the generated getdown.txt file.
     */
    private void writeJVMLocations(Writer configWriter) throws IOException {
        if (jvmLocations == null || jvmLocations.length == 0) {
            return;
        }

        configWriter.write("\n# Auto-generated 'java_location' entries\n");
        for (JvmLocation jvm : jvmLocations) {
            configWriter.write("java_location = ");
            String platform = jvm.getPlatform();
            if (platform != null && !platform.isEmpty()) {
                configWriter.write("[" + platform + "] ");
            }
            configWriter.write(jvm.getPath() + "\n");
        }
    }


    private Set<String> copyDependencies(File destination) throws DependencyResolutionException, IOException {
        Set<String> artifactNames = new HashSet<String>();
        for (Dependency dependency : project.getDependencies()) {
            if (!excludedScopes().contains(dependency.getScope())) {
                String depString = dependency.getGroupId() + ":" + dependency.getArtifactId()
                        + ":" + dependency.getVersion();

                CollectRequest request = new CollectRequest(
                        new org.eclipse.aether.graph.Dependency(
                                new DefaultArtifact(depString), dependency.getScope()), projectRepos);

                DependencyResult result = repoSystem.resolveDependencies(
                        repoSession,
                        new DependencyRequest(request, new ScopeDependencyFilter(null, excludedScopes())));

                for (ArtifactResult artifactResult : result.getArtifactResults()) {
                    File artifactFile = artifactResult.getArtifact().getFile();
                    File codeFile = new File(destination, artifactFile.getName());
                    FileUtils.copyFile(artifactFile, codeFile);
                    artifactNames.add(artifactFile.getName());
                }
            }
        }
        return artifactNames;
    }

    private Collection<String> excludedScopes() {
        return Arrays.asList("test", "provided");
    }

    private void copyTemplate(FileWriter configWriter) throws IOException {
        FileReader reader = new FileReader(configFile);
        int size = -1;
        char[] buf = new char[1024];
        do {
            size = reader.read(buf);
            if (size >= 0) {
                configWriter.write(buf, 0, size);
            }
        } while (size >= 0);
    }

    private void digest(File appDir) throws MojoExecutionException {
        try {
            Digester.createDigest(appDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Error writing digest.", e);
        }
    }

}
