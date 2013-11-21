Right now, this plugin isn't much.  It's a work in progress.

But for those using the Getdown auto-updater provided by Threerings, this plugin should help generate not only your getdown.txt and digest.txt files, but will do so as a simple run of the mill Maven plugin.

Currently, the plugin is NOT in Maven central, though the plan is to put it there before very long.

Below is an example configuration of your maven project to use this plugin:

  <pre>
  &lt;plugin&gt;
    &lt;groupId&gt;com.dbi&lt;/groupId&gt;
    &lt;artifactId&gt;getdown-plugin&lt;/artifactId&gt;
    &lt;version&gt;1.0-SNAPSHOT&lt;/version&gt;
    &lt;configuration&gt;
      &lt;!-- A reference to a base getdown.txt file in your project --&gt;
      &lt;configFile&gt;src/main/configs/getdown.txt&lt;/configFile&gt;
      &lt;!-- A list of additional properties that should appear in the getdown.txt --&gt;
      &lt;configProps&gt;
        &lt;appbase&gt;http://www.mycompany.com/software/myApp/
        &lt;ui.name&gt;My App - Play it and enjoy!&lt;/ui.name&gt;
      &lt;/configProps&gt;
    &lt;/configuration&gt;
    &lt;executions&gt;
      &lt;execution&gt;
        &lt;phase&gt;install&lt;/phase&gt;
        &lt;goals&gt;
          &lt;goal&gt;build&lt;/goal&gt;
        &lt;/goals&gt;
      &lt;execution&gt;
    &lt;/executions&gt;
  &lt;/plugin&gt;
  </pre>

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