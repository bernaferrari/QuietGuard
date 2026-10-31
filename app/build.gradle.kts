import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.Sync

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasKeystore = keystorePropertiesFile.exists().also { exists ->
    if (exists) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }
}

val rustDirectory = layout.projectDirectory.dir("src/main/rust")
val uniffiOutputDirectory = layout.buildDirectory.dir("generated/source/uniffi")
val hostRustTargetDirectory = layout.buildDirectory.dir("rustHost")
val uniffiBindgenTargetDirectory = layout.buildDirectory.dir("rustBindgen")
val androidRustTargetDirectory = layout.buildDirectory.dir("rustAndroid")
val hostRustLibrary = hostRustTargetDirectory.map { it.file("release/${System.mapLibraryName("netguard")}") }
val androidNdkVersion =
    (findProperty("quietguard.ndkVersion") as String?)
        ?: error("Set quietguard.ndkVersion in gradle.properties")
val rustSources = fileTree(rustDirectory) {
    include("Cargo.toml", "Cargo.lock", "build.rs", "uniffi.toml", "src/**")
}
data class AndroidRustTarget(val cargoTarget: String, val clangTarget: String)
val androidRustTargets = mapOf(
    "armeabi-v7a" to AndroidRustTarget("armv7-linux-androideabi", "armv7a-linux-androideabi"),
    "arm64-v8a" to AndroidRustTarget("aarch64-linux-android", "aarch64-linux-android"),
    "x86" to AndroidRustTarget("i686-linux-android", "i686-linux-android"),
    "x86_64" to AndroidRustTarget("x86_64-linux-android", "x86_64-linux-android"),
)
// CI uses -Pquietguard.abis=arm64-v8a to skip unused NDK/Cargo targets.
val selectedAndroidAbis: List<String> = run {
    val raw = (findProperty("quietguard.abis") as String?)?.trim().orEmpty()
    val requested = if (raw.isEmpty()) {
        androidRustTargets.keys.toList()
    } else {
        raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
    val unknown = requested.filter { it !in androidRustTargets }
    require(unknown.isEmpty()) {
        "Unknown quietguard.abis value(s): ${unknown.joinToString()}. " +
            "Valid: ${androidRustTargets.keys.joinToString()}"
    }
    requested
}
val rustJniDirectory = layout.buildDirectory.dir("generated/rustJniLibs")
val buildUniffiLibrary by tasks.registering(Exec::class) {
    workingDir(rustDirectory)
    inputs.files(rustSources)
    outputs.file(hostRustLibrary)
    environment("CARGO_TARGET_DIR", hostRustTargetDirectory.get().asFile)
    commandLine("cargo", "build", "--release")
}
val generateUniffiKotlin by tasks.registering(Exec::class) {
    dependsOn(buildUniffiLibrary)
    workingDir(rustDirectory)
    inputs.file(hostRustLibrary)
    inputs.files(fileTree(rustDirectory.dir("bindgen")) { include("Cargo.toml", "Cargo.lock", "src/**") })
    outputs.dir(uniffiOutputDirectory)
    val outputDirectory = uniffiOutputDirectory.get().asFile
    doFirst { outputDirectory.mkdirs() }
    environment("CARGO_TARGET_DIR", uniffiBindgenTargetDirectory.get().asFile)
    commandLine(
        "cargo",
        "run",
        "--quiet",
        "--manifest-path",
        rustDirectory.file("bindgen/Cargo.toml").asFile.absolutePath,
        "--",
        "generate",
        hostRustLibrary.get().asFile.absolutePath,
        "--language",
        "kotlin",
        "--out-dir",
        uniffiOutputDirectory.get().asFile.absolutePath,
        "--no-format",
    )
}

tasks.withType<KotlinCompile>().configureEach {
    // Bindings belong to production compilations, not the empty app test source sets.
    if (!name.contains("Test")) {
        dependsOn(generateUniffiKotlin)
        source(uniffiOutputDirectory)
    }
}

android {
    namespace = "com.bernaferrari.quietguard"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "com.bernaferrari.quietguard"
        versionName = "1.0"
        minSdk = 26
        targetSdk = 36
        versionCode = 1

        ndkVersion = androidNdkVersion
        ndk {
            abiFilters += selectedAndroidAbis
        }

        packaging {
            jniLibs {
                useLegacyPackaging = true
            }
        }
    }

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = false
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(file("proguard-rules.pro"))
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            buildConfigField("boolean", "PLAY_STORE_RELEASE", "false")
            buildConfigField(
                "String",
                "HOSTS_FILE_URI",
                "\"https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts\"",
            )
            buildConfigField(
                "String",
                "GITHUB_LATEST_API",
                "\"https://api.github.com/repos/bernaferrari/QuietGuard/releases/latest\"",
            )
        }
        create("play") {
            isMinifyEnabled = true
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(file("proguard-rules.pro"))
            buildConfigField("boolean", "PLAY_STORE_RELEASE", "true")
            buildConfigField("String", "HOSTS_FILE_URI", "\"\"")
            buildConfigField("String", "GITHUB_LATEST_API", "\"\"")
        }
        getByName("debug") {
            isMinifyEnabled = false

            proguardFiles(file("proguard-rules.pro"))
            buildConfigField("boolean", "PLAY_STORE_RELEASE", "false")
            buildConfigField(
                "String",
                "HOSTS_FILE_URI",
                "\"https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts\"",
            )
            buildConfigField(
                "String",
                "GITHUB_LATEST_API",
                "\"https://api.github.com/repos/bernaferrari/QuietGuard/releases/latest\"",
            )
        }
        // Store screenshots only: fictional apps and traffic from src/storeCapture, no VPN, no network.
        // Installs beside the real app; never built for release.
        create("storeCapture") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".storecapture"
            versionNameSuffix = "-storecapture"
            matchingFallbacks += "debug"
            buildConfigField("String", "HOSTS_FILE_URI", "\"\"")
            buildConfigField("String", "GITHUB_LATEST_API", "\"\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        aidl = true
        compose = true
        buildConfig = true
    }

    lint {
        disable.add("MissingTranslation")
    }

    sourceSets.getByName("main").jniLibs.directories.add(
        rustJniDirectory.get().asFile.absolutePath,
    )
}

