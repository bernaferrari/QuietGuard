@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose)
}

kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "netguard.js"
            }
        }
        useEsModules()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":shared"))
            // The executable web target must depend on the worker directly so Kotlin/webpack
            // emits the local NPM package used by WebWorkerSQLiteDriver.
            implementation(project(":sqliteWasmWorker"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
        }
    }
}

// Kotlin/JS/Wasm webpack picks up snippets from webpack.config.d/ (merged into generated webpack.config.js).
// COOP/COEP are required so Room's WebWorkerSQLiteDriver can use OPFS / SharedArrayBuffer locally.
// See webpack.config.d/coop-coep-headers.js and webApp/vercel.json for production.

dependencies {
    // Material 3 resolves newer Compose UI than the Compose plugin bundles by default.
    val skikoRuntime = variantOf(libs.skiko.wasm.js) { classifier("skiko-runtime") }
    configurations.matching { it.name == "COMPOSE_SKIKO_JS_WASM_RUNTIME" }.configureEach {
        dependencies.addLater(skikoRuntime)
    }
}
