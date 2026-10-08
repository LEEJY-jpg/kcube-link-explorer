package com.kcube.link.views;

import java.util.List;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreePathViewerSorter;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkingSet;

/**
 * 최상위 Working Set 을 Package Explorer 와 같은 순서로 정렬하는 정렬기.
 * <p>
 * CNF 의 Working Set 정렬기는 항상 이름순이라 Package Explorer 에서 사용자가 정한 순서가 사라진다.
 * Working Set 끼리 비교할 때만 지정된 순서를 쓰고, 그 밖의 모든 비교는 원래 정렬기에 맡긴다.
 */
final class OrderedWorkingSetSorter extends TreePathViewerSorter {

	/** 원래 정렬기 (CNF) */
	private final TreePathViewerSorter _delegate;

	/** Package Explorer 의 Working Set 순서 */
	private List<IWorkingSet> _order = List.of();

	/**
	 * 정렬기를 만든다.
	 *
	 * @param delegate 원래 정렬기
	 */
	OrderedWorkingSetSorter(TreePathViewerSorter delegate) {
		_delegate = delegate;
	}

	/**
	 * 사용할 순서를 지정한다.
	 *
	 * @param order Working Set 순서
	 */
	void setOrder(List<IWorkingSet> order) {
		_order = order;
	}

	/**
	 * @param element 요소
	 * @return 원래 정렬기의 분류
	 */
	@Override
	public int category(Object element) {
		return _delegate.category(element);
	}

	/**
	 * Working Set 두 개를 비교할 때는 지정된 순서를, 그 밖에는 원래 정렬기를 쓴다.
	 *
	 * @param viewer     뷰어
	 * @param parentPath 부모 경로
	 * @param e1         첫째 요소
	 * @param e2         둘째 요소
	 * @return 비교 결과
	 */
	@Override
	public int compare(Viewer viewer, TreePath parentPath, Object e1, Object e2) {
		if (e1 instanceof IWorkingSet w1 && e2 instanceof IWorkingSet w2) {
			int i1 = _order.indexOf(w1);
			int i2 = _order.indexOf(w2);
			if (i1 >= 0 && i2 >= 0) {
				return Integer.compare(i1, i2);
			}
			if (i1 >= 0 || i2 >= 0) {
				return i1 >= 0 ? -1 : 1;
			}
		}
		return _delegate.compare(viewer, parentPath, e1, e2);
	}

	/**
	 * @param parentPath 부모 경로
	 * @param element    요소
	 * @param property   속성
	 * @return 원래 정렬기의 판단
	 */
	@Override
	public boolean isSorterProperty(TreePath parentPath, Object element, String property) {
		return _delegate.isSorterProperty(parentPath, element, property);
	}

	/**
	 * @param element  요소
	 * @param property 속성
	 * @return 원래 정렬기의 판단
	 */
	@Override
	public boolean isSorterProperty(Object element, String property) {
		return _delegate.isSorterProperty(element, property);
	}
}