// Cargo produces the JNI library directly. Resolve the NDK only when an Android
// native task runs: KMP web tasks must configure on machines without an Android SDK.
val androidNdkToolchain = providers.provider {
    val sdkDirectory = System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: rootProject.file("local.properties").takeIf(File::isFile)?.let { localProperties ->
            Properties().also { localProperties.inputStream().use(it::load) }.getProperty("sdk.dir")
        }
        ?: error("Set ANDROID_HOME or sdk.dir to build Android Rust libraries")
    file("$sdkDirectory/ndk/$androidNdkVersion")
        .resolve("toolchains/llvm/prebuilt")
        .listFiles()
        ?.singleOrNull { it.resolve("bin").isDirectory }
        ?: error("Android NDK LLVM toolchain is unavailable")
}

val buildRustLibraries = selectedAndroidAbis.map { abi ->
    val target = androidRustTargets.getValue(abi)
    tasks.register<Exec>("buildRust${abi.replace("-", "").replaceFirstChar(Char::uppercase)}") {
        val library = androidRustTargetDirectory.map {
            it.file("${target.cargoTarget}/release/libnetguard.so")
        }
        workingDir(rustDirectory)
        inputs.files(rustSources)
        outputs.file(library)
        environment("CARGO_TARGET_DIR", androidRustTargetDirectory.get().asFile)
        doFirst {
            environment(
                "CARGO_TARGET_${target.cargoTarget.uppercase().replace('-', '_')}_LINKER",
                androidNdkToolchain.get().resolve("bin/${target.clangTarget}26-clang"),
            )
        }
        commandLine("cargo", "build", "--quiet", "--release", "--target", target.cargoTarget)
    }
}
val packageRustJni by tasks.registering(Sync::class) {
    dependsOn(buildRustLibraries)
    into(rustJniDirectory)
    selectedAndroidAbis.forEach { abi ->
        val target = androidRustTargets.getValue(abi)
        from(androidRustTargetDirectory.map { it.dir("${target.cargoTarget}/release") }) {
            include("libnetguard.so")
            into(abi)
        }
    }
}
tasks.configureEach {
    if (name.matches(Regex("merge.*(?:JniLibFolders|NativeLibs)"))) {
        dependsOn(packageRustJni)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.appfunctions)
    implementation("net.java.dev.jna:jna:5.19.1@aar")
    ksp(libs.androidx.appfunctions.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
}

ksp {
    arg("appfunctions:aggregateAppFunctions", "true")
}
