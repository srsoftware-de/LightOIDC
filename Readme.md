# LightOIDC

This aims to be a [specification] compliant OpenID connect provider with minimal footprint.

This goal is achieved by reducing external library dependiencies to an absolute minimum.

Currently, this project only depends on the following libraries:

* [org.json:json](https://github.com/douglascrockford/JSON-java)
* [org.bitbucket.b_c:jose4j](https://bitbucket.org/b_c/jose4j)

At the time of writing, these libraries have no further transitive dependencies, this mitigating any bloat from the project.

[specification]: https://openid.net/specs/openid-connect-core-1_0.html