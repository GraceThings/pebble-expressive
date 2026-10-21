import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

val properties = Properties().apply {
    try {
        load(rootDir.resolve("local.properties").reader())
    } catch (e: Exception) {
        println("local.properties file not found")
    }
}
val localReleaseBuild = properties["LOCAL_RELEASE_BUILD"]?.toString()?.toBooleanStrictOrNull() ?: false

// Most recent tag reachable from HEAD, so a release branch versions from its own tag.
val gitVersionName = providers.exec {
    isIgnoreExitValue = true
    commandLine("git", "describe", "--tags", "--abbrev=0", "HEAD")
}.standardOutput.asText.map { out ->
    val trimmed = out.trim()
    val isTagFormat = trimmed.isNotEmpty() && trimmed.split('.').all { it.toIntOrNull() != null }
    if (isTagFormat) trimmed else "1.0.0.0"
}

// Tag as an increasing int: 1.9.1.3 -> 10901003. Major must stay below 100, the rest below 1000.
val gitVersionCode = gitVersionName.map { name ->
    val parts = name.split('.').map { it.toIntOrNull() ?: -1 }
    if (parts.size > 4 || parts.first() !in 0..99 || parts.any { it !in 0..999 }) {
        10_000_000
    } else {
        listOf(10_000_000, 100_000, 1_000, 1).zip(parts) { scale, part -> scale * part }.sum()
    }
}

android {
    namespace = "coredevices.coreapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    val releaseKeystore = file("../keystore.jks")
    val hasReleaseKeystore = !localReleaseBuild && releaseKeystore.exists() && !System.getenv("RELEASE_KEYSTORE_PASSWORD").isNullOrEmpty()
    if (hasReleaseKeystore) {
        signingConfigs {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEYSTORE_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "coredevices.coreapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += setOf("armeabi-v7a", "arm64-v8a")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            if (localReleaseBuild || !hasReleaseKeystore) {
                // Crashlytics regenerates a mapping-id resource every build
                // (upToDateWhen=false), forcing aapt + a full R8 rerun even on
                // null builds. Skip it for local release builds.
                configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                    mappingFileUploadEnabled = false
                }
            }
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":composeApp"))
    // Components this module's manifest declares, so lint can resolve them.
    implementation(project(":util"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work)
    implementation(libs.health.kmp)

    androidTestImplementation(platform(libs.firebase.bom))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.ktor.client.okhttp)
    androidTestImplementation(libs.koin.core)
    androidTestImplementation(libs.koin.android)
    androidTestImplementation(libs.coroutines)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.firebase.auth)
    androidTestImplementation(project(":cactus"))
    androidTestImplementation(project(":experimental"))
    androidTestImplementation(project(":libindex"))
    androidTestImplementation(project(":index-ai"))
    androidTestImplementation(project(":mcp"))
}

// Resolved at execution time — a configuration-time .get() makes every commit invalidate the
// configuration cache.
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach {
            it.versionCode.set(gitVersionCode)
            it.versionName.set(gitVersionName)
        }
    }
}

/**
 * Packages a plugin API demo into this app's assets and the folder the iOS app bundles from, so
 * reinstalling the phone app refreshes it on the watch. `scripts/pack-plugin-pbw.py` inspects the
 * project: a watchapp (has `src/c`) is built with the Pebble SDK then has its plugin files injected;
 * a plugin-only project is packaged directly, needing no SDK.
 */
fun registerAppBuild(name: String) =
    tasks.register<Exec>("build${name.replaceFirstChar { it.uppercase() }}Pbw") {
        val appDir = file("../test-apps/$name")
        val pbw = File(appDir, "build/$name.pbw")
        val androidAsset = file("src/main/assets/bundled-apps/$name.pbw")
        val iosResource = file("../iosApp/bundled-apps/$name.pbw")
        val packScript = file("../scripts/pack-plugin-pbw.py")
        val isWatchapp = File(appDir, "src/c").isDirectory
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val pebbleInstalled = System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .any { dir ->
                if (isWindows) {
                    listOf("pebble.bat", "pebble.cmd", "pebble.exe", "pebble").any { File(dir, it).exists() }
                } else {
                    File(dir, "pebble").canExecute()
                }
            }
        val pythonExecutable = if (isWindows) "python" else "python3"
        val pythonInstalled = System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .any { dir ->
                if (isWindows) {
                    listOf("python.exe", "py.exe", "python3.exe").any { File(dir, it).exists() }
                } else {
                    listOf("python3", "python").any { File(dir, it).canExecute() }
                }
            }

        inputs.file(File(appDir, "package.json")).withPropertyName("manifest")
        inputs.files(fileTree(appDir).matching { include("*.html", "*.js") }).withPropertyName("pluginFiles")
        inputs.file(packScript).withPropertyName("packScript")
        if (isWatchapp) inputs.dir(File(appDir, "src")).withPropertyName("source")
        outputs.files(androidAsset, iosResource).withPropertyName("bundled")

        // Plugin-only pbws always build if Python is available; a watchapp demo is skipped when the SDK is absent, and the
        // app simply ships without it.
        onlyIf("Python is installed and (the Pebble SDK is installed, or there is no watchapp to build)") {
            pythonInstalled && (!isWatchapp || pebbleInstalled)
        }

        executable = pythonExecutable
        args(packScript.absolutePath, appDir.absolutePath)
        doLast {
            if (pbw.exists()) {
                listOf(androidAsset, iosResource).forEach { destination ->
                    destination.parentFile.mkdirs()
                    pbw.copyTo(destination, overwrite = true)
                }
            }
        }
    }

// spotify is intentionally not built/bundled yet — no prod dev account to sign in against.
val testApps = listOf("plugin-test", "weather-face", "met-weather", "hue", "stocks", "notion", "ticktick", "todoist")
val testAppPbws = testApps.map { registerAppBuild(it) }

// waf self-extracts its library to ~/.waf3-* on first run; two concurrent waf processes
// racing that unpack die with "cannot import name 'Scripting' from 'waflib'". The builds
// take ~1s each, so just serialize them.
testAppPbws.zipWithNext().forEach { (first, second) -> second.configure { mustRunAfter(first) } }

// Everything a demo generates lands outside this project's build dir, so `clean` has to be told
// about it: the build tree in the app itself, and the pbws it installed.
tasks.named<Delete>("clean") {
    testApps.forEach {
        delete(
            file("../test-apps/$it/build"),
            file("src/main/assets/bundled-apps/$it.pbw"),
            file("../iosApp/bundled-apps/$it.pbw"),
        )
    }
}

tasks.register("buildTestAppPbws") {
    description = "Builds every plugin API demo watchapp/plugin into the host apps' resources."
    dependsOn(testAppPbws)
}

// The pbws ship in this app's assets, so anything reading that dir — asset merging, and lint's
// model of the source sets — has to run after they land.
tasks.matching { it.name.contains("Assets") || it.name.contains("lint", ignoreCase = true) }
    .configureEach { dependsOn(testAppPbws) }


googleServices {
    missingGoogleServicesStrategy = MissingGoogleServicesStrategy.IGNORE
}
