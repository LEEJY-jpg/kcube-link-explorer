package com.kcube.link.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import com.kcube.link.Activator;
import com.kcube.link.core.LinkMode;

/**
 * 환경설정 기본값을 지정한다.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/**
	 * 기본 링크 방식을 OS 심볼릭 링크로 지정한다.
	 */
	@Override
	public void initializeDefaultPreferences() {
		Activator.getDefault().getPreferenceStore().setDefault(Activator.PREF_LINK_MODE, LinkMode.SYMLINK.name());
	}
}
