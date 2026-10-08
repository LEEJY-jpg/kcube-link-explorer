# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project layout

Eclipse plug-in built with Maven Tycho 4.0.13 (Eclipse 2024-12 target), structured like the sibling `kcube-markdown-viewer` (`~/git/kcube-markdown-viewer`). Modules: `com.kcube.link` (plug-in), `com.kcube.link.feature`, `com.kcube.link.update-site`. The version `major.minor.micro` must match across MANIFEST.MF, feature.xml and pom.xml. Design background (Korean) is in [docs/DEVELOPMENT_ko.md](docs/DEVELOPMENT_ko.md); the module names here follow the markdown viewer pattern rather than the `com.kcube.feature` / `com.kcube.updatesite` names in that doc.

## Build

```bash
./build.sh             # mvn -q -B clean verify; jar + update-site zip land in dist/
./build.sh --install   # also copies the jar into $ECLIPSE_HOME/dropins (default /Applications/Eclipse.app/Contents/Eclipse)
```

Requires JDK 17 (`build.sh` sets `JAVA_HOME` via `/usr/libexec/java_home -v 17`; the default `mvn` here resolves to JDK 26, so export `JAVA_HOME` first when running `mvn` directly). There are no tests or linter; verification is `mvn clean verify` (what CI runs) plus trying the plug-in in Eclipse (Run As > Eclipse Application, or install to `dropins` and restart Eclipse with `-clean`; remove stale `com.kcube.link*` jars from `dropins/` first). Never kill the user's running Eclipse instances.

## What the plugin does

"KCube Link Explorer" is a Package Explorer–like Eclipse view. Its core feature: **dragging a project onto a folder/project in the view creates a link to it** (OS symbolic link, or an Eclipse Linked Resource).

## Architecture

- **View**: `com.kcube.link.views.explorer` is a plain `org.eclipse.ui.navigator.CommonNavigator` (no custom view class), in the `com.kcube.category` ("KCube") category. All wiring is in `com.kcube.link/plugin.xml`: `viewerContentBinding` binds `org.eclipse.jdt.java.ui.javaContent`, `org.eclipse.ui.navigator.resourceContent` and `com.kcube.link.navigatorContent`; context menu id is `com.kcube.link.views.explorer#PopupMenu`.
- **`core/LinkService`** (UI-independent): `validate` (self-link and circular-link checks via real paths), `link`, `unlink`, `isLink`. `LinkMode` is `SYMLINK` (`Files.createSymbolicLink` + `refreshLocal`) or `LINKED_RESOURCE` (`IFolder.createLink`, stored in `.project`).
- **`dnd/ProjectLinkDropAssistant`** adapts the target to `IContainer`, validates every dragged project through `LinkService.validate`, and schedules one `WorkspaceJob` per project (rule = destination project). It only delegates to `LinkService`.
- **`handlers/UnlinkHandler`** removes only the link itself. Symlinks are deleted with `Files.delete` (never `IResource.delete`, which could follow into the original project); Linked Resources use `IResource.delete`.
- **`preferences/`**: `LinkPreferencePage` + `PreferenceInitializer` store the link mode under `Activator.PREF_LINK_MODE`; `Activator.getLinkMode()` reads it.

## Conventions and constraints

- Bundle ID `com.kcube.link` (singleton); execution environment `JavaSE-17`, so Java 17 syntax (pattern-matching `instanceof`, switch arrows) is fine. Source uses tab indentation.
- Logging uses SLF4J (via `Import-Package: org.slf4j`, not Require-Bundle) with `private static final Logger _log` and `if (_log.isXxxEnabled())` guards before log calls.
- Javadoc on methods is written in Korean with `@param` descriptions; match this style.
- `IPath.fromOSString` and `Status.error` require Eclipse 4.29+.
- Known pitfalls to handle: circular links (A↔B) must be detected at drop time; a linked project placed inside a source folder gets compiled twice; Windows symlinks need admin/developer mode (fallback: junction `mklink /J` or Linked Resource); OS symlinks get committed to Git as links (consider `.gitignore`).
- Remaining ideas from section 7 of the design doc are all implemented except junction (`mklink /J`) fallback on Windows and `.gitignore` handling for symlinks.
