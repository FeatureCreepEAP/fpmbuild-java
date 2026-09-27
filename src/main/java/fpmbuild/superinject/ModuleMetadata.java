package fpmbuild.superinject;

import java.util.List;

public record ModuleMetadata(
        String moduleEntry,
        String moduleName,
        String originalModId,
        String injectedModId,
        String modIdSource,
        List<String> mixinConfigs) {
    public ModuleMetadata {
        moduleName = moduleName == null ? "" : moduleName;
        originalModId = originalModId == null ? "" : originalModId;
        injectedModId = injectedModId == null ? "" : injectedModId;
        modIdSource = modIdSource == null ? "" : modIdSource;
        mixinConfigs = List.copyOf(mixinConfigs);
    }

    public boolean injectable() {
        return !originalModId.isBlank() && !mixinConfigs.isEmpty();
    }
}
