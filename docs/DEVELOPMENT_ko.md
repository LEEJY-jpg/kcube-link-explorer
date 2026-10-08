# KCube Link Explorer 설계 정리

이클립스용 프로젝트 뷰 플러그인입니다. 자바 뷰(Package Explorer)와 비슷하게 동작하며, **다른 프로젝트를 끌어다 놓으면 링크(심볼릭 링크)를 생성하는 기능**이 핵심입니다.

---

## 1. 개발 가능 여부

Eclipse 플러그인(PDE)으로 개발할 수 있으며, 모두 공개 API로 구현합니다.

| 구성 요소 | 사용 기술 |
|---|---|
| 뷰 | Common Navigator Framework (`org.eclipse.ui.navigator`) + `CommonNavigator` |
| 자바 뷰 유사 트리 | JDT 콘텐츠 `org.eclipse.jdt.java.ui.javaContent` 바인딩 |
| 드래그&드롭 | `CommonDropAdapterAssistant` 상속 |
| 링크 생성 | OS 심볼릭 링크(`Files.createSymbolicLink`) 또는 Eclipse Linked Resource(`IFolder.createLink`) |

### 링크 방식 비교

| 항목 | OS 심볼릭 링크 | Eclipse Linked Resource |
|---|---|---|
| 생성 API | `java.nio.file.Files.createSymbolicLink` | `IFolder.createLink` |
| 저장 위치 | 실제 파일시스템 | `.project` 파일 |
| Git·외부 빌드 도구 인식 | 인식함 | 인식 못함 (Eclipse 전용) |
| Windows 권한 | 관리자 또는 개발자 모드 필요 | 필요 없음 |

---

## 2. 네이밍

| 항목 | 값 |
|---|---|
| 제품명 (Bundle-Name) | `KCube Link Explorer` |
| 이클립스 프로젝트명 | `com.kcube.link` |
| 번들 ID (Bundle-SymbolicName) | `com.kcube.link` |
| 기본 패키지 | `com.kcube.link` |
| 뷰 이름 | `[KCube Link Explorer]` |
| 퍼스펙티브 이름 | `[KCube Link Explorer]` |
| 뷰 ID | `com.kcube.link.views.explorer` |
| 뷰 카테고리 ID / 이름 | `com.kcube.category` / `KCube` |
| 네비게이터 콘텐츠 ID | `com.kcube.link.navigatorContent` |
| 드롭 어시스턴트 ID | `com.kcube.link.dropAssistant` |

### 패키지 구조

```
com.kcube.link              // Activator, 상수
com.kcube.link.views        // 뷰
com.kcube.link.dnd          // ProjectLinkDropAssistant
com.kcube.link.core         // 링크 생성/해제 로직 (UI 비의존, 테스트·재사용 용이)
com.kcube.link.handlers     // Unlink 등 컨텍스트 메뉴
com.kcube.link.preferences  // 환경설정 페이지
```

### kcube 플러그인 계열 통일안

| 번들 ID | 용도 |
|---|---|
| `com.kcube.link` | KCube Link Explorer (이번 플러그인) |
| `com.kcube.markdown` | 마크다운 뷰어 |
| `com.kcube.feature` | 여러 플러그인을 묶는 feature |
| `com.kcube.updatesite` | 배포용 업데이트 사이트 |

---

