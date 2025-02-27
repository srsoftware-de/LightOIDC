description = "SRSoftware OIDC: sqlite datastore module"

dependencies{
    implementation("com.sun.mail:jakarta.mail:2.0.1")
    implementation("de.srsoftware:tools.optionals:1.0.0")
    implementation("de.srsoftware:tools.result:1.0.0")
    implementation("de.srsoftware:tools.util:1.0.2")
    implementation("org.json:json:20240303")
    implementation("org.bitbucket.b_c:jose4j:0.9.6")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")

    implementation(project(":de.srsoftware.oidc.api"))
    implementation(project(":de.srsoftware.oidc.web"))
    testImplementation(project(":de.srsoftware.oidc.api","testBundle"))
}

