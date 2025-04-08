# IntelliJ IDEA용 AutoXPlugin
<!-- Plugin description -->
![official JetBrains project](https://jb.gg/badges/official-flat-square.svg)
![JetBrains IntelliJ Platform SDK Docs](https://jb.gg/badges/docs.svg?style=flat-square)

이것은 AutoX.js 동반 플러그인 개발을 위한 IntelliJ IDEA 플러그인입니다. AutoX.js 프로그램이 아직 없다면 [AutoX.js](https://github.com/aiselp/AutoX)를 방문하세요.

[🌟 English](README_en.md) | [🌏 中文](README.md) | [🌏 日本語](README_JP.md) | [🌏 한국인](README_ko.md) | [🌏 Русский](README_ru.md) | [🌏 Français](README_fr.md)
> [!TIP]
> USB 디버깅 개발은 아직 지원되지 않으며, LAN 디버깅만 지원됩니다.

## 기능

- IDE 내에서 직접 간단한 WebSocket 서버 실행
- 모바일 기기 연결을 위한 QR 코드 생성
- 파일을 우클릭하여 연결된 기기로 전송
- 로컬 네트워크를 통한 실시간 파일 전송
- 다국어 지원, IDE 언어 변경 후 플러그인이 자동으로 언어 전환

<!-- Plugin description end -->
## 스크린샷
<div align="center">
<table>
<tr>

<td align="center">
<b>툴바</b><br>
<img src="/img/%E6%88%AA%E5%B1%8F2025-04-03%2000.43.22.png" width="500" alt="툴바"><br>
<small>시작 버튼을 클릭한 후 휴대폰으로 QR 코드를 스캔하여 연결할 수 있습니다</small>
</td>
<td align="center">
<b>폴더 우클릭</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.40.57.png" width="500" alt="폴더 우클릭"><br>
<small>폴더에서 우클릭하여 연결된 기기로 보내거나 실행할 수 있습니다</small>
</td>
<td align="center">
<b>JS 파일 우클릭</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.40.39.png" width="500" alt="JS 파일 우클릭"><br>
<em>유효한 autox.js 스크립트에서 우클릭하여 연결된 기기에서 저장, 실행, 재실행 또는 중지할 수 있습니다</em>
</td>
<td align="center">
<b>JSON 파일 우클릭</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.41.36.png" width="500" alt="JSON 파일 우클릭"><br>
<em>유효한 JSON 파일에서 우클릭하여 해당 디렉토리를 기기에 저장할 수 있습니다</em>
</td>
</tr>
</table>
</div>

## 자세한 기능 설명

유효한 autox.js 프로젝트 파일에는 project.json 파일이 포함되어 있기 때문에 project.json 파일이 포함된 폴더에서 "기기로 실행" 작업을 수행할 수 있습니다.

- 프로젝트를 기기에 저장: 우클릭한 폴더를 연결된 기기로 전송합니다. 작업이 성공하면 모바일 기기에서 파일을 볼 수 있습니다.
- 프로젝트를 기기에서 실행: 우클릭한 폴더를 연결된 기기로 전송합니다. 작업이 성공하면 모바일 기기에서 실행할 수 있지만 파일은 기기에 저장되지 않습니다.
- 스크립트를 기기에 저장: 우클릭한 단일 파일을 연결된 기기로 전송합니다. 작업이 성공하면 모바일 기기에서 파일을 볼 수 있습니다.
- 스크립트를 기기에서 실행: 우클릭한 단일 파일을 연결된 기기로 전송합니다. 작업이 성공하면 모바일 기기에서 실행할 수 있지만 파일은 기기에 저장되지 않습니다.
- 스크립트를 기기에서 재실행: 우클릭한 단일 파일을 연결된 기기로 전송합니다. 작업이 성공하면 이전에 실행 중인 프로그램을 먼저 중지한 후 모바일 기기에서 실행합니다.
- 기기에서 스크립트 중지: 작업이 성공하면 모바일 기기에서 실행 중인 동일한 이름의 프로그램을 중지합니다.

## 설치

1. Releases 페이지에서 최신 버전을 다운로드합니다.
2. IntelliJ IDEA에서 `설정` > `플러그인` > `디스크에서 플러그인 설치...`로 이동합니다.
3. 다운로드한 `.zip` 파일을 선택합니다.
4. IntelliJ IDEA를 재시작합니다.

## 사용 방법

1. 오른쪽 사이드바에서 **AutoXPlugin** 도구 창을 엽니다.
2. WebSocket 서버를 시작하려면 **"서버 시작"**을 클릭합니다.
3. 모바일 기기로 QR 코드를 스캔합니다.
4. 기기에서 서버에 연결합니다.
5. 프로젝트에서 파일을 우클릭하고 **"기기로 보내기"**를 선택합니다.

## 소스에서 빌드하기

1. 이 저장소를 클론합니다.
2. IntelliJ IDEA에서 프로젝트를 가져옵니다.
3. Gradle을 사용하여 프로젝트를 빌드합니다:

   ```bash
   ./gradlew buildPlugin
   ```

## 라이선스

이 프로젝트는 MIT 라이선스에 따라 라이선스가 부여됩니다. 자세한 내용은 LICENSE 파일을 참조하세요.

## 감사의 말

다음에 감사드립니다:
[jetbrains sample](https://github.com/JetBrains/intellij-sdk-code-samples) 예제 코드 저장소

[https://github.com/wilinz/Auto.js-VSCode-Extension](https://github.com/wilinz/Auto.js-VSCode-Extension) 유사한 기능을 가진 VSCode 플러그인 