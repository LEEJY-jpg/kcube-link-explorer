package com.kcube.link.navigator;

import org.eclipse.jface.viewers.ITreeContentProvider;

/**
 * 자식을 제공하지 않는 콘텐츠 프로바이더.
 * <p>
 * 이 플러그인의 navigatorContent 는 드롭 어시스턴트를 싣기 위한 용도이며,
 * 트리 내용과 라벨은 JDT·리소스·Working Set 확장이 담당한다. 여기서 자식을 내면 중복 표시된다.
 */
public class EmptyContentProvider implements ITreeContentProvider {

	/** 빈 배열 */
	private static final Object[] NONE = new Object[0];

	/**
	 * @param inputElement 입력 요소
	 * @return 항상 빈 배열
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		return NONE;
	}

	/**
	 * @param parentElement 부모 요소
	 * @return 항상 빈 배열
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		return NONE;
	}

	/**
	 * @param element 요소
	 * @return 항상 null
	 */
	@Override
	public Object getParent(Object element) {
		return null;
	}

	/**
	 * @param element 요소
	 * @return 항상 false
	 */
	@Override
	public boolean hasChildren(Object element) {
		return false;
	}
}
