package com.minecraftabnormals.caverns_and_chasms.core.mixin;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.passive.horse.ZombieHorseEntity;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(ZombieHorseEntity.class)
public abstract class ZombieHorseEntityMixin extends AbstractHorseEntity {
	private static final DataParameter<Boolean> CONVERTING = EntityDataManager.createKey(ZombieHorseEntity.class, DataSerializers.BOOLEAN);
	private int conversionTime;
	private UUID converstionStarter;

	protected ZombieHorseEntityMixin(EntityType<? extends AbstractHorseEntity> type, World worldIn) {
		super(type, worldIn);
	}

	protected void registerData() {
		super.registerData();
		this.dataManager.register(CONVERTING, false);
	}

	@Override
	public void writeAdditional(CompoundNBT compound) {
		super.writeAdditional(compound);
		compound.putInt("ConversionTime", this.isConverting() ? this.conversionTime : -1);
		if (this.converstionStarter != null) {
			compound.putUniqueId("ConversionPlayer", this.converstionStarter);
		}
	}

	@Override
	public void readAdditional(CompoundNBT compound) {
		super.readAdditional(compound);
		if (compound.contains("ConversionTime", 99) && compound.getInt("ConversionTime") > -1) {
			this.startConverting(compound.hasUniqueId("ConversionPlayer") ? compound.getUniqueId("ConversionPlayer") : null, compound.getInt("ConversionTime"));
		}
	}

	@Override
	public void tick() {
		if (!this.world.isRemote && this.isAlive() && this.isConverting()) {
			int i = this.getConversionProgress();
			this.conversionTime -= i;
			if (this.conversionTime <= 0 && ForgeEventFactory.canLivingConvert(this, EntityType.HORSE, (timer) -> this.conversionTime = timer)) {
				this.cureZombie((ServerWorld) this.world);
			}
		}

		super.tick();
	}

	@Inject(at = @At("HEAD"), method = "func_230254_b_", cancellable = true)
	public void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResultType> cir) {
		ItemStack itemstack = player.getHeldItem(hand);
		if (itemstack.getItem() == Items.GOLDEN_APPLE) {
			if (this.isPotionActive(Effects.WEAKNESS)) {
				if (!player.abilities.isCreativeMode) {
					itemstack.shrink(1);
				}

				if (!this.world.isRemote()) {
					this.startConverting(player.getUniqueID(), this.rand.nextInt(2401) + 3600);
				}

				cir.setReturnValue(ActionResultType.SUCCESS);
			} else {
				cir.setReturnValue(ActionResultType.CONSUME);
			}
		}
	}

	public boolean isConverting() {
		return this.getDataManager().get(CONVERTING);
	}

	private void startConverting(@Nullable UUID conversionStarterIn, int conversionTimeIn) {
		this.converstionStarter = conversionStarterIn;
		this.conversionTime = conversionTimeIn;
		this.getDataManager().set(CONVERTING, true);
		this.removePotionEffect(Effects.WEAKNESS);
		this.addPotionEffect(new EffectInstance(Effects.STRENGTH, conversionTimeIn, Math.min(this.world.getDifficulty().getId() - 1, 0)));
		this.world.setEntityState(this, (byte) 16);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void handleStatusUpdate(byte id) {
		if (id == 16) {
			if (!this.isSilent()) {
				this.world.playSound(this.getPosX(), this.getPosYEye(), this.getPosZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, this.getSoundCategory(), 1.0F + this.rand.nextFloat(), this.rand.nextFloat() * 0.7F + 0.3F, false);
			}

		} else {
			super.handleStatusUpdate(id);
		}
	}

	private void cureZombie(ServerWorld world) {
		HorseEntity horseEntity = this.copyEntityData();
		horseEntity.onInitialSpawn(world, world.getDifficultyForLocation(horseEntity.getPosition()), SpawnReason.CONVERSION, null, null);
		horseEntity.addPotionEffect(new EffectInstance(Effects.NAUSEA, 200, 0));
		if (!this.isSilent()) {
			world.playEvent(null, 1027, this.getPosition(), 0);
		}

		ForgeEventFactory.onLivingConvert(this, horseEntity);
	}

	public HorseEntity copyEntityData() {
		HorseEntity horse = this.func_233656_b_(EntityType.HORSE, true);
		horse.setHorseTamed(this.isTame());
		horse.setOwnerUniqueId(this.getOwnerUniqueId());
		return horse;
	}

	private int getConversionProgress() {
		int i = 1;
		if (this.rand.nextFloat() < 0.01F) {
			int j = 0;
			BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

			for (int k = (int) this.getPosX() - 4; k < (int) this.getPosX() + 4 && j < 14; ++k) {
				for (int l = (int) this.getPosY() - 4; l < (int) this.getPosY() + 4 && j < 14; ++l) {
					for (int i1 = (int) this.getPosZ() - 4; i1 < (int) this.getPosZ() + 4 && j < 14; ++i1) {
						Block block = this.world.getBlockState(blockpos$mutable.setPos(k, l, i1)).getBlock();
						if (block.isIn(BlockTags.CARPETS) || block instanceof BedBlock) {
							if (this.rand.nextFloat() < 0.3F) {
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