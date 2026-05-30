import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.sentry)
}

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) props.load(FileInputStream(f))
}

android {
    namespace = "org.pixelrush.moneyiq"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.pixelrush.moneyiq"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        multiDexKeepProguard = file("multidex-keep.pro")
    }

    // Release signing: reads from env vars set by GitHub Actions (SIGNING_*)
    // or from local.properties (signing.storeFile, etc.) for local builds.
    val storeFilePath: String? = System.getenv("SIGNING_STORE_FILE")
        ?: localProps.getProperty("signing.storeFile")
    val storePass: String? = System.getenv("SIGNING_STORE_PASSWORD")
        ?: localProps.getProperty("signing.storePassword")
    val keyAliasVal: String? = System.getenv("SIGNING_KEY_ALIAS")
        ?: localProps.getProperty("signing.keyAlias")
    val keyPass: String? = System.getenv("SIGNING_KEY_PASSWORD")
        ?: localProps.getProperty("signing.keyPassword")

    if (storeFilePath != null) {
        signingConfigs {
            create("release") {
                storeFile     = file(storeFilePath)
                storePassword = storePass
                keyAlias      = keyAliasVal
                keyPassword   = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (storeFilePath != null)
                signingConfigs.getByName("release") else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    val debugMonoflowUrl   = localProps.getProperty("monoflow.url",   "")
    val debugMonoflowToken = localProps.getProperty("monoflow.token", "")

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            buildConfigField("String", "DEBUG_MONOFLOW_URL",   "\"$debugMonoflowUrl\"")
            buildConfigField("String", "DEBUG_MONOFLOW_TOKEN", "\"$debugMonoflowToken\"")
        }
        release {
            buildConfigField("String", "DEBUG_MONOFLOW_URL",   "\"\"")
            buildConfigField("String", "DEBUG_MONOFLOW_TOKEN", "\"\"")
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.coroutines.android)

    // Charts
    implementation(libs.mpandroidchart)

    // DataStore
    implementation(libs.datastore.preferences)

    // Glance (виджеты рабочего стола)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // WorkManager (обновление виджетов)
    implementation(libs.work.runtime.ktx)

    // Biometric
    implementation(libs.biometric)

    // Sentry
    implementation(libs.sentry.android)

    // Testing — unit
    testImplementation(libs.junit)
    testImplementation("org.json:json:20231013")
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit.ktx)

    // Testing — instrumented
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.coroutines.test)
}

val sentryToken: String = System.getenv("SENTRY_AUTH_TOKEN")
    ?: localProps.getProperty("sentry.auth.token", "")

sentry {
    includeSourceContext = true
    org = "serg-yalosovetsky"
    projectName = "one_money"
    authToken = sentryToken
}

tasks.register("printVersionName") {
    doLast { println(android.defaultConfig.versionName) }
}
