# 베베뷰 (Bebeview) — Android

영상 기반 영유아 발달 관찰 앱 **베베뷰**의 안드로이드 버전입니다.

웹 버전 [GrowthLens](https://github.com/Park8259/growthLens)(React + MediaPipe)를 **그대로 앱 안에 담아** 실행합니다.
화면과 분석 로직은 웹 레포에서 관리하고, 이 레포는 안드로이드 껍데기(WebView 셸)만 담당합니다.

## 구조

```
웹 레포 (growthLens)                     이 레포 (bebeview)
┌──────────────────────┐   npm run build   ┌──────────────────────────────┐
│ React 화면            │ ───────────────▶ │ APK assets/www/              │
│ MediaPipe WASM·모델   │   (Gradle 자동)   │   ↓                           │
└──────────────────────┘                   │ WebView                       │
                                           │ https://appassets.android...  │
                                           │ + 카메라 권한 / 파일 선택 /    │
                                           │   공유 / 뒤로가기 (네이티브)    │
                                           └──────────────────────────────┘
```

- 웹 빌드 결과(`dist/`)가 APK 안에 들어가므로 **인터넷 없이 동작**합니다. (웹 글꼴만 CDN 사용, 없으면 기본 글꼴)
- `https://appassets.androidplatform.net/` 주소로 열기 때문에 카메라(getUserMedia)·WASM 등 보안 컨텍스트 기능이 동작합니다.
- 영상 분석은 웹과 똑같이 기기 안에서(MediaPipe) 처리되고, 데이터는 WebView의 localStorage / IndexedDB에 저장됩니다.

## 실행 방법

### 1. 준비물

- Android Studio (최신 버전)
- Node.js / npm
- 웹 레포 클론 + 의존성 설치

```bash
git clone https://github.com/Park8259/growthLens.git
cd growthLens
npm install        # MediaPipe WASM 파일도 이때 public/mediapipe 로 복사됨
```

### 2. 웹 레포 위치 설정

이 레포를 클론한 뒤, 프로젝트 루트의 `local.properties`에 웹 레포 경로를 추가합니다.
(`local.properties`는 Android Studio가 처음 열 때 만들어 주며, git에 올라가지 않습니다.)

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
growthlens.dir=/절대/경로/growthLens
```

| 설정 | 설명 |
|---|---|
| `growthlens.dir` | **필수.** 웹 레포 폴더 경로 |
| `growthlens.npm` | 선택. npm 위치 (기본: `/opt/homebrew/bin`, `/usr/local/bin` 등에서 찾음) |
| `growthlens.skipNpmBuild` | 선택. `true`면 npm 빌드를 건너뛰고 이미 있는 `dist/`만 복사 |

### 3. 실행

Android Studio에서 프로젝트를 열고 Gradle Sync → 기기 선택 → ▶ Run.

또는 터미널에서:

```bash
./gradlew installDebug
```

빌드할 때 웹 레포에서 `npm run build`가 자동으로 실행되고, 결과가 앱에 들어갑니다.
**웹 코드를 수정했다면 앱을 다시 실행하기만 하면 반영됩니다.** (웹 코드가 그대로면 npm 빌드는 건너뜀)

## 주요 파일

| 파일 | 역할 |
|---|---|
| `app/build.gradle.kts` | `buildWeb`(npm 빌드) → `copyWebAssets`(assets/www 복사) 태스크 |
| `app/src/main/java/.../MainActivity.kt` | 진입점. 상태바·키보드 영역 처리 |
| `app/src/main/java/.../web/WebAppScreen.kt` | WebView 설정, 카메라 권한, 파일 선택, 뒤로가기, 외부 링크 |
| `app/src/main/java/.../web/LocalWebAssets.kt` | assets/www 를 앱 주소로 제공. React Router 경로는 `index.html`로 연결 |
| `app/src/main/java/.../web/NativeBridge.kt` | `navigator.share` → 안드로이드 공유 시트 |

## 웹 기능 ↔ 네이티브 처리

| 웹에서 쓰는 기능 | 앱에서의 처리 |
|---|---|
| 카메라 (`getUserMedia`) | 안드로이드 카메라 권한 요청 후 WebView에 허용 |
| 영상 파일 선택 (`<input type="file">`) | 안드로이드 동영상 선택 창 |
| 공유 (`navigator.share`) | 안드로이드 공유 시트 (앱 내부 주소는 빼고 텍스트만 공유) |
| 뒤로 가기 | 웹 화면 기록이 있으면 이전 화면, 없으면 앱 종료 |
| 외부 링크 | 기본 브라우저로 열기 |
| `window.confirm` 등 대화상자 | WebView 기본 대화상자 |

## 디버깅

디버그 빌드에서는 WebView 원격 디버깅이 켜져 있습니다.
기기를 연결하고 Chrome에서 `chrome://inspect` 를 열면 웹 콘솔·네트워크를 볼 수 있습니다.

## 참고

- APK 크기는 약 67MB입니다. (MediaPipe WASM·모델 약 56MB)
- 화면은 세로 고정이며, 디자인 유지를 위해 시스템 글꼴 크기 설정은 반영하지 않습니다 (`textZoom = 100`).
- minSdk 24 / targetSdk 36
