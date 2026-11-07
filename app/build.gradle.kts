import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

// Release 签名配置：所有真实值只从 keystore.properties 读取。
// 该文件与 *.jks / *.keystore 均已在 .gitignore 中忽略，禁止提交。
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

val releaseStoreFile = keystoreProperties.getProperty("storeFile")?.trim().orEmpty()
val releaseStorePassword = keystoreProperties.getProperty("storePassword")?.trim().orEmpty()
val releaseKeyAlias = keystoreProperties.getProperty("keyAlias")?.trim().orEmpty()
val releaseKeyPassword = keystoreProperties.getProperty("keyPassword")?.trim().orEmpty()
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it.isNotEmpty() }

fun localProperty(key: String, defaultValue: String): String {
    val value = localProperties.getProperty(key)?.trim()
    return if (value.isNullOrEmpty()) defaultValue else value
}

fun String.asBuildConfigLiteral(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val defaultDeepSeekBaseUrl = "https://api.deepseek.com/v1"
val defaultDeepSeekModel = "deepseek-chat"

android {
    namespace = "com.recaitelab.app"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "com.recaitelab.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 451
        versionName = "4.5.1"
    }

    signingConfigs {
        // 只有四项都填写后才创建 release 签名配置；占位为空时不影响 debug 构建。
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                storeType = "JKS"
            }
        }
    }

    buildTypes {
        debug {
            // 调试阶段的 DeepSeek 配置：只从本机 local.properties 注入，绝不提交该文件。
            buildConfigField(
                "String",
                "DEEPSEEK_API_KEY",
                localProperty("DEEPSEEK_API_KEY", "").asBuildConfigLiteral()
            )
            buildConfigField(
                "String",
                "DEEPSEEK_BASE_URL",
                localProperty("DEEPSEEK_BASE_URL", defaultDeepSeekBaseUrl).asBuildConfigLiteral()
            )
            buildConfigField(
                "String",
                "DEEPSEEK_MODEL",
                localProperty("DEEPSEEK_MODEL", defaultDeepSeekModel).asBuildConfigLiteral()
            )
        }
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // 发布包开启 R8 混淆 + 资源裁剪；规则来自默认 optimize 配置与各库 consumer rules。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 发布包不注入调试 Key；正式配置后续由 DataStore 提供。
            buildConfigField("String", "DEEPSEEK_API_KEY", "\"\"")
            buildConfigField(
                "String",
                "DEEPSEEK_BASE_URL",
                defaultDeepSeekBaseUrl.asBuildConfigLiteral()
            )
            buildConfigField(
                "String",
                "DEEPSEEK_MODEL",
                defaultDeepSeekModel.asBuildConfigLiteral()
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
}

ksp {
    // 导出 Room schema，后续版本升级时据此编写 Migration。
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.3.0")
    implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.46.0")
    // T3 标准玻璃：唯一的背景模糊依赖（Android/Compose Multiplatform 通用）
    implementation("dev.chrisbanes.haze:haze:1.7.3")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
