package com.kcube.link.preferences;

import java.util.Arrays;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.kcube.link.Activator;
import com.kcube.link.core.LinkMode;

/**
 * 링크 방식을 선택하는 환경설정 페이지.
 */
public class LinkPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * 페이지를 생성한다.
	 */
	public LinkPreferencePage() {
		super(GRID);
	}

	/**
	 * 번들 환경설정 저장소를 연결한다.
	 *
	 * @param workbench 워크벤치
	 */
	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Link Explorer settings");
	}

	/**
	 * 링크 방식 콤보를 만든다.
	 */
	@Override
	protected void createFieldEditors() {
		String[][] entries = Arrays.stream(LinkMode.values())
				.map(m -> new String[] { m.label(), m.name() })
				.toArray(String[][]::new);
		addField(new ComboFieldEditor(Activator.PREF_LINK_MODE, "Link type on drop:", entries, getFieldEditorParent()));
	}
}
