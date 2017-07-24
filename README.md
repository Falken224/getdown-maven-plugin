Right now, this plugin isn't much.  It's a work in progress.

But for those using the Getdown auto-updater provided by Threerings, this plugin should help generate not only your getdown.txt and digest.txt files, but will do so as a simple run of the mill Maven plugin.

This plugin is in maven central now, though it's only updated rarely, since I've kind of let development lapse.  I've taken several contributions, however, and will gladly accept any that make sense.

Below is an example configuration of your maven project to use this plugin:

```xml
  <plugin>
    <groupId>com.digitalbarista</groupId>
    <artifactId>getdown-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <configuration>
      <!– A reference to a base getdown.txt file in your project –>
      <configFile>src/main/configs/getdown.txt</configFile>
      <!– A list of additional properties that should appear in the getdown.txt –>
      <configProps>
        <appbase>http://www.mycompany.com/software/myApp/
        <ui.name>My App - Play it and enjoy!</ui.name>
      </configProps>
      <!--
        Set stripVersions to true if you want the versions removed from the jar
        file names, so that getdown does not leave a trail of jar versions behind
      -->
      <stripVersions>false</stripVersions>

      <!--
        (Optional) Setup custom java_location entries (see getdown documentation
        for details).

      Per-platform example:
      <jvmLocations>
        <jvmLocation>
          <platform>windows</platform>
          <path>/jvm/jvm-windows.jar</path>
        </jvmLocation>
        <jvmLocation>
          <platform>linux</platform>
          <path>/jvm/jvm-linux.jar</path>
        </jvmLocation>
      </jvmLocations>

      Single platform example:
      <jvmLocations>
        <jvmLocation>
          <path>/jvm/jvm.jar</path>
        </jvmLocation>
      </jvmLocations>

      Adding non-classpath resources:
      <resources>
        <resource>
	      <source>${basedir}/src/main/scripts/startapp.cmd</source>
          <destination>/</destination>
        </resource>
      </resources>

		
      -->
    </configuration>
    <executions>
      <execution>
        <phase>install</phase>
        <goals>
          <goal>build</goal>
        </goals>
      <execution>
    </executions>
  </plugin>
```

The output is put into the **target/getdown** directory of your project

The resulting directory structure should look like this:

<pre>
  target
  |--getdown
    |--getdown.txt    #Modified with 'code = ' entries for all your dependencies.
    |--digest.txt     #Not signed yet, but that will come at some point.
    |--code
      |--myApp.jar
      |--someDependency.jar
      |--anotherDependency.jar
</pre>

Currently, the assumption is that there is ONE main project artifact, and that it's a JAR.  I had intended at one point to build in support to handle multi-artifact projects that may or may not be JARs, but I'm not sure there's much demand, and my time available to devote to this plugin isn't abundant anymore.

There aren't a ton of features, since this was mostly a learning project.  This is MEGA bare-bones.  Feedback is welcome, and I'll incorporate any suggestions I can as I learn how people are actually structuring their maven projects compared to how they want them structured for Getdown packaging.  However, this is a dormant project . . . I'm not actively using it, and with the recent movement toward gradle, I'm not sure there's a huge demand for this project.

However, please use it, modify it, and let me know what you think!
