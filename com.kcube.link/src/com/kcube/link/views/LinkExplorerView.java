package com.kcube.link.views;

import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.IExtensionStateModel;

/**
 * Link Explorer 뷰. Package Explorer 의 Working Set 설정(최상위 표시 방식과 활성 Working Set)을 따라 보여준다.
 * <p>
 * Working Set 자체는 워크벤치 전역({@code IWorkingSetManager})에서 관리되므로 같은 Working Set 이 나타나고,
 * 어떤 것을 보여줄지는 {@link PackageExplorerBridge} 로 Package Explorer 에서 읽어 온다.
 * Package Explorer 를 읽을 수 없으면 등록된 모든 Working Set 을 보여준다.
 * <p>
 * 기본 {@code CommonNavigator} 는 선택된 Working Set 이 하나도 없으면 "Top Level Elements &gt; Working Sets"
 * 로 바꿔도 뷰어 입력을 바꾸지 않는다(Project Explorer 는 전용 보조 코드가 있다).
 * 그래서 뷰어 입력은 이 클래스가 직접 설정한다.
 */
public class LinkExplorerView extends CommonNavigator {

	/** Working Set 콘텐츠 확장 ID */
	private static final String WORKING_SETS_EXTENSION_ID = "org.eclipse.ui.navigator.resources.workingSets";

	/** 최상위 요소를 Working Set 으로 보일지 여부를 담는 확장 상태 속성 */
	private static final String SHOW_TOP_LEVEL_WORKING_SETS = WORKING_SETS_EXTENSION_ID
			+ ".showTopLevelWorkingSets";

	/** Working Set 콘텐츠 확장의 상태 모델 */
	private IExtensionStateModel _state;

	/** 이 뷰가 설정한 Working Set 묶음 입력 (Projects 표시이거나 사용자가 직접 골랐으면 null) */
	private IWorkingSet _managedInput;

	/** 마지막으로 반영한 Package Explorer 상태 (변경이 없으면 입력을 다시 만들지 않기 위함) */
	private PackageExplorerBridge.Snapshot _applied;

	/** 모드·입력을 이 클래스가 바꾸는 중인지 여부 (감시자 재진입 방지) */
	private boolean _applying;

	/** 현재 연결된 Package Explorer 뷰 */
	private IViewPart _packageExplorer;

	/** Package Explorer 모델 감시자 해제 동작 */
	private Runnable _removeModelListener;

	/** 최상위 모드 변경 감시자 */
	private final IPropertyChangeListener _modeListener = this::onModeChanged;

	/** 워크벤치 Working Set 변경 감시자 */
	private final IPropertyChangeListener _managerListener = event -> asyncSync(true);

	/** Package Explorer Working Set 모델 변경 감시자 */
	private final IPropertyChangeListener _modelListener = event -> asyncSync(false);

	/** Package Explorer 의 열림·활성화와 이 뷰의 활성화를 감시한다. */
	private final IPartListener2 _partListener = new IPartListener2() {
		/**
		 * Package Explorer 가 열리면 연결하고, 이 뷰가 보이게 되면 Package Explorer 의 최신 상태를 반영한다.
		 *
		 * @param ref 파트 참조
		 */
		@Override
		public void partVisible(IWorkbenchPartReference ref) {
			if (PackageExplorerBridge.VIEW_ID.equals(ref.getId()) || ref.getId().equals(getViewSite().getId())) {
				asyncSync(false);
			}
		}

		/**
		 * Package Explorer 에서 다른 곳으로 포커스가 옮겨지면 상태를 반영한다 (최상위 모드 변경은 이벤트가 없어 이때 따라간다).
		 *
		 * @param ref 파트 참조
		 */
		@Override
		public void partDeactivated(IWorkbenchPartReference ref) {
			if (PackageExplorerBridge.VIEW_ID.equals(ref.getId())) {
				asyncSync(false);
			}
		}
	};

	/**
	 * 뷰를 만든 뒤 Package Explorer 의 Working Set 설정을 반영하고 감시자를 등록한다.
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
		_state.addPropertyChangeListener(_modeListener);
		PlatformUI.getWorkbench().getWorkingSetManager().addPropertyChangeListener(_managerListener);
		getSite().getPage().addPartListener(_partListener);
		sync(false);
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
		IWorkbenchPage page = getSite().getPage();
		if (page != null) {
			page.removePartListener(_partListener);
		}
		unhookPackageExplorer();
		super.dispose();
	}

	/**
	 * UI 스레드에서 상태를 반영한다.
	 *
	 * @param refreshIfSame true 면 Package Explorer 상태가 같아도 트리를 새로 고친다 (Working Set 내용 변경용)
	 */
	private void asyncSync(boolean refreshIfSame) {
		Display.getDefault().asyncExec(() -> sync(refreshIfSame));
	}

