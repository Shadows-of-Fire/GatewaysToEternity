package dev.shadowsoffire.gateways.data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import dev.shadowsoffire.gateways.Gateways;
import dev.shadowsoffire.placebo.json.WeightedItemStack;
import dev.shadowsoffire.placebo.systems.gear.GearSet;
import dev.shadowsoffire.placebo.systems.gear.GearSetRegistry;
import dev.shadowsoffire.placebo.util.data.DynamicRegistryProvider;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public class GearSetProvider extends DynamicRegistryProvider<GearSet> {

    public GearSetProvider(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, GearSetRegistry.INSTANCE);
    }

    @Override
    public String getName() {
        return "Gateway Gear Sets";
    }

    @Override
    public void generate() {
        addGearSet("iron",
            List.of(entry(Items.IRON_SWORD)),
            Collections.emptyList(),
            List.of(entry(Items.IRON_BOOTS)),
            List.of(entry(Items.IRON_LEGGINGS)),
            List.of(entry(Items.IRON_CHESTPLATE)),
            List.of(entry(Items.IRON_HELMET)));

        addGearSet("iron_with_diamond",
            List.of(entry(Items.DIAMOND_SWORD)),
            List.of(entry(Items.SHIELD)),
            List.of(entry(Items.IRON_BOOTS)),
            List.of(entry(Items.IRON_LEGGINGS)),
            List.of(entry(Items.IRON_CHESTPLATE)),
            List.of(entry(Items.IRON_HELMET)));

        addGearSet("chain_with_bow",
            List.of(entry(Items.BOW)),
            Collections.emptyList(),
            List.of(entry(Items.CHAINMAIL_BOOTS)),
            List.of(entry(Items.CHAINMAIL_LEGGINGS)),
            List.of(entry(Items.CHAINMAIL_CHESTPLATE)),
            List.of(entry(Items.CHAINMAIL_HELMET)));

        addGearSet("gold_with_axe",
            List.of(entry(Items.GOLDEN_AXE)),
            Collections.emptyList(),
            List.of(entry(Items.GOLDEN_BOOTS)),
            List.of(entry(Items.GOLDEN_LEGGINGS)),
            List.of(entry(Items.GOLDEN_CHESTPLATE)),
            List.of(entry(Items.GOLDEN_HELMET)));
    }

    private void addGearSet(String name, List<WeightedItemStack> mainhands, List<WeightedItemStack> offhands,
        List<WeightedItemStack> boots, List<WeightedItemStack> leggings, List<WeightedItemStack> chestplates, List<WeightedItemStack> helmets) {
        this.add(Gateways.loc(name), new GearSet(0, 0, mainhands, offhands, boots, leggings, chestplates, helmets));
    }

    @SuppressWarnings("deprecation")
    private static WeightedItemStack entry(ItemLike item) {
        return new WeightedItemStack(Optional.of(new ItemStackTemplate(item.asItem().builtInRegistryHolder(), 1, DataComponentPatch.EMPTY)), 1, 0F);
    }
}
