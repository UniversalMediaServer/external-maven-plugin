# External Dependency Maven Plugin

This Maven plugin can be used to manage external dependencies that are not available in public Maven repositories or not mavenized at all. The plugin can download and install these dependencies as Maven artifacts in your local Maven repository so that they are available for Maven as any other dependencies and also deploy them to a remote repository.

This is a fork of [com.savage7.maven.plugins:maven-external-dependency-plugin] (https://code.google.com/p/maven-external-dependency-plugin/) that is updated for Maven 3.x. Its use has also been simplified slightly.

Maven 3.x introduced changes in dependency resolution that makes it impossible for this plugin to function optimally. In Maven 3.x all dependencies are resolved before the lifecycle phases are started, making it impossible for a plugin to download and install the external dependencies before Maven resolves the project's dependencies. Any dependencies needed during the build lifecycle, e.g. for the compile phase, must be resolved before Maven will start the lifecycle. That means that the external dependencies must be given as dependencies in ```pom.xml```, or the compile phase will fail. At the same time, including them as dependencies prevents Maven from starting the lifecycle as the dependencies can not be resolved.

There are multiple ways  to get around this, but none of them are as elegant as if this plugin could run before dependency resolution (as is possible in Maven 2.x). Basicly, there are 3 options:
* Run this plugin from a parent POM so that the dependencies are already installed when the child POM starts.
* Bind this plugin to the ```clean``` phase and build with ```mvn clean install``` each time the external dependencies need to be installed or updated. This will install the external dependencies during the ```clean``` lifecycle so that they are already in place when the build lifecycle begins.
* Manually run ```mvn external:install``` each time the external dependencies need to be installed or updated before starting the build lifcycle with e.g. ```mvn package``` or ```maven install```.

## Configuration

### Artifact configuration

External dependencies have to be turned into Maven artifacts to be accessable for Maven. This is done by creating ```artifactItems``` in the ```configuration``` section for this plugin like this:

```xml
<project>
  ...
  <build>
    ...
    <plugins>
      ...
      <plugin>
        <groupId>com.universalmediaserver</groupId>
        <artifactId>external-maven-plugin</artifactId>
        <version>x.y.z</version>
        ...
        <configuration>
          ...
          <artifactItems>
            <artifactItem>
              <groupId>...</groupId>
              <artifactId>...</artifactId>
              <version>...</version>
              <classifier>...</classifier>
              <localFile>...<localFile>
              <stagingDirectory>...</stagingDirectory>
              <downloadUrl>...</downloadUrl>
              <timeout>...</timeout>
              <packaging>...</packaging>
              <install>...</install>
              <deploy>...</deploy>
              <force>...</force>
              <pomFile>...</pomFile>
              <generatePom>...</generatePom>
              <createChecksum>...</createChecksum>
              <centralChecksumVerification>...</centralChecksumVerification>
              <checksum>...</checksum>
              <extractFile>...</extractFile>
              <extractFileChecksum>...</extractFileChecksum>
              <repack>...</repack>
            </artifactItem>
          </artifactItems>
          ...
        </configuration>
        ...
      </plugin>
      ...
    </plugins>
    ...
  </build>
...
</project>
```

Most of these parameteres are optional.

### Artifact parameter description

*Parameter* | *Mandatory* | *Default value* | *Description*
------------|----|----|--------------------------
**groupId** | **Yes** |  | The group id for the generated artifact. If none exists for the dependency, you can make one up.
**artifactId** | **Yes** |  | The artifact id for the generated artifact. If none exists for the dependency, you can make one up, but it's natural to use the dependency name here. 
**version** | **Yes** |  | Anything will work, but you should use the actual version of the dependency.
**classifier** | No |  | Classifier for the artifact (tests, sources, etc.)
**localFile** | No | {artifactId}-{version}- {classifier}.{packaging} | The name of the created local artifact.
**stagingDirectory** | No | The plugin configured ```stagingDirectory``` | The folder to which the external dependency should be downloaded.
**downloadUrl** | **Yes** |  | The URL to get the dependency from.
**timeout** | No | No timeout | Timeout in milliseconds for artifact download.
**packaging** | No | jar | The packaging type of the artifact.
**install** | No | True | Should the artifact be installed to the local Maven repository during ```install``` and ```localinstall``` goals?
**deploy** | No | True | Should the artifact be deployed to an external Maven repository during the ```deploy``` goal?
**force** | No | False | Should download, install and deploy be forced for this external dependency? Force means that the action is performed even though it's deemed not necessary.
**pomFile** | No |  | Location of an existing POM file to be installed alongside the main artifact.
**generatePom** | No | True | Should a minimal POM be generated for the artifact if none is specified in ```pomFile```?
**createChecksum** | No | The plugin configured ```createChecksum``` | Should MD5 and SHA-1 checksums be generated for the artifact during installation and deployment?
**centralChecksumVerification** | No | False | If this is true, the downloaded file's checksum will be verified with a query against Maven central repository to make sure the artifact isn't already there.
**checksum** | No |  | A checksum for the downloaded file used for verification.
**extractFile** |  |  | If the downloaded file is an archive, the name of the file to extract from the archive.
**extractFileChecksum** | No |  | A checksum for the extracted file used for verification.
**repack** | No | False | Should a folder be repacked as a new artifact?

### Plugin configuration

The plugin configuration is done in the ```configuration``` for the plugin like this:
```xml
<project>
  ...
  <build>
    ...
    <plugins>
      ...
      <plugin>
        <groupId>com.universalmediaserver</groupId>
        <artifactId>external-maven-plugin</artifactId>
        <version>x.y.z</version>
        ...
        <configuration>
          <project>...</project>
          <userSettings>...</userSettings>
          <globalSettings>...</globalSettings>
          <localRepository>...</localRepository>
          <stagingDirectory>...</stagingDirectory>
          <force>...</force>
          <centralChecksumVerification>...</centralChecksumVerification>
          <createChecksum>...</createChecksum>
          <remoteRepositories>
            ..
          </remoteRepositories>
          <artifactItems>
            ...
          </artifactItems>
        </configuration>
        ...
      </plugin>
      ...
    </plugins>
    ...
  </build>
...
</project>
```

### Plugin parameter description

*Parameter* | *Mandatory* | *Default value* | *Description*
------------|----|----|--------------------------
**project** | No | ${project} | The maven project
**userSettings** | No | ${user.home}/.m2/settings.xml | Location of the user ```settings.xml``` if needed.
**globalSettings** | No | ${env.M2_HOME}/conf/settings.xml | Location of the global ```settings.xml``` if needed.
**localRepository** | No | ${localRepository} | The local Maven repository.
**stagingDirectory** | No | ${project.build.directory} /external-dependencies | The staging directory for external dependencies where it's not specified on the ```artifactItem```.
**force** | No | False | Should download, install and deploy be forced for all external dependencies? Force means that the action is performed even though it's deemed not necessary. 
**centralChecksumVerification** | No | False | If this is true, the downloaded files' checksums will be verified with queries against Maven central repository to make sure the artifacts aren't already there.
**createChecksum** | No | True | Should MD5 and SHA-1 checksums be generated for the artifacts during installation and deployment?
**disableSSLValidation** | No | False | Should SSL/HTTPS validation be disabled when downloading external dependencies?
**remoteRepositories** | No |  | A list of remote repositories to be used when resolving external dependencies.
**artifactItems** | **Yes** |  | A list of ```artifactItems``` for this plugin as described above.

## Using the plugin

Given that the parameters are configured correctly, goals can be executed with:
```
mvn external:<goal>
```
Goals can also be bound to lifecycle phases to automatic execution. 

### Goals description

*Goal* | *Bindable phases* | *Command* | *Description*
-------|-----------------|-----------|--------------
**resolve** | ```generate-sources```, ```clean``` | ```mvn external:resolve``` | Resolves and downloads the configured ```artifactItems``` to the ```stagingDirectory```. 
**localinstall** | ```generate-sources```, ```clean``` | ```mvn external:localinstall``` | Installs already staged ```artifactItems``` to the local Maven repository.
**install** | ```generate-sources```, ```clean``` | ```mvn external:install``` | A combination of ```resolve``` and ```localinstall```. Resolves, downloads and installes the configured ```artifactItems```.
**clean** | ```clean``` |```mvn external:clean``` | Cleans the staging directory.

### Binding goals to lifecycle phases

Binding the goals to the bindable lifecycle phases is done in a standard way under ```executions```. A typical example is given below:

```xml
<project>
  ...
  <build>
    ...
    <plugins>
      ...
      <plugin>
        <groupId>com.universalmediaserver</groupId>
        <artifactId>external-maven-plugin</artifactId>
        <version>x.y.z</version>
        ...
        <configuration>
          ...
        </configuration>
        ...
        <executions>
          <execution>
            <id>clean-external-dependencies</id>
            <phase>clean</phase>
            <goals>
              <goal>clean</goal>
            </goals>
          </execution>
          <execution>
            <id>install-external-dependencies</id>
            <phase>clean</phase>
            <goals>
              <goal>install</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      ...
    </plugins>
    ...
  </build>
...
</project>
```
