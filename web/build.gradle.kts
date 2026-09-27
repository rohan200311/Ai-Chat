// Web module is a Vite app, not Gradle. This file exists to satisfy settings.gradle.kts include.
// Real web build is via npm run build in /web.
// If using Kotlin/Wasm Compose, this would be a kotlin multiplatform module with wasmJs target.

plugins {
    id("base")
}

tasks.register("buildWeb") {
    group = "web"
    description = "Build web PWA via npm"
    // In CI: exec npm run build
}
