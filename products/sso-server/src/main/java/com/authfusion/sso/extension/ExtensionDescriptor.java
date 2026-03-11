package com.authfusion.sso.extension;

import com.authfusion.sso.extension.model.ExtensionType;
import com.authfusion.sso.extension.model.ExtensionStatus;
import lombok.*;

/**
 * 확장 메타데이터를 담는 디스크립터.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtensionDescriptor {

    private String id;
    private String name;
    private String description;
    private String version;
    private ExtensionType type;
    private ExtensionStatus status;
    private String vendor;
    private Class<?> spiInterface;
    private Object instance;
}
