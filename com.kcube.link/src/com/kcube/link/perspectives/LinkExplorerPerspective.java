package com.kcube.link.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Link Explorer 를 왼쪽에 배치한 퍼스펙티브.
 */
public class LinkExplorerPerspective implements IPerspectiveFactory {

	/** 퍼스펙티브 ID */
	public static final String ID = "com.kcube.link.perspective";

	/** Link Explorer 뷰 ID */
	private static final String VIEW_EXPLORER = "com.kcube.link.views.explorer";

	/**
	 * 초기 레이아웃을 구성한다.
	 *
	 * @param layout 페이지 레이아웃
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();

		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.25f, editorArea);
		left.addView(VIEW_EXPLORER);

		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.75f, editorArea);
		bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
		bottom.addView("org.eclipse.ui.console.ConsoleView");

		layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, 0.75f, editorArea);

		layout.addShowViewShortcut(VIEW_EXPLORER);
		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
	}
}
