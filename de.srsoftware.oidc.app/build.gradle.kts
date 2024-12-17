description = "SRSoftware OIDC: app"

dependencies{
    implementation("org.json:json:20240303")
    implementation("de.srsoftware:tools.http:1.0.1")
    implementation("de.srsoftware:tools.logging:1.0.0")
    implementation("de.srsoftware:tools.optionals:1.0.0")
    implementation("de.srsoftware:tools.util:1.0.3")
    implementation(project(":de.srsoftware.oidc.api"))
    implementation(project(":de.srsoftware.oidc.backend"))
    implementation(project(":de.srsoftware.oidc.datastore.encrypted"))
    implementation(project(":de.srsoftware.oidc.datastore.file"))
    implementation(project(":de.srsoftware.oidc.web"))
}