## 3. plugin.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?eclipse version="3.4"?>
<plugin>

  <!-- 뷰 카테고리 + 뷰 등록: CommonNavigator 기반 -->
  <extension point="org.eclipse.ui.views">
    <category id="com.kcube.category" name="KCube"/>
    <view id="com.kcube.link.views.explorer"
          name="[KCube Link Explorer]"
          class="org.eclipse.ui.navigator.CommonNavigator"
          category="com.kcube.category"/>
  </extension>

  <!-- 드롭 어시스턴트 등록 -->
  <extension point="org.eclipse.ui.navigator.navigatorContent">
    <navigatorContent id="com.kcube.link.navigatorContent"
                      name="KCube Link Content"
                      contentProvider="org.eclipse.ui.model.BaseWorkbenchContentProvider"
                      labelProvider="org.eclipse.ui.model.WorkbenchLabelProvider"
                      priority="low"
                      activeByDefault="true">
      <triggerPoints>
        <instanceof value="org.eclipse.core.resources.IResource"/>
      </triggerPoints>
      <dropAssistant id="com.kcube.link.dropAssistant"
                     class="com.kcube.link.dnd.ProjectLinkDropAssistant">
        <possibleDropTargets>
          <or>
            <adapt type="org.eclipse.core.resources.IContainer"/>
          </or>
        </possibleDropTargets>
      </dropAssistant>
    </navigatorContent>
  </extension>

  <!-- 뷰에 JDT 콘텐츠 + 리소스 콘텐츠 + 자체 콘텐츠 바인딩 -->
  <extension point="org.eclipse.ui.navigator.viewer">
    <viewerContentBinding viewerId="com.kcube.link.views.explorer">
      <includes>
        <contentExtension pattern="org.eclipse.jdt.java.ui.javaContent"/>
        <contentExtension pattern="org.eclipse.ui.navigator.resourceContent"/>
        <contentExtension pattern="com.kcube.link.navigatorContent"/>
      </includes>
    </viewerContentBinding>
  </extension>

</plugin>
```

---

## 4. MANIFEST.MF 참고

```
Bundle-ManifestVersion: 2
Bundle-Name: KCube Link Explorer
Bundle-SymbolicName: com.kcube.link;singleton:=true
Bundle-Version: 1.0.0.qualifier
Bundle-Vendor: KCube
Bundle-RequiredExecutionEnvironment: JavaSE-17
Require-Bundle: org.eclipse.ui,
 org.eclipse.ui.navigator,
 org.eclipse.ui.navigator.resources,
 org.eclipse.core.resources,
 org.eclipse.core.runtime,
 org.eclipse.jdt.ui
Import-Package: org.slf4j
```

---

## 5. 드롭 어시스턴트 (Java 17)

`com.kcube.link.dnd.ProjectLinkDropAssistant`

```java
package com.kcube.link.dnd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 다른 프로젝트를 드롭하면 대상 폴더에 링크를 생성하는 드롭 어시스턴트.
 */
public class ProjectLinkDropAssistant extends CommonDropAdapterAssistant {

    /** 로거 */
    private static final Logger _log = LoggerFactory.getLogger(ProjectLinkDropAssistant.class);

    /** true: OS 심볼릭 링크, false: Eclipse Linked Resource */
    private static final boolean USE_OS_SYMLINK = true;

    /**
     * 드롭 가능 여부를 검사한다.
     * @param target       드롭 대상 (IJavaProject 등도 IContainer로 어댑트)
     * @param operation    DnD 오퍼레이션
     * @param transferType 전송 타입
     * @return 허용 여부 상태
     */
    @Override
    public IStatus validateDrop(Object target, int operation, TransferData transferType) {
        IContainer dest = Adapters.adapt(target, IContainer.class);
        if (dest == null || !LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
            return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
    }

    /**
     * 드롭 처리: 선택된 프로젝트마다 링크 생성 작업을 실행한다.
     * @param adapter 드롭 어댑터
     * @param event   드롭 이벤트
     * @param target  드롭 대상
     * @return 처리 결과 상태
     */
    @Override
    public IStatus handleDrop(CommonDropAdapter adapter, DropTargetEvent event, Object target) {
        IContainer dest = Adapters.adapt(target, IContainer.class);
        if (dest == null
                || !(LocalSelectionTransfer.getTransfer().getSelection() instanceof IStructuredSelection sel)) {
            return Status.CANCEL_STATUS;
        }

        for (Object o : sel) {
            IProject src = Adapters.adapt(o, IProject.class);
            // 자기 자신 링크 방지 (순환 참조 예방)
            if (src == null || src.equals(dest.getProject())) {
                continue;
            }
            scheduleLinkJob(src, dest);
        }
        return Status.OK_STATUS;
    }

    /**
     * 워크스페이스 잠금 하에서 링크를 생성하는 Job을 스케줄한다.
     * @param src  링크 원본 프로젝트
     * @param dest 링크를 만들 대상 컨테이너
     */
    private void scheduleLinkJob(IProject src, IContainer dest) {
        WorkspaceJob job = new WorkspaceJob("Link " + src.getName()) {
            /**
             * 링크 생성 작업 본체.
             * @param monitor 진행 모니터
             * @return 작업 결과 상태
             */
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                try {
                    if (USE_OS_SYMLINK) {
                        createOsSymlink(src, dest, monitor);
                    } else {
                        createEclipseLink(src, dest, monitor);
                    }
                    if (_log.isInfoEnabled()) {
                        _log.info("Link created: {} -> {}", src.getName(), dest.getFullPath());
                    }
                    return Status.OK_STATUS;
                } catch (IOException e) {
                    if (_log.isErrorEnabled()) {
                        _log.error("Failed to create symlink for {}", src.getName(), e);
                    }
                    return Status.error("Symlink creation failed", e);
                }
            }
        };
        job.setRule(dest.getProject()); // 대상 프로젝트 단위 스케줄링 룰
        job.schedule();
    }

