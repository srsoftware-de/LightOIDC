description = "SRSoftware OIDC: encrypted datastore module"

dependencies{
    implementation("com.sun.mail:jakarta.mail:2.0.1")
    implementation("de.srsoftware:tools.optionals:1.0.0")
    implementation("de.srsoftware:tools.util:1.0.3")

    implementation(project(":de.srsoftware.oidc.api"))
    testImplementation(project(":de.srsoftware.oidc.api","testBundle"))
}

