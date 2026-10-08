package com.kcube.link.core;

/**
 * 링크 생성 방식.
 */
public enum LinkMode {

	/** OS 심볼릭 링크 (Git·외부 빌드 도구가 인식, Windows 는 권한 필요) */
	SYMLINK("OS symbolic link"),

	/** Eclipse Linked Resource (.project 에 기록, Eclipse 전용) */
	LINKED_RESOURCE("Eclipse linked resource");

	/** 환경설정 화면에 표시할 이름 */
	private final String _label;

	LinkMode(String label) {
		_label = label;
	}

	/**
	 * 화면 표시용 이름을 반환한다.
	 *
	 * @return 표시 이름
	 */
	public String label() {
		return _label;
	}

	/**
	 * 저장된 문자열을 링크 방식으로 변환한다.
	 *
	 * @param value enum 이름 (null·빈 값·알 수 없는 값 허용)
	 * @return 변환된 값. 해석할 수 없으면 {@link #SYMLINK}
	 */
	public static LinkMode parse(String value) {
		for (LinkMode mode : values()) {
			if (mode.name().equals(value)) {
				return mode;
			}
		}
		return SYMLINK;
	}
}
