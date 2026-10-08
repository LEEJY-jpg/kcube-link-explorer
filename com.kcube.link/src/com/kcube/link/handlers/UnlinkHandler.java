package com.kcube.link.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.kcube.link.Activator;
import com.kcube.link.core.LinkService;

/**
 * 선택한 링크를 해제하는 컨텍스트 메뉴 핸들러. 링크 자체만 지우고 원본은 보존한다.
 */
public class UnlinkHandler extends AbstractHandler {

	/**
	 * 선택 중인 링크를 확인 후 해제한다.
	 *
	 * @param event 실행 이벤트
	 * @return 항상 null
	 */
	@Override
	public Object execute(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (!(selection instanceof IStructuredSelection structured)) {
			return null;
		}
		List<IResource> links = structured.stream()
				.map(o -> Adapters.adapt(o, IResource.class))
				.filter(LinkService::isLink)
				.toList();
		if (links.isEmpty()) {
			return null;
		}
		boolean ok = MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), "Unlink",
				"Remove " + links.size() + " link(s)? The original projects are not deleted.");
		if (!ok) {
			return null;
		}
		WorkspaceJob job = new WorkspaceJob("Unlink") {
			/**
			 * 선택한 링크를 모두 해제한다.
			 *
			 * @param monitor 진행 모니터
			 * @return 실패가 있으면 실패 목록을 담은 상태
			 */
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) {
				MultiStatus result = new MultiStatus(Activator.PLUGIN_ID, 0, "Unlink failed", null);
				for (IResource link : links) {
					try {
						LinkService.unlink(link, monitor);
					} catch (CoreException e) {
						result.add(e.getStatus());
					}
				}
				return result.isOK() ? Status.OK_STATUS : result;
			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}
}
