package dev.shadowsoffire.gateways.data;

import java.util.concurrent.CompletableFuture;

import dev.shadowsoffire.gateways.GatewayObjects;
import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.gateways.gate.GatewayRegistry;
import dev.shadowsoffire.placebo.datagen.LegacyRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;

public class GatewayRecipeProvider extends LegacyRecipeProvider {

    public GatewayRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Gateways.MODID);
    }

    @Override
    public String getName() {
        return "Gateway Recipes";
    }

    @Override
    protected void genRecipes(RecipeOutput output, HolderLookup.Provider registries) {
        addGatewayRecipe("basic/blaze", 3, 3,
            "c:rods/blaze", "c:rods/blaze", "c:rods/blaze",
            "c:rods/blaze", "c:ender_pearls", "c:rods/blaze",
            Items.NETHER_BRICKS, Items.NETHER_BRICKS, Items.NETHER_BRICKS);

        addGatewayRecipe("basic/enderman", 3, 3,
            "c:gems/amethyst", "c:ender_pearls", "c:gems/amethyst",
            "c:ender_pearls", Items.ENDER_EYE, "c:ender_pearls",
            "c:gems/amethyst", "c:ender_pearls", "c:gems/amethyst");

        addGatewayRecipe("basic/slime", 3, 3,
            "c:dusts/redstone", "c:dusts/redstone", "c:dusts/redstone",
            Items.RED_MUSHROOM, "c:ender_pearls", Items.BROWN_MUSHROOM,
            potionIngredient(Potions.WATER), potionIngredient(Potions.WATER), potionIngredient(Potions.WATER));

        addGatewayRecipe("emerald_grove", 3, 3,
            "minecraft:flowers", "minecraft:flowers", "minecraft:flowers",
            "minecraft:flowers", Items.ENDER_EYE, "minecraft:flowers",
            "minecraft:flowers", "minecraft:flowers", "minecraft:flowers");

        addGatewayRecipe("hellish_fortress", 3, 3,
            Items.ROTTEN_FLESH, Items.WITHER_SKELETON_SKULL, Items.ROTTEN_FLESH,
            "c:storage_blocks/gold", Items.ENDER_EYE, "c:storage_blocks/gold",
            "c:rods/blaze", "c:rods/blaze", "c:rods/blaze");

        addGatewayRecipe("overworldian_nights", 3, 3,
            "c:dusts/glowstone", Items.SPIDER_EYE, "c:dusts/glowstone",
            "c:gunpowders", Items.ENDER_EYE, "c:gunpowders",
            "c:bones", Items.ROTTEN_FLESH, "c:bones");

        addGatewayRecipe("endless/blaze", 3, 3,
            "c:gems/emerald", "c:gems/emerald", "c:gems/emerald",
            "c:rods/blaze", gatePearlIngredient("basic/blaze"), "c:rods/blaze",
            Items.MAGMA_CREAM, Items.MAGMA_CREAM, Items.MAGMA_CREAM);
    }

    private void addGatewayRecipe(String gatewayPath, int width, int height, Object... input) {
        Identifier key = Gateways.loc(gatewayPath);
        DataComponentPatch patch = DataComponentPatch.builder()
            .set(GatewayObjects.GATEWAY_COMPONENT, GatewayRegistry.INSTANCE.holder(key))
            .build();
        ItemStackTemplate result = new ItemStackTemplate(GatewayObjects.GATE_PEARL.value().builtInRegistryHolder(), 1, patch);
        addShaped(key, "gateways", result, width, height, input);
    }

    private static net.minecraft.world.item.crafting.Ingredient gatePearlIngredient(String gatewayPath) {
        DataComponentPatch patch = DataComponentPatch.builder()
            .set(GatewayObjects.GATEWAY_COMPONENT, GatewayRegistry.INSTANCE.holder(Gateways.loc(gatewayPath)))
            .build();
        ItemStackTemplate template = new ItemStackTemplate(GatewayObjects.GATE_PEARL.value().builtInRegistryHolder(), 1, patch);
        return net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(false, template);
    }
}
