description = "SRSoftware OIDC: file datastore module"

dependencies{
    implementation("com.sun.mail:jakarta.mail:2.0.1")
    implementation("de.srsoftware:tools.optionals:1.0.0")
    implementation("de.srsoftware:tools.util:1.0.3")
    implementation("org.json:json:20240303")

    implementation(project(":de.srsoftware.oidc.api"))
    implementation(project(":de.srsoftware.oidc.web"))
    testImplementation(project(":de.srsoftware.oidc.api","testBundle"))
}

