Right now, this plugin isn't much.  It's a work in progress.

But for those using the Getdown auto-updater provided by Threerings, this plugin should help generate not only your getdown.txt and digest.txt files, but will do so as a simple run of the mill Maven plugin.

Currently, the plugin is NOT in Maven central, though the plan is to put it there before very long.

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

I'm still building features . . . and this is MEGA bare-bones.  Feedback is welcome, especially as I learn how people are actually structuring their maven projects compared to how they want them structured for Getdown packaging.  Eventually, the entire **target/getdown** directory will be zipped up and attached as an artifact on the project.

Currently, the assumption is that there is ONE main project artifact, and that it's a JAR.  Before long, I'll build in support to handle multi-artifact projects that may or may not be JARs.