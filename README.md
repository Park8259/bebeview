# 베베뷰 (Bebeview) — Android

영상 기반 영유아 발달 관찰 앱 **베베뷰**의 안드로이드 버전입니다.

> 이전에는 웹 버전(GrowthLens, React+MediaPipe)을 WebView로 감싸는 구조였지만,
> 지금은 **네이티브 Kotlin 앱으로 완전히 전환 중**입니다. (WebView 셸 코드는 제거됨)

## 구조

```
app/src/main/java/com/example/bebeview/
├── MainActivity.kt            RecordAppRoot 진입 (권한은 촬영 화면에서 요청)
├── ui/
│   ├── theme/BebeDesign.kt    growthLens Tailwind 디자인 값 (색, Pretendard 글꼴, 글자 크기)
│   └── components/            공용 UI: FormField, ParentNav(하단 탭), BottomSheet, BebeIcons(웹 아이콘 자동 변환) 등
└── record/
    ├── data/                  TaskType(과제 문구·시간), AppData(아이 정보·기록·질문·메모 기기 저장), 모델
    ├── engine/                MediaPipe FaceLandmarker 기반 분석 (FaceAnalyzer, NameResponseAnalyzer)
    ├── api/                   베베뷰 서버(EC2) 연동 (Retrofit)
    └── ui/                    화면 (growthLens screens/* 와 1:1 대응)
        ├── RecordAppRoot.kt   화면 이동(뒤로가기 기록) · 스플래시 · 로그인 분기
        ├── auth/              시작 · 회원가입 · 가입 완료 · 이용약관 · 로그인
        ├── onboarding/        아이 정보 입력 (선택 시트, 휠 선택기)
        ├── tasks/             오늘의 과제 · 과제 안내
        ├── RecordingScreen.kt 촬영 (CameraX) · recording/ 분석 화면, 데모 파일 분석
        ├── result/            결과 · 기록 상세 · AI가 본 장면 · 지난 기록 상세
        ├── timeline/ charts/ summary/   기록 · 변화 · 진료 요약(공유 코드, PDF)
```

## 현재 상태 (2026-11-21 기준)

growthLens 웹의 **보호자 앱 화면 전체를 네이티브 Compose로 포팅**했습니다 (웹과 같은 크기·색·문구).
의사 화면은 웹에서 병원용 프로그램으로 분리하기로 해서 포함하지 않았습니다.

- 데이터: 웹처럼 아이 정보·기록·질문은 기기 안(`filesDir/*.json`)에만 저장. 처음엔 웹과 같은 예시 기록으로 시작
- 촬영 → 분석: "호명 반응"은 CameraX 녹화 + MediaPipe FaceLandmarker 로 네이티브 분석
  (growthLens `src/engine/metrics/nameResponse.ts` 포팅, yaw 임계값은 실기기 보정 필요할 수 있음)
- 서버: 회원가입·로그인, 지표 업로드(`POST /api/metrics`, 교정 월령 포함), 공유 코드 발급
  (현재 `43.201.67.12:8080`, HTTP — 평문 통신은 `res/xml/network_security_config.xml`에서 이 IP만 허용)

**아직 안 된 것** (다음 단계):
- "동작 모방", "공 주고받기" 분석과 공통 지표(Pose/Hand landmarker 포팅 필요) — 지금은 재촬영 안내가 나옴
- 서버 API: 로그인 토큰, 이메일 인증번호, 비밀번호 찾기, 아이 정보·기록 저장/조회 등 (지금 이메일 인증은 웹처럼 6자리면 통과)

## 실행 방법

Android Studio에서 프로젝트를 열고 Gradle Sync → 기기 선택 → ▶ Run.
촬영 화면에 처음 들어갈 때 카메라·마이크 권한을 요청합니다.
