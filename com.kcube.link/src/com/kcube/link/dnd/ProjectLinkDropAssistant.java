package com.kcube.link.dnd;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
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

import com.kcube.link.Activator;
import com.kcube.link.core.LinkMode;
import com.kcube.link.core.LinkService;

/**
 * 다른 프로젝트를 드롭하면 대상 폴더에 링크를 생성하는 드롭 어시스턴트.
 */
public class ProjectLinkDropAssistant extends CommonDropAdapterAssistant {

	/** 로거 */
	private static final Logger _log = LoggerFactory.getLogger(ProjectLinkDropAssistant.class);

	/**
	 * 드롭 가능 여부를 검사한다.
	 *
	 * @param target       드롭 대상 (IJavaProject 등도 IContainer 로 어댑트)
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
		if (LocalSelectionTransfer.getTransfer().getSelection() instanceof IStructuredSelection sel) {
			for (Object o : sel) {
				IProject src = Adapters.adapt(o, IProject.class);
				// 프로젝트가 아닌 항목이 섞여 있으면 기본 이동·복사 동작에 맡긴다.
				if (src == null || !LinkService.validate(src, dest).isOK()) {
					return Status.CANCEL_STATUS;
				}
			}
			return Status.OK_STATUS;
		}
		return Status.CANCEL_STATUS;
	}

	/**
	 * 드롭 처리: 선택된 프로젝트마다 링크 생성 작업을 실행한다.
	 *
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
		LinkMode mode = Activator.getLinkMode();
		for (Object o : sel) {
			IProject src = Adapters.adapt(o, IProject.class);
			if (src != null) {
				scheduleLinkJob(src, dest, mode);
			}
		}
		return Status.OK_STATUS;
	}

	/**
	 * 워크스페이스 잠금 하에서 링크를 생성하는 Job 을 스케줄한다.
	 *
	 * @param src  링크 원본 프로젝트
	 * @param dest 링크를 만들 대상 컨테이너
	 * @param mode 링크 방식
	 */
	private void scheduleLinkJob(IProject src, IContainer dest, LinkMode mode) {
		WorkspaceJob job = new WorkspaceJob("Link " + src.getName()) {
			/**
			 * 링크 생성 작업 본체.
			 *
			 * @param monitor 진행 모니터
			 * @return 작업 결과 상태
			 */
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				try {
					LinkService.link(src, dest, mode, monitor);
					return Status.OK_STATUS;
				} catch (CoreException e) {
					if (_log.isErrorEnabled()) {
						_log.error("Failed to link {} into {}", src.getName(), dest.getFullPath(), e);
					}
					return e.getStatus();
				}
			}
		};
		job.setRule(dest.getProject());
		job.setUser(true);
		job.schedule();
	}
}
