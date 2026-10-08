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

	/** 마지막으로 검사한 드롭 대상 (드래그 중 매 틱마다 같은 입력이면 재검사를 건너뛰기 위함) */
	private IContainer _lastDest;

	/** 마지막으로 검사한 선택 항목 (참조 동일성으로 비교; 드래그 중에는 같은 인스턴스가 유지된다) */
	private IStructuredSelection _lastSelection;

	/** 마지막 검사 결과 */
	private boolean _lastResult;

	/**
	 * 드롭 가능 여부를 검사한다.
	 * <p>
	 * SWT DND 는 드래그 중 마우스가 움직일 때마다 이 메서드를 호출한다. 같은 대상·선택이면
	 * {@link LinkService#validate}(디스크 I/O 포함)를 반복하지 않고 마지막 결과를 재사용한다.
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
			_lastDest = null;
			_lastSelection = null;
			return Status.CANCEL_STATUS;
		}
		if (LocalSelectionTransfer.getTransfer().getSelection() instanceof IStructuredSelection sel) {
			if (dest.equals(_lastDest) && sel == _lastSelection) {
				return _lastResult ? Status.OK_STATUS : Status.CANCEL_STATUS;
			}
			boolean ok = isDroppable(sel, dest);
			_lastDest = dest;
			_lastSelection = sel;
			_lastResult = ok;
			return ok ? Status.OK_STATUS : Status.CANCEL_STATUS;
		}
		_lastDest = null;
		_lastSelection = null;
		return Status.CANCEL_STATUS;
	}

	/**
	 * 선택한 모든 항목을 대상에 링크할 수 있는지 검사한다.
	 *
	 * @param sel  선택 항목
	 * @param dest 드롭 대상
	 * @return 모두 가능하면 true
	 */
	private boolean isDroppable(IStructuredSelection sel, IContainer dest) {
		for (Object o : sel) {
			IProject src = Adapters.adapt(o, IProject.class);
			// 프로젝트가 아닌 항목이 섞여 있으면 기본 이동·복사 동작에 맡긴다.
			if (src == null || !LinkService.validate(src, dest).isOK()) {
				return false;
			}
		}
		return true;
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
		_lastDest = null;
		_lastSelection = null;
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
