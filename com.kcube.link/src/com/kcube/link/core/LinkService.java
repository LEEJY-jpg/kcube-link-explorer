package com.kcube.link.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kcube.link.Activator;

/**
 * 프로젝트 링크 생성·해제·검증 로직. UI 에 의존하지 않는다.
 */
public final class LinkService {

	/** 로거 */
	private static final Logger _log = LoggerFactory.getLogger(LinkService.class);

	private LinkService() {
	}

	/**
	 * 원본 프로젝트를 대상 컨테이너 아래에 링크할 수 있는지 검사한다.
	 *
	 * @param src  링크 원본 프로젝트
	 * @param dest 링크를 만들 대상 컨테이너
	 * @return 가능하면 OK, 아니면 사유를 담은 상태
	 */
	public static IStatus validate(IProject src, IContainer dest) {
		if (src.equals(dest.getProject())) {
			return error("A project cannot be linked into itself: " + src.getName(), null);
		}
		if (src.getLocation() == null || dest.getLocation() == null) {
			return error("Project location is not available", null);
		}
		try {
			Path srcPath = src.getLocation().toPath().toRealPath();
			Path destPath = dest.getLocation().toPath().toRealPath();
			// 대상이 원본 트리 안에 있으면 원본 안에 원본을 넣는 순환이 된다.
			if (destPath.startsWith(srcPath)) {
				return error("Circular link: " + dest.getFullPath() + " is inside " + src.getName(), null);
			}
			// 원본 최상위에 이미 대상 프로젝트를 가리키는 링크가 있으면 서로 링크하는 순환이 된다.
			Path destProjectPath = dest.getProject().getLocation().toPath().toRealPath();
			if (containsLinkTo(srcPath, destProjectPath)) {
				return error("Circular link: " + src.getName() + " already links to " + dest.getProject().getName(),
						null);
			}
		} catch (IOException e) {
			return error("Failed to inspect project locations", e);
		}
		return Status.OK_STATUS;
	}

	/**
	 * 링크를 생성한다.
	 *
	 * @param src     링크 원본 프로젝트
	 * @param dest    링크를 만들 대상 컨테이너
	 * @param mode    링크 방식
	 * @param monitor 진행 모니터
	 * @throws CoreException 검증 실패 또는 생성 실패
	 */
	public static void link(IProject src, IContainer dest, LinkMode mode, IProgressMonitor monitor)
			throws CoreException {
		IStatus status = validate(src, dest);
		if (!status.isOK()) {
			throw new CoreException(status);
		}
		switch (mode) {
			case SYMLINK -> createOsSymlink(src, dest, monitor);
			case LINKED_RESOURCE -> createEclipseLink(src, dest, monitor);
		}
	}

	/**
	 * 리소스가 링크(OS 심볼릭 링크 또는 Linked Resource)인지 확인한다.
	 *
	 * @param resource 검사할 리소스
	 * @return 링크이면 true
	 */
	public static boolean isLink(IResource resource) {
		if (resource == null || resource.getType() == IResource.PROJECT || resource.getType() == IResource.ROOT) {
			return false;
		}
		if (resource.isLinked()) {
			return true;
		}
		IPath location = resource.getLocation();
		return location != null && Files.isSymbolicLink(location.toPath());
	}

	/**
	 * 링크를 해제한다. 링크 자체만 지우며 원본 내용은 건드리지 않는다.
	 *
	 * @param resource 해제할 링크 리소스
	 * @param monitor  진행 모니터
	 * @throws CoreException 해제 실패
	 */
	public static void unlink(IResource resource, IProgressMonitor monitor) throws CoreException {
		if (resource.isLinked()) {
			// Linked Resource 는 delete 가 링크 정의만 제거한다.
			resource.delete(IResource.NONE, monitor);
		} else {
			Path link = resource.getLocation().toPath();
			try {
				// 심볼릭 링크를 따라가지 않고 링크 파일만 삭제한다.
				Files.delete(link);
			} catch (IOException e) {
				throw new CoreException(error("Failed to remove symlink " + link, e));
			}
			resource.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
		}
		if (_log.isInfoEnabled()) {
			_log.info("Link removed: {}", resource.getFullPath());
		}
	}

	/**
	 * OS 레벨 심볼릭 링크를 생성하고 대상 폴더를 리프레시한다.
	 *
	 * @param src     원본 프로젝트
	 * @param dest    대상 컨테이너
	 * @param monitor 진행 모니터
	 */
	private static void createOsSymlink(IProject src, IContainer dest, IProgressMonitor monitor)
			throws CoreException {
		Path link = dest.getLocation().toPath().resolve(src.getName());
		Path original = src.getLocation().toPath();
		if (Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
			throw new CoreException(error("Already exists: " + link, null));
		}
		try {
			Files.createSymbolicLink(link, original);
		} catch (IOException | UnsupportedOperationException e) {
			throw new CoreException(error("Failed to create symlink " + link
					+ " (Windows needs administrator rights or developer mode)", e));
		}
		dest.refreshLocal(IResource.DEPTH_ONE, monitor);
		if (_log.isInfoEnabled()) {
			_log.info("Symlink created: {} -> {}", link, original);
		}
	}

	/**
	 * Eclipse Linked Resource(.project 기록 방식)로 링크를 생성한다.
	 *
	 * @param src     원본 프로젝트
	 * @param dest    대상 컨테이너
	 * @param monitor 진행 모니터
	 */
	private static void createEclipseLink(IProject src, IContainer dest, IProgressMonitor monitor)
			throws CoreException {
		IFolder folder = dest.getFolder(IPath.fromOSString(src.getName()));
		if (folder.exists()) {
			throw new CoreException(error("Already exists: " + folder.getFullPath(), null));
		}
		folder.createLink(src.getLocationURI(), IResource.ALLOW_MISSING_LOCAL, monitor);
		if (_log.isInfoEnabled()) {
			_log.info("Linked resource created: {} -> {}", folder.getFullPath(), src.getLocationURI());
		}
	}

	/**
	 * 디렉터리 바로 아래 항목 중 대상 경로를 가리키는 심볼릭 링크가 있는지 확인한다.
	 *
	 * @param dir    검사할 디렉터리
	 * @param target 찾을 실제 경로
	 * @return 있으면 true
	 */
	private static boolean containsLinkTo(Path dir, Path target) throws IOException {
		if (!Files.isDirectory(dir)) {
			return false;
		}
		try (var children = Files.newDirectoryStream(dir)) {
			for (Path child : children) {
				if (Files.isSymbolicLink(child) && child.toRealPath().equals(target)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 오류 상태를 만든다.
	 *
	 * @param message   메시지
	 * @param exception 원인 (없으면 null)
	 * @return 오류 상태
	 */
	private static IStatus error(String message, Throwable exception) {
		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, exception);
	}
}
