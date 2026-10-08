package com.kcube.link;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.kcube.link.core.LinkMode;

/**
 * 플러그인 생명주기와 공용 상수를 관리하는 Activator.
 */
public class Activator extends AbstractUIPlugin {

	/** 번들 ID */
	public static final String PLUGIN_ID = "com.kcube.link";

	/** 링크 방식 환경설정 키 (값은 {@link LinkMode#name()}) */
	public static final String PREF_LINK_MODE = "linkMode";

	/** 단일 인스턴스 */
	private static Activator _instance;

	/**
	 * 번들 시작 시 인스턴스를 보관한다.
	 *
	 * @param context 번들 컨텍스트
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		_instance = this;
	}

	/**
	 * 번들 종료 시 인스턴스를 해제한다.
	 *
	 * @param context 번들 컨텍스트
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		_instance = null;
		super.stop(context);
	}

	/**
	 * 단일 인스턴스를 반환한다.
	 *
	 * @return Activator (번들이 시작되지 않았으면 null)
	 */
	public static Activator getDefault() {
		return _instance;
	}

	/**
	 * 환경설정에 저장된 링크 방식을 읽는다. 값이 없거나 잘못됐으면 기본값을 돌려준다.
	 *
	 * @return 현재 링크 방식
	 */
	public static LinkMode getLinkMode() {
		Activator activator = getDefault();
		if (activator == null) {
			return LinkMode.SYMLINK;
		}
		IPreferenceStore store = activator.getPreferenceStore();
		return LinkMode.parse(store.getString(PREF_LINK_MODE));
	}
}