	/**
	 * Package Explorer 의 최상위 표시 방식과 활성 Working Set 을 읽어 뷰어 입력에 반영한다.
	 * Package Explorer 를 읽을 수 없으면 등록된 모든 Working Set 을 보여준다.
	 * <p>
	 * 이전에 반영한 상태와 같으면 입력을 다시 설정하지 않는다(트리 펼침 상태 유지).
	 * 따라서 사용자가 이 뷰에서 직접 고른 Working Set 은 Package Explorer 가 바뀌기 전까지 유지된다.
	 *
	 * @param refreshIfSame true 면 상태가 같아도 트리를 새로 고친다
	 */
	private void sync(boolean refreshIfSame) {
		CommonViewer viewer = getCommonViewer();
		if (viewer == null || viewer.getControl() == null || viewer.getControl().isDisposed()) {
			return;
		}
		hookPackageExplorer();
		PackageExplorerBridge.Snapshot snapshot = _packageExplorer == null ? null
				: PackageExplorerBridge.read(_packageExplorer);
		if (snapshot == null) {
			snapshot = new PackageExplorerBridge.Snapshot(true, List.of(allWorkingSets()));
		}
		if (snapshot.equals(_applied)) {
			if (refreshIfSame) {
				viewer.refresh();
			}
			return;
		}
		_applied = snapshot;
		apply(snapshot);
	}

	/**
	 * 상태를 뷰어에 적용한다.
	 *
	 * @param snapshot Package Explorer 상태
	 */
	private void apply(PackageExplorerBridge.Snapshot snapshot) {
		CommonViewer viewer = getCommonViewer();
		_applying = true;
		try {
			_state.setBooleanProperty(SHOW_TOP_LEVEL_WORKING_SETS, snapshot.workingSetsAsRoots());
			if (snapshot.workingSetsAsRoots()) {
				IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
				IWorkingSet[] sets = snapshot.activeWorkingSets().toArray(IWorkingSet[]::new);
				_managedInput = manager.createAggregateWorkingSet("", "", sets);
				viewer.setInput(_managedInput);
			} else {
				_managedInput = null;
				viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
			}
		} finally {
			_applying = false;
		}
	}

	/**
	 * 이미 열려 있는 Package Explorer 에 연결한다. 열리기 전이면 열릴 때(partVisible) 다시 시도한다.
	 */
	private void hookPackageExplorer() {
		IViewPart part = PackageExplorerBridge.find(getSite().getPage());
		if (part == _packageExplorer) {
			return;
		}
		unhookPackageExplorer();
		_packageExplorer = part;
		if (part != null) {
			_removeModelListener = PackageExplorerBridge.addListener(part, _modelListener);
		}
	}

	/**
	 * Package Explorer 와의 연결을 끊는다.
	 */
	private void unhookPackageExplorer() {
		if (_removeModelListener != null) {
			_removeModelListener.run();
			_removeModelListener = null;
		}
		_packageExplorer = null;
	}

	/**
	 * 워크벤치에 등록된 모든 Working Set.
	 *
	 * @return Working Set 배열
	 */
	private static IWorkingSet[] allWorkingSets() {
		return PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
	}

	/**
	 * 뷰 메뉴에서 Top Level Elements 를 사용자가 바꿨을 때 입력을 맞춘다.
	 * Working Set 으로 바꾸면 Package Explorer 의 활성 Working Set(없으면 전체)을 보여준다.
	 *
	 * @param event 속성 변경 이벤트
	 */
	private void onModeChanged(PropertyChangeEvent event) {
		if (_applying || !SHOW_TOP_LEVEL_WORKING_SETS.equals(event.getProperty())) {
			return;
		}
		PackageExplorerBridge.Snapshot snapshot = _packageExplorer == null ? null
				: PackageExplorerBridge.read(_packageExplorer);
		List<IWorkingSet> sets = snapshot != null && !snapshot.activeWorkingSets().isEmpty()
				? snapshot.activeWorkingSets()
				: List.of(allWorkingSets());
		boolean showing = _state.getBooleanProperty(SHOW_TOP_LEVEL_WORKING_SETS);
		// 사용자의 선택이므로 이후 Package Explorer 가 바뀌기 전까지 유지되도록 마지막 반영 상태도 갱신한다.
		_applied = new PackageExplorerBridge.Snapshot(showing, sets);
		apply(_applied);
	}
}
