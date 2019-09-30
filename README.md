[![Build Status](https://dev.azure.com/YauheniPo/WebTestFramework/_apis/build/status/YauheniPo.Parser-TestNG-xml-of-Results-to-xls-for-filtering?branchName=master)](https://dev.azure.com/YauheniPo/WebTestFramework/_build/latest?definitionId=9&branchName=master)

# Parser-TestNG-xml-of-Results-to-xls-for-filtering
Parser TestNG xml of Results to xls for filtering

mvn clean package
    for getting runnable jar file

mvn clean -DskipTests deploy
    for deploy to git repository

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
            <version>4.1.YauheniPo</version>
        </dependency>
```
