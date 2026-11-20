import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// ── 웹 앱(GrowthLens) 묶기 ───────────────────────────────────────────────
// local.properties 설정
//   growthlens.dir=/path/to/growthLens   웹 프로젝트 폴더 (필수)
//   growthlens.npm=/path/to/npm          npm 위치 (없으면 흔한 위치에서 찾음)
//   growthlens.skipNpmBuild=true         npm 빌드 생략, 이미 있는 dist/ 만 복사
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val webDir: File = file(
    localProps.getProperty("growthlens.dir")
        ?: error("local.properties 에 growthlens.dir=<웹 프로젝트 경로> 를 설정하세요")
)
val skipNpmBuild = localProps.getProperty("growthlens.skipNpmBuild")?.toBoolean() ?: false
val npmPath: String = localProps.getProperty("growthlens.npm")
    ?: listOf("/opt/homebrew/bin/npm", "/usr/local/bin/npm", "/usr/bin/npm")
        .firstOrNull { File(it).canExecute() }
    ?: "npm"

// Android Studio에서 실행하면 PATH에 node가 없을 수 있어서 npm 폴더를 앞에 붙임
val buildWeb = tasks.register<Exec>("buildWeb") {
    group = "growthlens"
    description = "웹 앱을 npm run build 로 빌드합니다 (dist/)"
    workingDir = webDir
    commandLine(npmPath, "run", "build")
    File(npmPath).parentFile?.let { environment("PATH", "${it.absolutePath}:${System.getenv("PATH")}") }
    inputs.dir(webDir.resolve("src"))
    inputs.dir(webDir.resolve("public"))
    inputs.files(
        listOf("index.html", "package.json", "package-lock.json", "vite.config.ts",
            "tailwind.config.js", "postcss.config.js", "tsconfig.app.json")
            .map(webDir::resolve).filter(File::exists)
    )
    outputs.dir(webDir.resolve("dist"))
    enabled = !skipNpmBuild
}

abstract class CopyWebAssetsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val distDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun copy() {
        fs.sync {
            from(distDir) { into("www") }
            into(outputDir)
        }
    }
}

val copyWebAssets = tasks.register<CopyWebAssetsTask>("copyWebAssets") {
    group = "growthlens"
    description = "웹 빌드 결과(dist/)를 앱 assets/www 로 복사합니다"
    dependsOn(buildWeb)
    distDir.set(webDir.resolve("dist"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyWebAssets, CopyWebAssetsTask::outputDir)
    }
}
// ────────────────────────────────────────────────────────────────────────

android {
    namespace = "com.example.bebeview"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.bebeview"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        // WASM·모델 파일(약 56MB)은 압축하지 않아야 읽기가 빠름
        noCompress += listOf("wasm", "task", "tflite")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.webkit)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
