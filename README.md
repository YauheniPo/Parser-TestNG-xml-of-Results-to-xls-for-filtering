[![Build Status](https://dev.azure.com/YauheniPo/WebTestFramework/_apis/build/status/YauheniPo.Parser-TestNG-xml-of-Results-to-xls-for-filtering?branchName=master)](https://dev.azure.com/YauheniPo/WebTestFramework/_build/latest?definitionId=9&branchName=master)

[![ParseTestNGResultsXml:parse_testng_results package in YaiheniPo feed in Azure Artifacts](https://feeds.dev.azure.com/YauheniPo/_apis/public/Packaging/Feeds/325280d1-3c19-4f4d-b4bd-b0f686103d11/Packages/7124f50d-eea2-4f5c-a119-1e643d49e2bf/Badge)](https://dev.azure.com/YauheniPo/WebTestFramework/_packaging?_a=package&feed=325280d1-3c19-4f4d-b4bd-b0f686103d11&package=7124f50d-eea2-4f5c-a119-1e643d49e2bf&preferRelease=true)


# Parser-TestNG-xml-of-Results-to-xls-for-filtering
Parser TestNG xml of Results to xls for filtering

mvn clean package
    for getting runnable jar file

mvn clean deploy  / -Dregistry=https://maven.pkg.github.com/YauheniPo -Dtoken=GH_TOKEN /
    -DskipTests
    for deploy to git repository

pom.xml
```
<repositories>
    <repository>
        <id>dev-azure-com-yauhenipo-yaihenipo</id>
        <url>https://pkgs.dev.azure.com/YauheniPo/_packaging/YaiheniPo/maven/v1</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependency>
    <groupId>ParseTestNGResultsXml</groupId>
    <artifactId>parse_testng_results</artifactId>
    <version>4.1.0.1.YauheniPo</version>
</dependency>
```

.m/settings.xml:
```
<profiles>
    <profile>
	    <id>dev-azure-com-yauhenipo-yaihenipo</id>
        <repositories>
            <repository>
                <url>https://pkgs.dev.azure.com/YauheniPo/_packaging/YaiheniPo/maven/v1</url>
                <releases>
                    <enabled>true</enabled>
                </releases>
                <snapshots>
                    <enabled>true</enabled>
                </snapshots>
            </repository>
        </repositories>
	</profile>
</profiles>

<activeProfiles>
	<activeProfile>dev-azure-com-yauhenipo-yaihenipo</activeProfile>
</activeProfiles>


<activeProfiles>
    <activeProfile>github</activeProfile>
</activeProfiles>

  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>github</id>
          <name>parse_testng_results</name>
          <url>https://maven.pkg.github.com/YauheniPo/Parser-TestNG-xml-of-Results-to-xls-for-filtering</url>
        </repository>
      </repositories>
    </profile>
  </profiles>
```
