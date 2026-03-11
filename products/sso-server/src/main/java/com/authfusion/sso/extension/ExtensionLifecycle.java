package com.authfusion.sso.extension;

/**
 * 확장 생명주기 인터페이스.
 * 모든 Extension SPI 구현체는 이 인터페이스를 구현하여
 * 초기화, 종료 등 생명주기 이벤트를 처리할 수 있습니다.
 */
public interface ExtensionLifecycle {

    /**
     * 확장 초기화. Registry에 등록된 후 호출됩니다.
     */
    default void onInit() {}

    /**
     * 확장 종료. Registry에서 해제되기 전 호출됩니다.
     */
    default void onDestroy() {}

    /**
     * 확장 활성화 여부를 반환합니다.
     */
    default boolean isEnabled() {
        return true;
    }
}
