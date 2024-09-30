# LightOIDC

<table>
    <tr>
        <th>Deutsch</th>
        <th>English</th>
    </tr>
    <tr>
        <td>

LightOIDC ist ein [Spezifikations][specification]-treuer OpenID-Connect-Provider mit minimalem Fußabdruck.

Dieses Ziel wird durch Minimierung der externen Abhängigkeiten auf ein absolutes Minimum erreicht:

Im Moment baut das Projekt nur auf die folgenden Laufzeit-Bibliotheken auf:

* [org.json:json](https://github.com/douglascrockford/JSON-java)
* [org.bitbucket.b_c:jose4j](https://bitbucket.org/b_c/jose4j)
* [com.sun.mail:jakarta.mail](https://projects.eclipse.org/projects/ee4j.mail)
* [com.sun.activation:jakarta.activation](https://projects.eclipse.org/projects/ee4j.jaf)

Im Moment haben diese Bibliotheken keine weiteren (transitiven) Anhängigkeiten, so dass das Projekt nicht durch eine Kaskade von Libraries aufgeblasen wird.

Das Ermöglicht es, dass die compilierte JAR-Datei weniger als 1,5 MB groß ist!

</td>
<td>

This aims to be a [specification] compliant OpenID connect provider with minimal footprint.

This goal is achieved by reducing external library dependiencies to an absolute minimum.

Currently, this project only depends on the following runtime libraries:

* [org.json:json](https://github.com/douglascrockford/JSON-java)
* [org.bitbucket.b_c:jose4j](https://bitbucket.org/b_c/jose4j)
* [com.sun.mail:jakarta.mail](https://projects.eclipse.org/projects/ee4j.mail)
* [com.sun.activation:jakarta.activation](https://projects.eclipse.org/projects/ee4j.jaf)

At the time of writing, these libraries have no further transitive dependencies, thus mitigating any bloat from the project.

As a result, the compiled jar has a size of less than 1.5 MB!

</td>
</tr>
</table>

## build

<table>
    <tr>
        <th>Deutsch</th>
        <th>English</th>
    </tr>
    <tr>
        <td>

Dies ist ein Gradle-Project. Um es zu compilieren brauchen Sie ein aktuelles Java-Development-Kit. 

Das Projekt kann durch Aufruf von `./gradlew build` in einem Terminal innerhalb des Wurzelverzeichnisses gebaut werden.

</td>
<td>

This is a gradle project. To compile it, you should have a recent version of a Java Development Kit installed.

Build the project by launching `./gradlew build` in a terminal while being in the root folder of the project.

</td>
</tr>
</table>

This is a gradle project. To compile it, you should have a recent version of a Java Development Kit installed.

Build the project by launching `./gradlew build` in a terminal while being in the root folder of the project.

[specification]: https://openid.net/specs/openid-connect-core-1_0.html