# Introduction #

This project is hosted in the following public M2 repositories:

&lt;BR&gt;



  * 

&lt;release-builds&gt;

  http://repo2.maven.org/maven2    (Maven Central)
  * 

&lt;snapshot-builds&gt;

  http://oss.sonatype.org/content/groups/public   (Sonatype OSS)



&lt;BR&gt;



# Details #

Use the following **groupId** and **artifactId** when defining the plugin definition.
Make sure to update the **version** to the latest released version (_or snapshot version if you roll that way_).

```

         <plugin>
             <groupId>com.savage7.maven.plugins</groupId>
             <artifactId>maven-external-dependency-plugin</artifactId>
             <version>0.5</version>
             <inherited>false</inherited>                

```

If you are using a RELEASE version, then the artifacts will automatically be pulled from Maven Central.  No additional configuration is required.

If you are use a SNAPSHOT version, then you will need to add the following **pluginRepository** definition to your **pluginRepositories** section.

```

  <!--  MAVEN PLUGIN REPOSITORIES  -->
 
  <pluginRepositories>

      <pluginRepository>
          <id>ossrh</id>
          <name>Sonatype OSS Repository</name>
          <url>http://oss.sonatype.org/content/groups/public</url>
          <layout>default</layout>
      </pluginRepository>

  </pluginRepositories>  

```


---


Please visit this link for a complete example of a POM file configured for this this plugin: 

&lt;BR&gt;


http://code.google.com/p/maven-external-dependency-plugin/source/browse/trunk/maven-external-dependency-plugin-test/pom.xml