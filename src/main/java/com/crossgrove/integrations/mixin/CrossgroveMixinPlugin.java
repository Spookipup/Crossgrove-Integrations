package com.crossgrove.integrations.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class CrossgroveMixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE = CrossgroveMixinPlugin.class.getPackageName();
    private static final String JADE_MIXIN = MIXIN_PACKAGE + ".GtceuControllableBlockProviderMixin";
    private static final String LSO_MIXIN = MIXIN_PACKAGE + ".LsoBlockModifierMixin";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (JADE_MIXIN.equals(mixinClassName)) {
            return isLoaded("jade");
        }
        if (LSO_MIXIN.equals(mixinClassName)) {
            return isLoaded("legendarysurvivaloverhaul");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isLoaded(String modId) {
        return FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }
}
