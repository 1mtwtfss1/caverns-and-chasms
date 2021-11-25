package com.teamabnormals.caverns_and_chasms.core.data.server;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.teamabnormals.caverns_and_chasms.core.CavernsAndChasms;
import com.teamabnormals.caverns_and_chasms.core.registry.CCBlocks;
import com.teamabnormals.caverns_and_chasms.core.registry.CCItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTable.Builder;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CCLootTableProvider extends LootTableProvider {
	private final List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, Builder>>>, LootContextParamSet>> tables = ImmutableList.of(Pair.of(BlockProvider::new, LootContextParamSets.BLOCK));

	public CCLootTableProvider(DataGenerator generator) {
		super(generator);
	}

	@Override
	public List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, Builder>>>, LootContextParamSet>> getTables() {
		return tables;
	}

	@Override
	protected void validate(Map<ResourceLocation, LootTable> map, ValidationContext context) {
	}

	private static class BlockProvider extends BlockLoot {
		@Override
		public void addTables() {
			this.dropSelf(CCBlocks.RAW_SILVER_BLOCK.get());
			this.add(CCBlocks.SILVER_ORE.get(), (block) -> createOreDrop(block, CCItems.RAW_SILVER.get()));
			this.add(CCBlocks.DEEPSLATE_SILVER_ORE.get(), (block) -> createOreDrop(block, CCItems.RAW_SILVER.get()));
			this.add(CCBlocks.SPINEL_ORE.get(), BlockProvider::createSpinelOreDrops);
			this.add(CCBlocks.DEEPSLATE_SPINEL_ORE.get(), BlockProvider::createSpinelOreDrops);
		}

		protected static LootTable.Builder createSpinelOreDrops(Block block) {
			return createSilkTouchDispatchTable(block, applyExplosionDecay(block, LootItem.lootTableItem(CCItems.SPINEL.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 6.0F))).apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))));
		}

		@Override
		public Iterable<Block> getKnownBlocks() {
			return ForgeRegistries.BLOCKS.getValues().stream().filter(block ->
					block == CCBlocks.RAW_SILVER_BLOCK.get() || block == CCBlocks.SILVER_ORE.get() || block == CCBlocks.DEEPSLATE_SILVER_ORE.get() || block == CCBlocks.SPINEL_ORE.get() || block == CCBlocks.DEEPSLATE_SPINEL_ORE.get()
			).collect(Collectors.toSet());
		}
	}
}