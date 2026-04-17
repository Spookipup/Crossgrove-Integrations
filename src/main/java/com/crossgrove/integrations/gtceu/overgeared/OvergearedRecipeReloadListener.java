package com.crossgrove.integrations.gtceu.overgeared;

import com.crossgrove.integrations.mixin.RecipeManagerMixin;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class OvergearedRecipeReloadListener implements PreparableReloadListener {
    private final RecipeManager recipeManager;

    public OvergearedRecipeReloadListener(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                                          ProfilerFiller prepProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.<Void>completedFuture(null)
                .thenCompose(barrier::wait)
                .thenRunAsync(this::applyInjection, gameExecutor);
    }

    private void applyInjection() {
        RecipeManagerMixin accessor = (RecipeManagerMixin) recipeManager;
        CrossgroveOvergearedRecipes.RecipeMaps maps = CrossgroveOvergearedRecipes.inject(
                accessor.crossgrove$getRecipes(), accessor.crossgrove$getByName());
        accessor.crossgrove$setRecipes(maps.recipes());
        accessor.crossgrove$setByName(maps.byName());
    }
}
