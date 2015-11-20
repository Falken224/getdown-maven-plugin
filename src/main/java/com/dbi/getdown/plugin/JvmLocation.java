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
package ru.open.getdown.plugin;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Definition of a jvm_location entry in the <code>getdown.txt</code> file.
 */
public class JvmLocation {
    
    @Parameter
    private String platform = null;
    
    @Parameter (required = true)
    private String path = null;

    /**
     * Set the platform identifier.
     * @param platform platform identifier.
     */
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    /**
     * Set the path to the jvm's jar archive.
     * @param path path to the jvm's jar archive.
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Get the platform definition string for this jvm.
     * 
     * @return string identifying the target platform, or null if there's none.
     */
    public String getPlatform() {
        return platform;
    }
    
    /**
     * Get the path to the jar file containing the jvm.
     * 
     * @return string to the jvm's path.
     */
    public String getPath() {
        return path;
    }
    
    @Override
    public String toString() {
        String buf = path;
        if (platform != null && !platform.isEmpty()) {
            buf += " (" + platform + ")";
        }
        return buf;
    }
    
}
