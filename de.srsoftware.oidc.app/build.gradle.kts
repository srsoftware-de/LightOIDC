description = "SRSoftware OIDC: app"

plugins {
    java
}

dependencies{
    implementation("org.json:json:20240303")
    implementation("de.srsoftware:tools.http:1.5.4")
    implementation("de.srsoftware:tools.logging:1.2.0")
    implementation("de.srsoftware:tools.optionals:1.0.0")
    implementation("de.srsoftware:tools.util:1.3.1")
    implementation(project(":de.srsoftware.oidc.api"))
    implementation(project(":de.srsoftware.oidc.backend"))
    implementation(project(":de.srsoftware.oidc.datastore.encrypted"))
    implementation(project(":de.srsoftware.oidc.datastore.file"))
    implementation(project(":de.srsoftware.oidc.web"))
}

tasks.jar {
    manifest.attributes["Main-Class"] = "de.srsoftware.oidc.app.Application"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
     val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree) // OR .map { zipTree(it) }
    from(dependencies)
}