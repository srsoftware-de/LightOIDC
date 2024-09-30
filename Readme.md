# LightOIDC

<table>
    <tr>
        <th>Deutsch</th>
        <th>English</th>
    </tr>
    <tr>
        <td>

LightOIDC ist ein [Spezifikations][specification]-treuer OpenID-Connect-Provider mit minimalem Fußabdruck.  
Dieses Ziel wird durch Minimierung der externen Abhängigkeiten auf ein absolutes Minimum erreicht.
Im Moment baut das Projekt nur auf die folgenden Laufzeit-Bibliotheken auf:

* [org.json:json](https://github.com/douglascrockford/JSON-java)
* [org.bitbucket.b_c:jose4j](https://bitbucket.org/b_c/jose4j)
* [com.sun.mail:jakarta.mail](https://projects.eclipse.org/projects/ee4j.mail)
* [org.xerial:sqlite-jdbc](https://github.com/xerial/sqlite-jdbc)

Im Gegensatz zum [Main-Branch][main], der keine DB-Abhängigkeiten enthält, 
 bietet dieser Branch Support für einen SQLite-Datenspeicher.
Leider werden durch die SQLite-Bibliothek weitere Transitive Abhängigkeiten eingebunden, 
 die das JAR ein wenig aufblähen.

## bauen

Dies ist ein Gradle-Project. Um es zu compilieren brauchen Sie ein aktuelles Java-Development-Kit.  
Das Projekt kann durch Aufruf von `./gradlew build` in einem Terminal innerhalb des Wurzelverzeichnisses gebaut werden.

</td><td>

This aims to be a [specification] compliant OpenID connect provider with minimal footprint.  
This goal is achieved by reducing external library dependiencies to an absolute minimum.  
Currently, this project only depends on the following runtime libraries:

* [org.json:json](https://github.com/douglascrockford/JSON-java)
* [org.bitbucket.b_c:jose4j](https://bitbucket.org/b_c/jose4j)
* [com.sun.mail:jakarta.mail](https://projects.eclipse.org/projects/ee4j.mail)
* [org.xerial:sqlite-jdbc](https://github.com/xerial/sqlite-jdbc)

While the [main] branch does not contain any dependencies for database support,
 this branch _does_ allow storing data in an SQLite database.
Unfortunately the SQLite library also draws in some additional dependencies,
 which – to a certain extend – increases the size of the compiled JAR archive.

## build

This is a gradle project. To compile it, you should have a recent version of a Java Development Kit installed.  
Build the project by launching `./gradlew build` in a terminal while being in the root folder of the project.

</td>
</tr>
</table>

[main]: ../main
[specification]: https://openid.net/specs/openid-connect-core-1_0.html