package com.authfusion.sso.extension;

import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.extension.model.ExtensionStatus;
import com.authfusion.sso.extension.model.ExtensionType;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 확장 레지스트리. 모든 Extension SPI 구현체를 관리합니다.
 * CC 모드에서는 {@link com.authfusion.sso.cc.ConditionalOnExtendedMode}로 비활성화됩니다.
 * {@link com.authfusion.sso.extension.config.ExtensionAutoConfiguration}에서 빈으로 생성됩니다.
 */
@ExtendedFeature("확장 레지스트리")
@Slf4j
public class ExtensionRegistry {

    private final Map<String, ExtensionDescriptor> extensions = new ConcurrentHashMap<>();

    public void register(ExtensionDescriptor descriptor) {
        if (descriptor.getId() == null || descriptor.getId().isBlank()) {
            throw new IllegalArgumentException("Extension ID is required");
        }
        if (extensions.containsKey(descriptor.getId())) {
            throw new IllegalStateException("Extension already registered: " + descriptor.getId());
        }

        descriptor.setStatus(ExtensionStatus.REGISTERED);

        // Initialize if lifecycle-aware
        if (descriptor.getInstance() instanceof ExtensionLifecycle lifecycle) {
            try {
                lifecycle.onInit();
                descriptor.setStatus(ExtensionStatus.ACTIVE);
            } catch (Exception e) {
                log.error("Extension init failed: {}", descriptor.getId(), e);
                descriptor.setStatus(ExtensionStatus.ERROR);
            }
        } else {
            descriptor.setStatus(ExtensionStatus.ACTIVE);
        }

        extensions.put(descriptor.getId(), descriptor);
        log.info("Extension registered: {} (type={})", descriptor.getId(), descriptor.getType());
    }

    public void unregister(String extensionId) {
        ExtensionDescriptor descriptor = extensions.remove(extensionId);
        if (descriptor != null && descriptor.getInstance() instanceof ExtensionLifecycle lifecycle) {
            try {
                lifecycle.onDestroy();
            } catch (Exception e) {
                log.warn("Extension destroy failed: {}", extensionId, e);
            }
        }
        log.info("Extension unregistered: {}", extensionId);
    }

    public Optional<ExtensionDescriptor> getExtension(String extensionId) {
        return Optional.ofNullable(extensions.get(extensionId));
    }

    public List<ExtensionDescriptor> getExtensionsByType(ExtensionType type) {
        return extensions.values().stream()
                .filter(d -> d.getType() == type)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getExtensionInstances(Class<T> spiInterface) {
        return extensions.values().stream()
                .filter(d -> spiInterface.isInstance(d.getInstance()))
                .filter(d -> d.getStatus() == ExtensionStatus.ACTIVE)
                .map(d -> (T) d.getInstance())
                .collect(Collectors.toList());
    }

    public List<ExtensionDescriptor> getAllExtensions() {
        return new ArrayList<>(extensions.values());
    }

    @PreDestroy
    public void shutdown() {
        extensions.values().forEach(descriptor -> {
            if (descriptor.getInstance() instanceof ExtensionLifecycle lifecycle) {
                try {
                    lifecycle.onDestroy();
                } catch (Exception e) {
                    log.warn("Extension cleanup failed: {}", descriptor.getId(), e);
                }
            }
        });
        extensions.clear();
        log.info("Extension registry shut down");
    }
}
