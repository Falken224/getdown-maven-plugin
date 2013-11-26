package com.dbi.getdown.plugin;

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

import com.threerings.getdown.tools.Digester;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
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


/**
 * This Mojo can take a variety of inputs and generate a getdown.txt with appropriate
 * 'code = ' entries, either from scratch, or from a default getdown.txt template.
 * It will also create a basic project structure that can be deployed directly
 * tot he web server where the appbase points, ready and configured for use.
 * 
 * This project structure can either be zipped up or not.
 * 
 * All output goes into the target/getdown directory.
 * @author Falken
 */
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
    
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;

    /**
     * This parameter a map of configuration properties to be placed into the getdown.txt
     * file.
     */
    @Parameter( property = "configProps")
    private Map<String,String> configProps;
    
    /**
     * This parameter is a template getdown.txt config file which will be copied
     * and appended to in the final product.
     */
    @Parameter( property = "configFile")
    private File configFile;
    
    @Parameter( defaultValue = "${project}")
    private MavenProject project;
    
    /**
     * This flag when 'true' will zip up the contents of the target/getdown
     * directory into a specified output file.  Defaults to 'false'
     */
    @Parameter( property = "zipContents")
    private boolean zipContents = false;
    
    @Component(role = Archiver.class, hint = "zip")
    private ZipArchiver zipArchiver;
    
    /**
     * When the 'zipContents' flag is true, this is the name of the outputted
     * ZIP file.
     */
    @Parameter( property = "zipFileName")
    private String zipFileName = "getdown-project.zip";
    
    public void execute()
        throws MojoExecutionException
    {
        File appDir = new File(outputDirectory,"getdown");
        File codeDir = new File(appDir, "code");

        if ( !appDir.exists() )
        {
            appDir.mkdirs();
        }

        if ( !codeDir.exists() )
        {
            codeDir.mkdirs();
        }
        
        File config = new File( appDir, "getdown.txt" );

        FileWriter configWriter = null;
        try
        {
            configWriter = new FileWriter( config );
            if(configFile!=null)
            {
                FileReader reader = new FileReader(configFile);
                int size=-1;
                char[] buf = new char[1024];
                do
                {
                    size = reader.read(buf);
                    if(size>=0)
                    {
                        configWriter.write(buf, 0, size);
                    }
                }while (size>=0);
            }
            StringBuffer sb = new StringBuffer();
            sb.append("\n\n# Generic specified getdown.txt properties\n");
            for(Entry<String,String> entry : configProps.entrySet())
            {
                sb.append(entry.getKey().toString()).append(" = ").append(entry.getValue()).append("\n");
                configWriter.write(sb.toString());
            }
        
            configWriter.write("\n# Auto-generated 'code' entries\n");
            for(Dependency dep : project.getDependencies())
            {
                String depString = dep.getGroupId()+":"+dep.getArtifactId()+":"+dep.getVersion();
                CollectRequest request = new CollectRequest(new org.sonatype.aether.graph.Dependency(new DefaultArtifact(depString), dep.getScope()),projectRepos);
                DependencyResult result = repoSystem.resolveDependencies(repoSession, new DependencyRequest(request, new ScopeDependencyFilter("test","provided")));

                for(ArtifactResult dependency : result.getArtifactResults())
                {
                    File d = dependency.getArtifact().getFile();
                    configWriter.write("code = code/"+d.getName() +"\n");
                    File codeFile = new File( codeDir, d.getName());
                    copyFile(d, codeFile);
                }
            }
            copyFile(project.getArtifact().getFile(),new File(codeDir, project.getArtifact().getFile().getName()));
            configWriter.write("code = code/"+project.getArtifact().getFile().getName());
            
            //This should be the LAST thing we do!  Make the digest!
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating file.", e );
        } catch (DependencyResolutionException e) {
            throw new MojoExecutionException( "Error resolving dependency.", e );
        }
        finally
        {
            if ( configWriter != null )
            {
                try
                {
                    configWriter.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
        try {
            Digester.createDigest(appDir);
        } catch (IOException e) {
            throw new MojoExecutionException( "Error writing digest.", e );
        }
        
        if(zipContents)
        {
            File zipOutFile = new File(appDir, this.zipFileName);
            try {
                zipArchiver.addDirectory(appDir);
                zipArchiver.setDestFile(new File(appDir, this.zipFileName));
                zipArchiver.createArchive();
            } catch (IOException e) {
                throw new MojoExecutionException( "Error zipping the getdown project.", e );
            }
        }
    }
    
    /**
     * A quick utility method to copy a file to a target location.
     * 
     * @param source The file to be copied.
     * @param dest The File to be written.
     * @throws IOException 
     */
    private void copyFile(File source, File dest) throws IOException
    {
        dest.createNewFile();
        FileInputStream in = new FileInputStream(source);
        FileOutputStream out = new FileOutputStream(dest);
        
        byte[] buf = new byte[1024];
        int size=-1;
        do
        {
            size = in.read(buf);
            if(size>=0)
            {
                out.write(buf, 0, size);
            }
        }while(size>=0);
        out.flush();
        try{in.close();}catch(Exception ex){}
        try{out.close();}catch(Exception ex){}
    }
}
