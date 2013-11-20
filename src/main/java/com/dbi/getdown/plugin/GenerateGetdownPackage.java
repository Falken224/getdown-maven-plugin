package com.dbi.getdown.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.filter.ScopeDependencyFilter;


@Mojo( name = "build", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class GenerateGetdownPackage
    extends AbstractMojo
{
    @Component
    private RepositorySystem repoSystem;
    
    @Parameter( defaultValue = "${repositorySystemSession}",readonly = true)
    private RepositorySystemSession repoSession;
    
    @Parameter( defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> projectRepos;
    
    /**
     * Location of the file.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;

    @Parameter( property = "configProps")
    private Properties props;
    
    @Parameter( defaultValue = "${project}")
    private MavenProject project;
    
    public void execute()
        throws MojoExecutionException
    {
        File f = new File(outputDirectory,"getdown");
        File codeDir = new File(f, "code");

        if ( !f.exists() )
        {
            f.mkdirs();
        }

        if ( !codeDir.exists() )
        {
            codeDir.mkdirs();
        }
        
        File config = new File( f, "getdown.txt" );

        FileWriter w = null;
        try
        {
            w = new FileWriter( config );
            StringBuffer sb = new StringBuffer();
            sb.append("# Generic specified getdown.txt properties\n");
            for(Entry<Object,Object> entry : props.entrySet())
            {
                sb.append(entry.getKey().toString()).append(" = ").append(entry.getValue()).append("\n");
            }
        
            sb.append("# Auto-generated 'code' entries\n");
            for(Dependency dep : project.getDependencies())
            {
                String depString = dep.getGroupId()+":"+dep.getArtifactId()+":"+dep.getVersion();
                CollectRequest request = new CollectRequest(new org.sonatype.aether.graph.Dependency(new DefaultArtifact(depString), dep.getScope()),projectRepos);
                DependencyResult result = repoSystem.resolveDependencies(repoSession, new DependencyRequest(request, new ScopeDependencyFilter("test","provided")));

                for(ArtifactResult dependency : result.getArtifactResults())
                {
                    File d = dependency.getArtifact().getFile();
                    w.write("code = code/"+d.getName() +"\n");
                    File codeFile = new File( codeDir, d.getName());
                    copyFile(d, codeFile);
                }
            }
            copyFile(project.getArtifact().getFile(),new File(codeDir, project.getArtifact().getFile().getName()));
            w.write("code = code/"+project.getArtifact().getFile().getName());
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating file.", e );
        } catch (DependencyResolutionException e) {
            throw new MojoExecutionException( "Error resolving dependency.", e );
        }
        finally
        {
            if ( w != null )
            {
                try
                {
                    w.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
    }
    
    public void copyFile(File source, File dest) throws IOException
    {
        dest.createNewFile();
    }
}
