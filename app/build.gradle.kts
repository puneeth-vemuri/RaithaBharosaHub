import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    // KSP configuration
    ksp {
        // Disable incremental processing to work around KSTypeArgument bug
        arg("room.incremental", "false")
        arg("room.expandProjection", "true")
        arg("room.schemaLocation", "$projectDir/schemas")
        // Workaround for KSTypeArgument.type should not have been null bug
        arg("ksp.incremental", "false")
    }
}

kotlin {
    jvmToolchain(17)
}

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
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // Room KSP processor
    add("ksp", libs.androidx.room.compiler)
    
    // SQLCipher
    implementation(libs.sqlcipher.android)
    
    // Hilt
    implementation(libs.hilt.android)
    add("ksp", libs.hilt.android.compiler)
    
    // Hilt Work
    implementation(libs.androidx.hilt.work)
    add("ksp", libs.androidx.hilt.compiler)
    
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation(libs.androidx.work.runtime.ktx)
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

// Kotlin version resolution strategy
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            when (requested.name) {
                "kotlin-stdlib", "kotlin-stdlib-jdk7", "kotlin-stdlib-jdk8", "kotlin-reflect" -> useVersion("1.9.24")
            }
        }
    }
}