    /**
     * OS 레벨 심볼릭 링크를 생성하고 대상 폴더를 리프레시한다.
     * @param src     원본 프로젝트
     * @param dest    대상 컨테이너
     * @param monitor 진행 모니터
     */
    private void createOsSymlink(IProject src, IContainer dest, IProgressMonitor monitor)
            throws IOException, CoreException {
        Path link = dest.getLocation().toPath().resolve(src.getName()); // 생성할 링크 경로
        Path original = src.getLocation().toPath();                     // 실제 원본 경로
        if (Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
            if (_log.isWarnEnabled()) {
                _log.warn("Link already exists: {}", link);
            }
            return;
        }
        Files.createSymbolicLink(link, original);
        dest.refreshLocal(IResource.DEPTH_ONE, monitor); // Eclipse에 반영
    }

    /**
     * Eclipse Linked Resource(.project 기록 방식)로 링크를 생성한다.
     * @param src     원본 프로젝트
     * @param dest    대상 컨테이너
     * @param monitor 진행 모니터
     */
    private void createEclipseLink(IProject src, IContainer dest, IProgressMonitor monitor)
            throws CoreException {
        IFolder folder = dest.getFolder(IPath.fromOSString(src.getName()));
        if (folder.exists()) {
            if (_log.isWarnEnabled()) {
                _log.warn("Linked folder already exists: {}", folder.getFullPath());
            }
            return;
        }
        folder.createLink(src.getLocationURI(), IResource.ALLOW_MISSING_LOCAL, monitor);
    }
}
```

`IPath.fromOSString`과 `Status.error`는 Eclipse 4.29 이상에서 제공되는 API입니다.

> 향후 리팩터링: `createOsSymlink`, `createEclipseLink`는 UI 비의존 로직이므로 `com.kcube.link.core` 패키지(예: `LinkService`)로 분리하고, 드롭 어시스턴트는 호출만 담당하도록 정리합니다.

---

## 6. 실무에서 주의할 점

- **Windows 심볼릭 링크 권한**: 관리자 권한이나 개발자 모드가 필요합니다. 대안으로 디렉터리 정션(`mklink /J`)을 쓰거나 Linked Resource 방식으로 대체할 수 있습니다.
- **중복 빌드와 인덱싱**: 링크된 프로젝트의 `src`가 대상 프로젝트의 소스 폴더 안에 들어가면 같은 클래스가 두 번 컴파일됩니다. 링크를 소스 폴더 밖에 두거나 빌드 경로에서 제외하세요.
- **순환 링크**: A↔B처럼 서로 링크하면 리프레시나 검색이 무한 루프에 빠질 수 있으므로 드롭 시점에 검사합니다.
- **Git**: OS 심볼릭 링크는 링크 자체로 커밋됩니다. 의도하지 않았다면 `.gitignore`에 등록하세요.
- **대안**: 새 뷰가 꼭 필요하지 않다면, 기존 Project Explorer에 `dropAssistant`만 추가하는 방식이 더 간단합니다.

---

## 7. 다음 작업 후보

- 링크 로직을 `com.kcube.link.core.LinkService`로 분리
- 링크 해제(Unlink) 컨텍스트 메뉴 (`com.kcube.link.handlers`)
- 링크 방식(OS 심볼릭 링크 / Linked Resource) 선택 환경설정 페이지 (`com.kcube.link.preferences`)
- 순환 링크 검사 로직
- `com.kcube.feature`, `com.kcube.updatesite` 구성
