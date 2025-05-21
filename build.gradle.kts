import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.gradle.download.task)
}

group = "com.jixin.translato"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

tasks.register<Copy>("copyComposeResources") {
    from(layout.projectDirectory.dir("src/main/composeResources"))
    into(layout.buildDirectory.dir("copiedComposeResources"))
}

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadAdbZip") {
    dependsOn("copyComposeResources")

    // 下载地址可以在这个url找到 https://dl.google.com/android/repository/repository2-2.xml
    val windowsUrl = "platform-tools_r36.0.0-win.zip"
    val linuxUrl = "platform-tools_r36.0.0-linux.zip"
    val macOSUrl = "platform-tools_r36.0.0-darwin.zip"

    val os = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()

    val (url, targetDir) = when {
        os.isWindows -> windowsUrl to "windows"
        os.isLinux -> linuxUrl to "linux"
        os.isMacOsX -> macOSUrl to "macos"
        else -> throw GradleException("Unsupported System.")
    }

    src("https://dl.google.com/android/repository/$url")
    dest(layout.buildDirectory.file("copiedComposeResources/files/$targetDir/adb.zip"))
    overwrite(false)
}

afterEvaluate {
    tasks.getByName("convertXmlValueResourcesForMain").dependsOn("downloadAdbZip")
    tasks.getByName("copyNonXmlValueResourcesForMain").dependsOn("downloadAdbZip")
    tasks.getByName("prepareComposeResourcesTaskForMain").dependsOn("downloadAdbZip")
}

compose.resources {
    customDirectory(
        sourceSetName = "main",
        directoryProvider = layout.buildDirectory.dir(
            "copiedComposeResources"
        )
    )
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.components.resources)
    implementation(libs.kotlinx.serialization)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    // Material 3
    implementation(compose.material3)
    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ToolBox"
            packageVersion = "1.0.0"
        }
    }
}
