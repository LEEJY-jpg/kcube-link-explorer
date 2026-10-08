package com.kcube.link.navigator;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * 아무 라벨도 제공하지 않는 라벨 프로바이더. null 을 돌려주면 다른 확장의 라벨이 사용된다.
 */
public class NullLabelProvider extends LabelProvider {

	/**
	 * @param element 요소
	 * @return 항상 null
	 */
	@Override
	public String getText(Object element) {
		return null;
	}

	/**
	 * @param element 요소
	 * @return 항상 null
	 */
	@Override
	public Image getImage(Object element) {
		return null;
	}
}
