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

## Motivation

Natürlich gibt es freie Implementierungen des OIDC-Protokolls.
Der Platzhirsch auf dem Open-Source-Markt dürfte wohl [Keycloak] sein.
Allerdings ist Keykloak alles andere als einfach zu konfigurieren und dürfte wohl für viele Nutzer deutlich mehr Features als Ballast mitbringen, als eigentlich benötigt werden.

Deshalb war das Ziel hier:

* kein [Feature-Creep](https://datei.wiki/definition/die-grundlagen-des-feature-creep/)
* kein NodeJS – und damit keine Dependency-Hölle
* Platformunabhängig dank Java
* erweiterbar
* verschlüsselte Datenbank

## bauen

Dies ist ein Gradle-Project. Um es zu compilieren brauchen Sie ein aktuelles Java-Development-Kit.  
Das Projekt kann durch Aufruf von `./gradlew build` in einem Terminal innerhalb des Wurzelverzeichnisses gebaut werden.

## Backends

Im Main-Branch ist kein Datenbank-Backend enthalten.
Alle Einstellungen werden in einer JSON-Datei gespeichert, wobei eine Verschlüsselung einfach konfiguriert werden kann.
Dieses Setup sollte für kleine und mittlere Instanzen reichen.

### Datenbank-Unterstützung

Um das Projekt klein zu halten ist im _main_-Branch kein Datenbank-Support eingebaut.
Es gibt aber einen separaten Branch, der die Benutzung von SQLite-Datenbanken untersützt: [sqlite]

Die Anbindung an andere Datenbanksysteme ist möglich, im Moment aber noch nicht implementiert.

### andere Backends

Aufgrund der Architektur des OIDC-Providers sollte es jederzeit möglich sein weitere Backends, wie z.B. LDAP anzubinden.
Dies ist aber im Moment noch nicht implementiert – und wird wohl erst auf Nachfrage implementiert werden.


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

## Motivation

Of course, there are plenty other implementations of the OIDC protocol.
The most well-known open source OIDC provider might be [Keycloak].
However, that piece of software called Keycloak is really heavy duty, hard to configure and bloated with features the most users won`t need.

Thus, the goal for LightOIDC was:

* don`t be a feature creep
* don`t use NodeJS – avoid the dependency hell
* be platform neutral by using Java
* be extensible
* allow data to be encrypted

## build

This is a gradle project. To compile it, you should have a recent version of a Java Development Kit installed.  
Build the roject by launching `./gradlew build` in a terminal while being in the root folder of the project.

## backends

In the main branch, there ist no database backend.
Alle preferences and data are stored in a JSON file, allowing for easy encryption of data.
This setup should be fine for small and medium instances.

### database support

In order to achieve a minimal footprint, no database support is incorporated in the main branch.
However, there is SQLite support in a separate branch: [sqlite]

Utilizing other databases should be possible, but has not been implemented, yet.

### other backends

LightOIDCs architecture shout allow integration other backends, like LDAP, without a hazzle.
However, this is not implemented yet – work will have to be done on demand!

</td>
</tr>
</table>

[main]: ../main
[Keycloak]: https://www.keycloak.org/
[specification]: https://openid.net/specs/openid-connect-core-1_0.html
