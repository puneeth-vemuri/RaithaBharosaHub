import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.raithabharosahub"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.raithabharosahub"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val openWeatherApiKey = getLocalProperty(
            "OPENWEATHER_API_KEY",
            getLocalProperty("OWM_API_KEY", "demo_key_placeholder")
        )
        val databasePassphrase = getLocalProperty("DATABASE_PASSPHRASE", "")

        buildConfigField("String", "OWM_API_KEY", "\"$openWeatherApiKey\"")
        buildConfigField("String", "DATABASE_PASSPHRASE", "\"$databasePassphrase\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

kotlin {
    jvmToolchain(17)
}

// Function to read properties from local.properties file
fun getLocalProperty(key: String, defaultValue: String): String {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    return if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
        localProperties.getProperty(key, defaultValue)
    } else {
        defaultValue
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("com.google.android.material:material:1.9.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    add("kapt", libs.androidx.room.compiler)
    
    // SQLCipher for encryption - now enabled
    implementation(libs.sqlcipher.android)
    
    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    add("kapt", libs.hilt.compiler)
    
    // Hilt Work extension for @HiltWorker
    implementation(libs.androidx.hilt.work)
    add("kapt", libs.hilt.work.compiler)
    
    // DataStore for preferences
    implementation(libs.androidx.datastore.preferences)
    
    // Network dependencies
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    
    // WorkManager for background weather refresh
    implementation(libs.androidx.work.runtime.ktx)
    
    // MPAndroidChart for season history charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    testImplementation(libs.junit)
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit-ktx:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Force Kotlin stdlib versions to match the Kotlin Gradle plugin
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            when (requested.name) {
                "kotlin-stdlib",
                "kotlin-stdlib-jdk7",
                "kotlin-stdlib-jdk8",
                "kotlin-reflect" -> useVersion("1.9.24")
            }
        }
    }
}
tasks.matching { it.name.startsWith("kapt") }.configureEach {
    val kaptProcessJvmArgsGetter = javaClass.methods.firstOrNull { method ->
        method.name == "getKaptProcessJvmArgs" && method.parameterCount == 0
    }

    if (kaptProcessJvmArgsGetter != null) {
        @Suppress("UNCHECKED_CAST")
        val kaptProcessJvmArgs = kaptProcessJvmArgsGetter.invoke(this) as? org.gradle.api.provider.ListProperty<String>
        kaptProcessJvmArgs?.addAll(
            listOf(
                "-Djava.io.tmpdir=C:\\Users\\punee\\AppData\\Local\\Temp",
                "-Dorg.sqlite.tmpdir=C:\\Users\\punee\\AppData\\Local\\Temp"
            )
        )
    }
}