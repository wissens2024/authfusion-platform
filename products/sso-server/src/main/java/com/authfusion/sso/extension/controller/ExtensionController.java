package com.authfusion.sso.extension.controller;

import com.authfusion.sso.cc.ConditionalOnExtendedMode;
import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.extension.ExtensionDescriptor;
import com.authfusion.sso.extension.ExtensionRegistry;
import com.authfusion.sso.extension.model.ExtensionInfo;
import com.authfusion.sso.extension.model.ExtensionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ExtendedFeature("확장 관리 API")
@ConditionalOnExtendedMode
@RestController
@RequestMapping("/api/v1/extensions")
@RequiredArgsConstructor
@Tag(name = "Extensions", description = "Extension management API")
public class ExtensionController {

    private final ExtensionRegistry extensionRegistry;

    @GetMapping
    @Operation(summary = "List all extensions", description = "Returns all registered extensions")
    public ResponseEntity<List<ExtensionInfo>> listExtensions() {
        List<ExtensionInfo> extensions = extensionRegistry.getAllExtensions().stream()
                .map(ExtensionInfo::fromDescriptor)
                .toList();
        return ResponseEntity.ok(extensions);
    }

    @GetMapping("/{extensionId}")
    @Operation(summary = "Get extension details", description = "Returns extension details by ID")
    public ResponseEntity<ExtensionInfo> getExtension(@PathVariable String extensionId) {
        return extensionRegistry.getExtension(extensionId)
                .map(ExtensionInfo::fromDescriptor)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "List extensions by type", description = "Returns extensions filtered by type")
    public ResponseEntity<List<ExtensionInfo>> listByType(@PathVariable ExtensionType type) {
        List<ExtensionInfo> extensions = extensionRegistry.getExtensionsByType(type).stream()
                .map(ExtensionInfo::fromDescriptor)
                .toList();
        return ResponseEntity.ok(extensions);
    }
}
