# KCube Link Explorer

Package Explorer 와 비슷한 Eclipse 뷰입니다. **프로젝트를 폴더/프로젝트 위로 드래그하면 해당 프로젝트에 대한 링크가 생성됩니다.**
링크는 OS 심볼릭 링크 또는 Eclipse Linked Resource 로 만들 수 있습니다.

## 주요 기능

- **드래그 앤 드롭 링크 생성**: 뷰에서 프로젝트를 다른 폴더/프로젝트로 끌어다 놓으면 링크 생성
- **링크 방식 선택**: `OS symbolic link` 또는 `Eclipse linked resource` (환경설정)
- **Unlink**: 우클릭 메뉴에서 링크만 제거 (원본 프로젝트는 삭제하지 않음)
- **검증**: 자기 자신을 가리키는 링크와 순환 링크(A↔B)를 드롭 시점에 차단
- **JDT 지원**: Java 요소(패키지, 클래스 등)를 Package Explorer 처럼 표시
- **전용 퍼스펙티브**: `[KCube Link Explorer]` 퍼스펙티브 제공

## 요구 사항

- Eclipse 2024-12 이상 (Tycho 타깃 기준)
- 실행: Java 17 이상
- 빌드: JDK 17, Maven

## 설치

Eclipse 에서 `Help > Install New Software...` 의 `Work with:` 에 아래 업데이트 사이트 URL 을 입력해 설치합니다.

```
https://leejy-jpg.github.io/kcube-link-explorer/
```

직접 빌드한 jar 를 `dropins` 에 복사하거나 zip 으로 설치할 수도 있습니다. 상세 절차는 [docs/INSTALL.md](docs/INSTALL.md) 를 참고하세요.

## 사용 방법

1. `Window > Show View > Other... > KCube > [KCube Link Explorer]` 로 뷰를 엽니다.
2. 링크할 프로젝트를 대상 폴더/프로젝트로 드래그합니다.
3. 링크를 제거하려면 링크를 우클릭하고 `Unlink` 를 선택합니다.
4. 링크 방식은 `Window > Preferences > KCube Link Explorer` 의 *Link type on drop* 에서 변경합니다.

### 링크 방식

| 방식 | 설명 |
|------|------|
| OS symbolic link | 파일시스템 심볼릭 링크. Git·외부 빌드 도구가 인식하며, Windows 는 관리자 권한 또는 개발자 모드 필요 |
| Eclipse linked resource | `.project` 에 기록되는 Eclipse 전용 링크 |

## 빌드

JDK 17 이 필요합니다. `build.sh` 가 `JAVA_HOME` 을 JDK 17 로 설정합니다.

```bash
./build.sh             # mvn -q -B clean verify, 결과물은 dist/ 에 생성
./build.sh --install   # 빌드 후 $ECLIPSE_HOME/dropins 에 jar 복사
```

Windows 에서는 `build.bat` 을 사용합니다. `JAVA_HOME` 은 직접 JDK 17 로 지정해야 하며, `--install` 에는 `ECLIPSE_HOME` (dropins 폴더가 있는 Eclipse 설치 경로) 이 필요합니다.

```bat
build.bat             :: mvn -q -B clean verify, 결과물은 dist\ 에 생성
build.bat --install   :: 빌드 후 %ECLIPSE_HOME%\dropins 에 jar 복사
```

`mvn` 을 직접 실행할 경우 먼저 JDK 17 을 지정하세요.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn -q -B clean verify
```

생성물 (`dist/`):

- `com.kcube.link.kcube-link-explorer-<version>.jar`: 플러그인
- `kcube-link-explorer-update-site-<version>.zip`: 업데이트 사이트

## 프로젝트 구조

Maven Tycho 4.0.13 기반 Eclipse 플러그인입니다.

| 모듈 | 설명 |
|------|------|
| `com.kcube.link` | 플러그인 (뷰, 드롭 어시스턴트, 핸들러, 환경설정) |
| `com.kcube.link.feature` | Feature |
| `com.kcube.link.update-site` | 업데이트 사이트 |

주요 패키지 (`com.kcube.link`):

- `core.LinkService`: 링크 검증/생성/해제 (UI 독립)
- `dnd.ProjectLinkDropAssistant`: 드롭 처리, 프로젝트별 `WorkspaceJob` 실행
- `handlers.UnlinkHandler`: 링크만 제거 (심볼릭 링크는 `Files.delete` 로 삭제하여 원본을 건드리지 않음)
- `preferences`: 링크 방식 환경설정

버전(`major.minor.micro`)은 `MANIFEST.MF`, `feature.xml`, `pom.xml` 에서 일치해야 합니다.
설계 배경은 [docs/DEVELOPMENT_ko.md](docs/DEVELOPMENT_ko.md) 를 참고하세요.

## 알려진 제약

- Windows 에서 심볼릭 링크는 관리자 권한/개발자 모드가 필요합니다. 권한이 없으면 junction(`mklink /J`)으로 자동 대체됩니다 (디렉터리 전용).
- 링크된 프로젝트를 소스 폴더 안에 두면 두 번 컴파일될 수 있습니다.
- OS 심볼릭 링크는 Git 에 링크 파일로 커밋됩니다. `.gitignore` 자동 처리는 아직 없습니다.

## 라이선스

[LICENSE](LICENSE) 를 참고하세요.
