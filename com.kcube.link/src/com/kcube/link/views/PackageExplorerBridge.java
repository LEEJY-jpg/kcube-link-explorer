package com.kcube.link.views;

import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Package Explorer 의 최상위 표시 방식과 활성 Working Set 을 읽어오는 브리지.
 * <p>
 * 해당 정보는 JDT 내부 클래스({@code PackageExplorerPart}, {@code WorkingSetModel})에만 있으므로
 * 컴파일 의존 없이 리플렉션으로 접근한다. 읽지 못하면 null 을 돌려주고 호출 측이 기본 동작으로 대체한다.
 */
final class PackageExplorerBridge {

	/** Package Explorer 뷰 ID ({@code JavaUI.ID_PACKAGES}) */
	static final String VIEW_ID = "org.eclipse.jdt.ui.PackageExplorer";

	/** Package Explorer 가 Working Set 을 최상위로 표시하는 모드 값 ({@code WORKING_SETS_AS_ROOTS}) */
	private static final int WORKING_SETS_AS_ROOTS = 2;

	/** 로거 */
	private static final Logger _log = LoggerFactory.getLogger(PackageExplorerBridge.class);

	private PackageExplorerBridge() {
	}

	/**
	 * Package Explorer 의 어느 시점 상태.
	 *
	 * @param workingSetsAsRoots Working Set 을 최상위로 보여주는지 여부
	 * @param activeWorkingSets  활성 Working Set (표시 순서대로)
	 */
	record Snapshot(boolean workingSetsAsRoots, List<IWorkingSet> activeWorkingSets) {
	}

	/**
	 * 이미 생성된 Package Explorer 뷰를 찾는다.
	 *
	 * @param page 워크벤치 페이지
	 * @return 뷰 (열린 적이 없으면 null)
	 */
	static IViewPart find(IWorkbenchPage page) {
		return page == null ? null : page.findView(VIEW_ID);
	}

	/**
	 * Package Explorer 의 현재 상태를 읽는다.
	 *
	 * @param part Package Explorer 뷰
	 * @return 상태 (읽지 못하면 null)
	 */
	static Snapshot read(IViewPart part) {
		try {
			Object model = workingSetModel(part);
			int mode = (Integer) part.getClass().getMethod("getRootMode").invoke(part);
			IWorkingSet[] active = (IWorkingSet[]) model.getClass().getMethod("getActiveWorkingSets").invoke(model);
			return new Snapshot(mode == WORKING_SETS_AS_ROOTS, List.of(active));
		} catch (ReflectiveOperationException | RuntimeException e) {
			if (_log.isDebugEnabled()) {
				_log.debug("Cannot read the Package Explorer state", e);
			}
			return null;
		}
	}

	/**
	 * Package Explorer 의 Working Set 모델에 변경 감시자를 등록한다.
	 *
	 * @param part     Package Explorer 뷰
	 * @param listener 감시자
	 * @return 감시자를 해제하는 동작 (등록하지 못하면 null)
	 */
	static Runnable addListener(IViewPart part, IPropertyChangeListener listener) {
		try {
			Object model = workingSetModel(part);
			Method add = model.getClass().getMethod("addPropertyChangeListener", IPropertyChangeListener.class);
			Method remove = model.getClass().getMethod("removePropertyChangeListener",
					IPropertyChangeListener.class);
			add.invoke(model, listener);
			return () -> {
				try {
					remove.invoke(model, listener);
				} catch (ReflectiveOperationException | RuntimeException e) {
					if (_log.isDebugEnabled()) {
						_log.debug("Cannot remove the working set listener", e);
					}
				}
			};
		} catch (ReflectiveOperationException | RuntimeException e) {
			if (_log.isDebugEnabled()) {
				_log.debug("Cannot listen to the Package Explorer working sets", e);
			}
			return null;
		}
	}

	/**
	 * Package Explorer 의 Working Set 모델을 가져온다.
	 *
	 * @param part Package Explorer 뷰
	 * @return {@code WorkingSetModel}
	 */
	private static Object workingSetModel(IViewPart part) throws ReflectiveOperationException {
		Object model = part.getClass().getMethod("getWorkingSetModel").invoke(part);
		if (model == null) {
			throw new IllegalStateException("Package Explorer has no working set model yet");
		}
		return model;
	}
}
