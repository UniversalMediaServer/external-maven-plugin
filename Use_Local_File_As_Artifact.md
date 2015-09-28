# Details #

If you need to resolve, install, and deploy an artifact that is located on your local file system rather than downloading a file from a website, you can use the file protocol syntax to specify a file on the local filesystem.


&lt;BR&gt;


```
file:///C:/install/google-api-translate-java/0.92/google-api-translate-java.jar
```


&lt;BR&gt;

See the artifact item example below:
```

<!-- THIS JAR IS NOT AVAILABLE IN A MAVEN REPO
     USE JAR FILE FROM LOCAL FILE SYSTEM -->
<artifactItem>
    <groupId>com.google.code</groupId>
    <artifactId>google-api-translate-java</artifactId>
    <version>0.92</version>
    <packaging>jar</packaging>
    <downloadUrl>
        file:///C:/install/google-api-translate-java/0.92/google-api-translate-java.jar
    </downloadUrl>                        
</artifactItem>

```



---
