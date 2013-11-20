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
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;


@Mojo( name = "getdown-config", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class GenerateGetdownConfigMojo
    extends AbstractMojo
{
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

        if ( !f.exists() )
        {
            f.mkdirs();
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
            
            for(Artifact artifact : project.getDependencyArtifacts())
            {
                File dependency = artifact.getFile();
                w.write("code = code/"+dependency.getName());
                File codeFile = new File( f, artifact.getFile().getName());
                copyFile(dependency, codeFile);
            }
            
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating file.", e );
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
        dest.mkdirs();
        dest.createNewFile();
    }
}
