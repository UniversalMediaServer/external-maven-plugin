# Substitution Tokens #

| **Token** | **Value** |
|:----------|:----------|
| {groupId} | The artifact's group ID specified in 

&lt;groupId&gt;

  |
| {artifactId} | The artifact's group ID specified in 

&lt;artifactId&gt;

 |
| {version} |  The artifact's version specified in 

&lt;version&gt;

 |
| {`_`version} | The artifact's version specified in 

&lt;version&gt;

 with the period characters replaced with underscores |
| {packaging} | The artifact's packaging specified in 

&lt;packaging&gt;

 if one was specified |
| {classifier} | The artifact's classifier specified in 

&lt;classifier&gt;

 if one was specified |


---

# Supported Properties #

The following properties of an  < artifactItem >  support the token substitutions listed above.

> 

&lt;stagingDirectory&gt;


> 

&lt;localFile&gt;


> 

&lt;downloadUrl&gt;


> 

&lt;extractFile&gt;




---

# Example #
```

<!-- HERE IS AN EXAMPLE OF A FILE USING A CLASSIFIER 
	 AND A CHECKSUM VERIFICATION ON THE DOWNLOADED FILE -->
<artifactItem>
	<groupId>org.apache.ant</groupId>
	<artifactId>apache-ant</artifactId>
	<version>1.8.0</version>
	<classifier>bin</classifier>
	<packaging>zip</packaging>
	<downloadUrl>
	   <!-- http://apache.mirrors.timporter.net/ant/binaries/apache-ant-1.8.0-bin.zip -->
           http://apache.mirrors.timporter.net/ant/binaries/{artifactId}-{version}-{classifier}.{packaging}
	</downloadUrl>                        
	<checksum>025836cd51474bd3bbda6f74a1168e092a670363</checksum>                        
</artifactItem>


<!-- HERE IS AN EXAMPLE OF AN ARTIFACT 
	 EXTRACTED FROM A ZIP FILE -->
<artifactItem>
	<groupId>com.google.code</groupId>
	<artifactId>tweener</artifactId>
	<version>1.33.74</version>
	<packaging>swc</packaging>
        <classifier>as3</classifier>
	<downloadUrl>
	   <!-- http://tweener.googlecode.com/files/tweener_1_33_74_as3_swc.zip -->
           http://tweener.googlecode.com/files/{artifactId}_{_version}_{classifier}_{packaging}.zip
	</downloadUrl>                        
	<extractFile>tweener.swc</extractFile>                        
</artifactItem>

```


---


Please visit this link for a complete example of a POM file configured for this this plugin: 

&lt;BR&gt;


http://code.google.com/p/maven-external-dependency-plugin/source/browse/trunk/maven-external-dependency-plugin-test/pom.xml