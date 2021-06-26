package com.minecraftabnormals.caverns_and_chasms.common.entity.zombie;

import com.minecraftabnormals.caverns_and_chasms.core.registry.CCEntities;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.UUID;

public class ZombieCatEntity extends CatEntity {
	private static final DataParameter<Boolean> CONVERTING = EntityDataManager.defineId(ZombieCatEntity.class, DataSerializers.BOOLEAN);
	private int conversionTime;
	private UUID converstionStarter;

	public ZombieCatEntity(EntityType<? extends ZombieCatEntity> type, World worldIn) {
		super(type, worldIn);
	}

	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(CONVERTING, false);
	}

	@Override
	public ZombieCatEntity getBreedOffspring(ServerWorld world, AgeableEntity entity) {
		ZombieCatEntity cat = CCEntities.ZOMBIE_CAT.get().create(world);
		if (this.random.nextBoolean()) {
			cat.setCatType(this.getCatType());
		} else {
			cat.setCatType(cat.getCatType());
		}

		if (this.isTame()) {
			cat.setOwnerUUID(this.getOwnerUUID());
			cat.setTame(true);
			if (this.random.nextBoolean()) {
				cat.setCollarColor(this.getCollarColor());
			} else {
				cat.setCollarColor(cat.getCollarColor());
			}
		}

		return cat;
	}

	@Override
	public void addAdditionalSaveData(CompoundNBT compound) {
		super.addAdditionalSaveData(compound);
		compound.putInt("ConversionTime", this.isConverting() ? this.conversionTime : -1);
		if (this.converstionStarter != null) {
			compound.putUUID("ConversionPlayer", this.converstionStarter);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundNBT compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("ConversionTime", 99) && compound.getInt("ConversionTime") > -1) {
			this.startConverting(compound.hasUUID("ConversionPlayer") ? compound.getUUID("ConversionPlayer") : null, compound.getInt("ConversionTime"));
		}
	}

	@Override
	public CreatureAttribute getMobType() {
		return CreatureAttribute.UNDEAD;
	}

	@Override
	public void tick() {
		if (!this.level.isClientSide && this.isAlive() && this.isConverting()) {
			int i = this.getConversionProgress();
			this.conversionTime -= i;
			if (this.conversionTime <= 0 && ForgeEventFactory.canLivingConvert(this, EntityType.CAT, (timer) -> this.conversionTime = timer)) {
				this.cureZombie((ServerWorld) this.level);
			}
		}

		super.tick();
	}

	@Override
	public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
		ItemStack itemstack = player.getItemInHand(hand);
		if (itemstack.getItem() == Items.GOLDEN_APPLE) {
			if (this.hasEffect(Effects.WEAKNESS)) {
				if (!player.abilities.instabuild) {
					itemstack.shrink(1);
				}

				if (!this.level.isClientSide) {
					this.startConverting(player.getUUID(), this.random.nextInt(2401) + 3600);
				}

				return ActionResultType.SUCCESS;
			} else {
				return ActionResultType.CONSUME;
			}
		} else {
			return super.mobInteract(player, hand);
		}
	}

	public boolean isConverting() {
		return this.getEntityData().get(CONVERTING);
	}

	private void startConverting(@Nullable UUID conversionStarterIn, int conversionTimeIn) {
		this.converstionStarter = conversionStarterIn;
		this.conversionTime = conversionTimeIn;
		this.getEntityData().set(CONVERTING, true);
		this.removeEffect(Effects.WEAKNESS);
		this.addEffect(new EffectInstance(Effects.DAMAGE_BOOST, conversionTimeIn, Math.min(this.level.getDifficulty().getId() - 1, 0)));
		this.level.broadcastEntityEvent(this, (byte) 16);
	}

	@OnlyIn(Dist.CLIENT)
	public void handleEntityEvent(byte id) {
		if (id == 16) {
			if (!this.isSilent()) {
				this.level.playLocalSound(this.getX(), this.getEyeY(), this.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE, this.getSoundSource(), 1.0F + this.random.nextFloat(), this.random.nextFloat() * 0.7F + 0.3F, false);
			}

		} else {
			super.handleEntityEvent(id);
		}
	}

	private void cureZombie(ServerWorld world) {
		CatEntity catEntity = this.copyEntityData();
		catEntity.finalizeSpawn(world, world.getCurrentDifficultyAt(catEntity.blockPosition()), SpawnReason.CONVERSION, null, null);
		catEntity.setCatType(this.getCatType());
		catEntity.addEffect(new EffectInstance(Effects.CONFUSION, 200, 0));
		if (!this.isSilent()) {
			world.levelEvent(null, 1027, this.blockPosition(), 0);
		}

		ForgeEventFactory.onLivingConvert(this, catEntity);
	}

	public CatEntity copyEntityData() {
		CatEntity cat = this.convertTo(EntityType.CAT, false);
		cat.setCollarColor(this.getCollarColor());
		cat.setTame(this.isTame());
		cat.setOrderedToSit(this.isOrderedToSit());
		if (this.getOwner() != null)
			cat.setOwnerUUID(this.getOwner().getUUID());
		return cat;
	}

	private int getConversionProgress() {
		int i = 1;
		if (this.random.nextFloat() < 0.01F) {
			int j = 0;
			BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

			for (int k = (int) this.getX() - 4; k < (int) this.getX() + 4 && j < 14; ++k) {
				for (int l = (int) this.getY() - 4; l < (int) this.getY() + 4 && j < 14; ++l) {
					for (int i1 = (int) this.getZ() - 4; i1 < (int) this.getZ() + 4 && j < 14; ++i1) {
						Block block = this.level.getBlockState(blockpos$mutable.set(k, l, i1)).getBlock();
						if (block.is(BlockTags.CARPETS) || block instanceof BedBlock) {
							if (this.random.nextFloat() < 0.3F) {
								++i;
							}

							++j;
						}
					}
				}
			}
		}

		return i;
	}
}