# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current state

This repository is a **design-stage skeleton** for an Eclipse plugin (PDE). It contains no source, `plugin.xml`, `MANIFEST.MF`, or build files yet — only an Eclipse `.project` stub (no natures/builders), `.settings/` (UTF-8 encoding), and the design document [docs/DEVELOPMENT_ko.md](docs/DEVELOPMENT_ko.md) (written in Korean). It is not a git repository. There are no build, lint, or test commands yet; the plugin is expected to be built through Eclipse PDE (and possibly Tycho later, via the planned `com.kcube.updatesite`).

Treat `docs/DEVELOPMENT_ko.md` as the source of truth for intended design. It contains full draft `plugin.xml`, `MANIFEST.MF` and `ProjectLinkDropAssistant` code to copy from when scaffolding.

## What the plugin does

"KCube Link Explorer" is a Package Explorer–like Eclipse view. Its core feature: **dragging a project onto a folder/project in the view creates a link to it** (OS symbolic link, or an Eclipse Linked Resource).

## Architecture (planned)

- **View**: `com.kcube.link.views.explorer` is a plain `org.eclipse.ui.navigator.CommonNavigator` (Common Navigator Framework), registered under the `com.kcube.category` ("KCube") view category. No custom view class is needed.
- **Content**: `viewerContentBinding` binds three content extensions to the viewer: JDT's `org.eclipse.jdt.java.ui.javaContent` (Java-view-like tree), `org.eclipse.ui.navigator.resourceContent`, and the plugin's own `com.kcube.link.navigatorContent`.
- **Drag & drop**: `com.kcube.link.dnd.ProjectLinkDropAssistant` (extends `CommonDropAdapterAssistant`, registered as `com.kcube.link.dropAssistant` inside the navigator content). It adapts the drop target to `IContainer`, accepts only `LocalSelectionTransfer`, skips dropping a project onto itself, and schedules one `WorkspaceJob` per dropped project with the destination project as scheduling rule.
- **Link strategy**: switched by the `USE_OS_SYMLINK` constant (to become a preference). OS symlink (`Files.createSymbolicLink` then `refreshLocal(DEPTH_ONE)`) is visible to Git/external build tools; Linked Resource (`IFolder.createLink`, stored in `.project`) is Eclipse-only but needs no Windows privileges.
- **Planned package layout** (base package `com.kcube.link`): root (Activator, constants), `views`, `dnd`, `core` (UI-independent link create/remove logic — planned `LinkService`; the drop assistant should only delegate to it), `handlers` (Unlink context menu), `preferences`.
- **Sibling bundles** (naming scheme): `com.kcube.markdown`, `com.kcube.feature`, `com.kcube.updatesite`.

## Conventions and constraints

- Bundle ID / project name: `com.kcube.link` (singleton); Bundle-Name `KCube Link Explorer`; execution environment `JavaSE-21`, so Java 21 features (e.g. pattern-matching `instanceof`) are used.
- Logging uses SLF4J (`Import-Package: org.slf4j`) with `private static final Logger _log` and `if (_log.isXxxEnabled())` guards before log calls.
- Javadoc on methods is written in Korean with `@param` descriptions; match this style.
- `IPath.fromOSString` and `Status.error` require Eclipse 4.29+.
- Known pitfalls to handle: circular links (A↔B) must be detected at drop time; a linked project placed inside a source folder gets compiled twice; Windows symlinks need admin/developer mode (fallback: junction `mklink /J` or Linked Resource); OS symlinks get committed to Git as links (consider `.gitignore`).
- Pending work list is in section 7 of the design doc (LinkService extraction, Unlink handler, link-mode preference page, cycle check, feature/updatesite).
