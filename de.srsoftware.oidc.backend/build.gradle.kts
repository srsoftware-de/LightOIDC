description = "SRSoftware OIDC: backend"

dependencies{
    implementation("com.sun.mail:jakarta.mail:2.0.1")
    implementation("de.srsoftware:tools.http:1.5.4")
    implementation("de.srsoftware:tools.optionals:1.0.0")
    implementation("de.srsoftware:tools.result:1.0.0")
    implementation("de.srsoftware:tools.util:1.3.1")
    implementation("org.bitbucket.b_c:jose4j:0.9.6")
    implementation("org.json:json:20240303")

    implementation(project(":de.srsoftware.oidc.api"))
}

