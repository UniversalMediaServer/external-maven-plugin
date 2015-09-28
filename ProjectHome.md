This Maven (mojo) plugin is used to aid in managing external  Maven artifacts that are not published public Maven repositories.   This plugin can install and or deploy local files or web hosted files into your Maven repository.


---


So you have a project fully setup Maven resolving all dependencies from various public Maven repositories.  **Wonderful!**  But what do you do when there are one or two artifacts that are not hosted on any public M2 repository?  Well typically you manually install then in your local M2 repository and if you have a group or company M2 repository you may manually deploy them there for all team members to get access to.  Obviously the best option is to contact the project owner and try to get them to publish to a public repository, but in reality this may take some time to get in place or the project may simply choose not to publish to Maven.  I wanted a more automated method to manage these type of external dependencies so I started this Maven plugin to help streamline this process.

The goal of this project is to allow you to define a configuration for external (_not hosted in M2 repo_) artifacts that will automatically download the artifact, install to you local M2 repository and optionally deploy to your team/company M2 repository.

Granted there is still a bit of manual effort involved for updating the POM configuration information for these external dependencies when new versions are available, but this should eliminate the manual effort of having to install and deploy these types of files by hand.

Here is an example of an artifact configured to download from an external URL and install to the local Maven repository
```
   <!-- THIS JAR IS HOSTED ON GOOGLE CODE, 
        BUT IS NOT AVAILABLE IN A MAVEN REPO -->
   <artifactItem>
       <groupId>com.google.code</groupId>
       <artifactId>google-api-translate-java</artifactId>
       <version>0.92</version>
       <packaging>jar</packaging>
       <downloadUrl>
          http://google-api-translate-java.googlecode.com/files/google-api-translate-java-{version}.jar
       </downloadUrl>
       <install>true</install>
       <force>false</force>
   </artifactItem>
```


&lt;BR&gt;



Please visit this link for a complete example of a POM file configured for this this plugin: 

&lt;BR&gt;


http://code.google.com/p/maven-external-dependency-plugin/source/browse/trunk/maven-external-dependency-plugin-test/pom.xml


---

**Maven Documentation Site**

&lt;BR&gt;


http://www.savage7.com/maven/plugin/maven-external-dependency-plugin/index.html
