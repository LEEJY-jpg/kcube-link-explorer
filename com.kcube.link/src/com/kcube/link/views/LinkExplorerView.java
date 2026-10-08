package com.kcube.link.views;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.IExtensionStateModel;

/**
 * Link Explorer 뷰. Package Explorer 처럼 워크스페이스 공용 Working Set 을 최상위 요소로 보여준다.
 * <p>
 * Working Set 자체는 워크벤치 전역({@code IWorkingSetManager})에서 관리되므로
 * Package Explorer 와 같은 Working Set 이 그대로 나타난다.
 * <p>
 * 기본 {@code CommonNavigator} 는 선택된 Working Set 이 하나도 없으면 "Top Level Elements &gt; Working Sets"
 * 로 바꿔도 뷰어 입력을 바꾸지 않는다(Project Explorer 는 전용 보조 코드가 있다).
 * 그래서 선택된 Working Set 이 없을 때는 모든 Working Set 을 묶은 입력을 이 클래스가 직접 설정한다.
 */
public class LinkExplorerView extends CommonNavigator {

	/** Working Set 콘텐츠 확장 ID */
	private static final String WORKING_SETS_EXTENSION_ID = "org.eclipse.ui.navigator.resources.workingSets";

	/** 최상위 요소를 Working Set 으로 보일지 여부를 담는 확장 상태 속성 */
	private static final String SHOW_TOP_LEVEL_WORKING_SETS = WORKING_SETS_EXTENSION_ID
			+ ".showTopLevelWorkingSets";

	/** 저장된 뷰 상태가 있었는지 여부 (처음 열 때만 기본값을 적용하기 위함) */
	private boolean _restored;

	/** Working Set 콘텐츠 확장의 상태 모델 */
	private IExtensionStateModel _state;

	/** 모든 Working Set 을 묶은 기본 입력 (Working Set 이 없으면 null) */
	private IWorkingSet _allWorkingSets;

	/** 최상위 모드 변경 감시자 */
	private final IPropertyChangeListener _modeListener = this::onModeChanged;

	/** 워크벤치 Working Set 변경 감시자 */
	private final IPropertyChangeListener _managerListener = this::onWorkingSetsChanged;

	/**
	 * 저장된 상태가 있는지 기억해 둔다.
	 *
	 * @param site    뷰 사이트
	 * @param memento 저장된 상태 (처음 열면 null)
	 */
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		_restored = memento != null;
		super.init(site, memento);
	}

	/**
	 * 뷰를 만든 뒤 Working Set 최상위 표시를 설정하고 변경 감시자를 등록한다.
	 *
	 * @param aParent 부모 컴포지트
	 */
	@Override
	public void createPartControl(Composite aParent) {
		super.createPartControl(aParent);
		_state = getCommonViewer().getNavigatorContentService().findStateModel(WORKING_SETS_EXTENSION_ID);
		if (_state == null) {
			return;
		}
		if (!_restored) {
			_state.setBooleanProperty(SHOW_TOP_LEVEL_WORKING_SETS, true);
		}
		_state.addPropertyChangeListener(_modeListener);
		PlatformUI.getWorkbench().getWorkingSetManager().addPropertyChangeListener(_managerListener);
		applyDefaultInput();
	}

	/**
	 * 감시자를 해제한다.
	 */
	@Override
	public void dispose() {
		if (_state != null) {
			_state.removePropertyChangeListener(_modeListener);
		}
		PlatformUI.getWorkbench().getWorkingSetManager().removePropertyChangeListener(_managerListener);
		super.dispose();
	}

	/**
	 * 최상위 모드가 Working Set 이면서 사용자가 고른 Working Set 도 없을 때 모든 Working Set 을 입력으로 쓴다.
	 * 사용자가 Working Set 을 골랐다면 뷰어 입력이 이미 다른 값이므로 건드리지 않는다.
	 */
	private void applyDefaultInput() {
		CommonViewer viewer = getCommonViewer();
		if (viewer == null || viewer.getControl().isDisposed() || !isShowingWorkingSets()) {
			return;
		}
		if (viewer.getInput() == ResourcesPlugin.getWorkspace().getRoot() || viewer.getInput() == _allWorkingSets) {
			IWorkingSet all = createAllWorkingSets();
			if (all != null) {
				_allWorkingSets = all;
				viewer.setInput(all);
			}
		}
	}

	/**
	 * Projects 모드에서는 입력이 항상 워크스페이스 루트이어야 한다.
	 */
	private void applyProjectsInput() {
		CommonViewer viewer = getCommonViewer();
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		Object root = ResourcesPlugin.getWorkspace().getRoot();
		if (viewer.getInput() != root) {
			_allWorkingSets = null;
			viewer.setInput(root);
		}
	}

	/**
	 * 최상위 요소가 Working Set 인지 확인한다.
	 *
	 * @return Working Set 이면 true
	 */
	private boolean isShowingWorkingSets() {
		return _state != null && _state.getBooleanProperty(SHOW_TOP_LEVEL_WORKING_SETS);
	}

	/**
	 * 워크벤치에 등록된 모든 Working Set 을 하나로 묶는다.
	 *
	 * @return 묶음 (Working Set 이 없으면 null)
	 */
	private static IWorkingSet createAllWorkingSets() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSet[] sets = manager.getWorkingSets();
		return sets.length == 0 ? null : manager.createAggregateWorkingSet("", "", sets);
	}

	/**
	 * 뷰 메뉴에서 Top Level Elements 가 바뀌었을 때 입력을 맞춘다.
	 *
	 * @param event 속성 변경 이벤트
	 */
	private void onModeChanged(PropertyChangeEvent event) {
		if (!SHOW_TOP_LEVEL_WORKING_SETS.equals(event.getProperty())) {
			return;
		}
		if (isShowingWorkingSets()) {
			applyDefaultInput();
		} else {
			applyProjectsInput();
		}
	}

	/**
	 * Working Set 이 추가·삭제·변경되면 모든 Working Set 묶음을 다시 만든다.
	 *
	 * @param event 속성 변경 이벤트
	 */
	private void onWorkingSetsChanged(PropertyChangeEvent event) {
		Display display = Display.getDefault();
		display.asyncExec(() -> {
			CommonViewer viewer = getCommonViewer();
			if (viewer == null || viewer.getControl().isDisposed()) {
				return;
			}
			// 기본 입력을 쓰는 중이거나(변경 반영), 프로젝트 입력이면서 Working Set 이 막 생긴 경우만 갱신한다.
			if (viewer.getInput() == _allWorkingSets) {
				_allWorkingSets = null;
				viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
				applyDefaultInput();
			} else {
				applyDefaultInput();
			}
		});
	}
}
