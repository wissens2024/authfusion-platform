package com.authfusion.sso.extension.model;

import com.authfusion.sso.extension.ExtensionDescriptor;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtensionInfo {

    private String id;
    private String name;
    private String description;
    private String version;
    private ExtensionType type;
    private ExtensionStatus status;
    private String vendor;

    public static ExtensionInfo fromDescriptor(ExtensionDescriptor descriptor) {
        return ExtensionInfo.builder()
                .id(descriptor.getId())
                .name(descriptor.getName())
                .description(descriptor.getDescription())
                .version(descriptor.getVersion())
                .type(descriptor.getType())
                .status(descriptor.getStatus())
                .vendor(descriptor.getVendor())
                .build();
    }
}
