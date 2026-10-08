# AGENTS.md

Eclipse plug-in (Maven Tycho 4.0.13, Eclipse 2024-12 target). Modules: `com.kcube.link` (plug-in), `com.kcube.link.feature`, `com.kcube.link.update-site`. No tests, no linter.

## Build / verify

```bash
./build.sh             # mvn -q -B clean verify; jar + update-site zip land in dist/
./build.sh --install   # also copies jar into $ECLIPSE_HOME/dropins (default /Applications/Eclipse.app/Contents/Eclipse)
```

- `build.sh` sets `JAVA_HOME` to JDK 17 via `/usr/libexec/java_home -v 17`. Default `mvn` here resolves to JDK 26, so when running `mvn` directly first run `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`.
- Verification is `mvn clean verify` (what CI `.github/workflows/build.yml` runs) plus manual check in Eclipse (Run As > Eclipse Application, or dropins install + restart with `-clean`). CI publishes `com.kcube.link.update-site/target/repository` to GitHub Pages on `main`/`v*`, and `dist/*` to releases on `v*` tags.
- Manual install: remove stale `com.kcube.link*.jar` from `dropins/` first, then restart Eclipse with `-clean`. Never kill the user's running Eclipse instances.

## Versioning

`major.minor.micro` must match across `com.kcube.link/META-INF/MANIFEST.MF` (`Bundle-Version`), `com.kcube.link.feature/feature.xml`, and `pom.xml`.

## Architecture (all wiring in `com.kcube.link/plugin.xml`)

- View `com.kcube.link.views.explorer` is `views.LinkExplorerView extends CommonNavigator` (custom class, not plain). It mirrors Package Explorer's working-set mode/selection via `views/PackageExplorerBridge` (+ `OrderedWorkingSetSorter`); `viewerContentBinding` includes `javaContent`, `resourceContent`, working-set/linkHelper extensions, and `com.kcube.link.navigatorContent`. Popup menu id: `com.kcube.link.views.explorer#PopupMenu`.
- `core/LinkService` (UI-independent): `validate` (self-link + circular-link checks via real paths, including A↔B via `containsLinkTo`), `link`, `unlink`, `isLink`. `SYMLINK` = `Files.createSymbolicLink` + `refreshLocal`; `LINKED_RESOURCE` = `IFolder.createLink` (stored in `.project`).
- `dnd/ProjectLinkDropAssistant`: adapts target to `IContainer`; `validateDrop` returns `CANCEL` if any dragged item isn't a project or fails `LinkService.validate`. `handleDrop` schedules one `WorkspaceJob` per project (rule = destination project). Business logic stays in `LinkService`.
- `handlers/UnlinkHandler`: deletes only the link. Symlinks/junctions via `Files.delete` — never `IResource.delete` (it could follow into the original project). Linked Resources via `IResource.delete`.
- `preferences/` (`LinkPreferencePage` + `PreferenceInitializer`) stores mode under `Activator.PREF_LINK_MODE`; read via `Activator.getLinkMode()`.

## Conventions / gotchas

- `JavaSE-17`, tab indentation, Korean Javadoc with `@param` on methods. Logging: SLF4J via `Import-Package: org.slf4j` with `private static final Logger _log` and `if (_log.isXxxEnabled())` guards.
- `IPath.fromOSString` and `Status.error` need Eclipse 4.29+ — don't replace with older APIs.
- Windows: symlink without admin/developer mode falls back to junction (`cmd /c mklink /J`, dirs only); `isLinkPath` treats junctions as links. Prefer Linked Resource mode there.
- Known pitfalls: circular links must be rejected at drop time; a link inside a source folder gets compiled twice; OS symlinks are committed to Git as links (no `.gitignore` automation yet).

Design background (Korean): `docs/DEVELOPMENT_ko.md`.
