# 설치 방법

`dist/` 하위에 빌드된 jar(`com.kcube.link.kcube-link-explorer-1.0.0.jar`)가 있다고 가정합니다.
빌드가 필요하면 [README](../README.md) 또는 `./build.sh`를 참고하세요.

- 요구 사항: Eclipse 2024-12(4.34) 이상, JDK 17 이상
- 설치 방식: `dropins` 폴더에 jar 복사 (가장 간단)

## 공통 주의사항

- 이전 버전이 설치되어 있다면 `dropins/` 에서 `com.kcube.link*` jar를 먼저 삭제하세요.
- 복사 후 Eclipse를 **`-clean` 옵션으로 재시작**해야 인식됩니다.
- 설치 후 `Window > Show View > Other... > KCube > Link Explorer` 에서 뷰를 엽니다.

## macOS

기본 Eclipse 경로는 `/Applications/Eclipse.app/Contents/Eclipse` 입니다.

```bash
ECLIPSE_HOME=/Applications/Eclipse.app/Contents/Eclipse
rm -f "$ECLIPSE_HOME"/dropins/com.kcube.link*.jar
cp dist/com.kcube.link.kcube-link-explorer-1.0.0.jar "$ECLIPSE_HOME/dropins/"
"$ECLIPSE_HOME/../MacOS/eclipse" -clean
```

`./build.sh --install` 로 빌드와 복사를 한 번에 할 수도 있습니다. 그 후 Eclipse를 `-clean` 으로 재시작하세요.

## Windows

Eclipse가 `C:\eclipse` 에 설치되었다고 가정합니다 (PowerShell).

```powershell
$EclipseHome = "C:\eclipse"
Remove-Item "$EclipseHome\dropins\com.kcube.link*.jar" -ErrorAction SilentlyContinue
Copy-Item dist\com.kcube.link.kcube-link-explorer-1.0.0.jar "$EclipseHome\dropins\"
& "$EclipseHome\eclipse.exe" -clean
```

- `Program Files` 아래에 설치했다면 관리자 권한 PowerShell이 필요합니다.
- OS 심볼릭 링크(Symlink) 모드는 **관리자 권한 또는 개발자 모드**(설정 > 개발자용 > 개발자 모드)가 필요합니다. 권한이 없으면 설정에서 링크 방식을 `Linked Resource` 로 바꾸세요.

## Linux

Eclipse가 `~/eclipse` 에 설치되었다고 가정합니다.

```bash
ECLIPSE_HOME=~/eclipse
rm -f "$ECLIPSE_HOME"/dropins/com.kcube.link*.jar
cp dist/com.kcube.link.kcube-link-explorer-1.0.0.jar "$ECLIPSE_HOME/dropins/"
"$ECLIPSE_HOME/eclipse" -clean
```

- 시스템 경로(`/opt/eclipse` 등)에 설치했다면 `sudo` 가 필요합니다.
- Snap/Flatpak 으로 설치한 Eclipse는 `dropins` 경로가 다르거나 쓰기 불가일 수 있으므로, 아래 업데이트 사이트 방식을 사용하세요.

## 대안: 업데이트 사이트로 설치

`dist/kcube-link-explorer-update-site-1.0.0.zip` 이 있다면 OS와 관계없이 사용할 수 있습니다.

1. `Help > Install New Software...`
2. `Add... > Archive...` 에서 zip 파일 선택
3. `KCube Link Explorer` 를 선택하고 설치 후 Eclipse 재시작

## 설정

`Window > Preferences > KCube Link Explorer`에서 링크 방식을 선택합니다.

- `OS symbolic link` (SYMLINK): OS 심볼릭 링크
- `Eclipse linked resource` (LINKED_RESOURCE): Eclipse Linked Resource (`.project` 에 저장)

## 제거

`dropins/` 의 `com.kcube.link*.jar` 를 삭제하고 Eclipse를 `-clean` 으로 재시작합니다.
업데이트 사이트로 설치했다면 `Help > About Eclipse IDE > Installation Details` 에서 Uninstall 하세요.
