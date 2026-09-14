/*
 * YunX (云析) Desktop - Windows port of CYQawa/YunX (Android).
 * Copyright (C) 2026 CYQawa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.2"
    id("com.google.devtools.ksp") version "2.1.21-2.0.1"
}

repositories {
    mavenCentral()
    google()
    // KCEF (JCEF) 依赖 JOGL 运行时
    maven("https://jogamp.org/deployment/maven")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

    // 网络（与上游 Android 版一致）
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20250517")

    // Room KMP（桌面端）
    implementation("androidx.room:room-runtime:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("androidx.sqlite:sqlite-bundled:2.5.2")

    // JCEF 封装（内嵌登录浏览器）
    implementation("dev.datlag:kcef:2025.03.23")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
    }
}

compose.desktop {
    application {
        mainClass = "com.yunx.app.MainKt"

        buildTypes.release.proguard {
            isEnabled.set(false) // KCEF/JCEF 需要保留类名，关闭混淆
        }

        nativeDistributions {
            // Windows 打包产物（在 Windows 上执行 gradlew packageReleaseMsi / packageReleaseExe）
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "YunX"
            packageVersion = "1.2.6"
            description = "云析 YunX - 网盘分享链接解析与高速下载（夸克/百度）"
            vendor = "YunX Desktop"
            copyright = "Copyright (C) 2026 CYQawa, AGPL-3.0"

            windows {
                menu = true
                dirChooser = true
                upgradeUuid = "7e7d1b9a-2f2f-4c9f-973d-e1e9cf20b8f7"
                console = false
            }

            // JCEF / CEF 在打包运行时需要的 JVM 参数
            jvmArgs(
                "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "-Dsun.awt.noerasebackground=true"
            )

            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
        }
    }
}
