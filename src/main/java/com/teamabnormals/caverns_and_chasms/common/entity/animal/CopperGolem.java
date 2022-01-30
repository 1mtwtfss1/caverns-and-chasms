package com.teamabnormals.caverns_and_chasms.common.entity.animal;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.teamabnormals.caverns_and_chasms.common.block.CopperButtonBlock;
import com.teamabnormals.caverns_and_chasms.core.CavernsAndChasms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class CopperGolem extends AbstractGolem {
	public static final int HEAD_SPIN_TIME = 16;
	public static final int PRESS_ANIM_TIME = 12;
	
	private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("A8EF581F-B1E8-4950-860C-06FA72505003");
	private static final EntityDataAccessor<Integer> OXIDATION = SynchedEntityData.defineId(CopperGolem.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> WAXED = SynchedEntityData.defineId(CopperGolem.class, EntityDataSerializers.BOOLEAN);

	private int ticksSinceButtonPress;

	private float headSpinTicks;
	private float headSpinTicksO;
	private float buttonPressTicks;
	private float buttonPressTicksO;

	public CopperGolem(EntityType<? extends AbstractGolem> entity, Level level) {
		super(entity, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new CopperGolem.PressButtonGoal());
		this.goalSelector.addGoal(1, new CopperGolem.RandomWalkingGoal());
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(OXIDATION, 0);
		this.entityData.define(WAXED, false);
	}

	public static AttributeSupplier.Builder registerAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0D).add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.KNOCKBACK_RESISTANCE, 0.25D);
	}

	@Override
	protected float getStandingEyeHeight(Pose pose, EntityDimensions size) {
		return size.height * 0.7F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSource) {
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.IRON_GOLEM_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putInt("Oxidation", this.getOxidation().getId());
		compound.putBoolean("Waxed", this.isWaxed());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		this.setOxidation(Oxidation.byId(compound.getInt("Oxidation")));
		this.setWaxed(compound.getBoolean("Waxed"));
	}

	public Oxidation getOxidation() {
		return Oxidation.byId(this.entityData.get(OXIDATION));
	}

	public void setOxidation(Oxidation oxidation) {
		this.entityData.set(OXIDATION, oxidation.getId());

		AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
		attributeinstance.removeModifier(SPEED_MODIFIER_UUID);
		if (oxidation != Oxidation.UNAFFECTED) {
			double penalty = oxidation == Oxidation.EXPOSED ? -0.05D : oxidation == Oxidation.WEATHERED ? -0.1D : -0.25D;
			attributeinstance.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_UUID, "Weathering speed penalty", penalty, AttributeModifier.Operation.ADDITION));
		}
	}

	public boolean isWaxed() {
		return this.entityData.get(WAXED);
	}

	public void setWaxed(boolean waxed) {
		this.entityData.set(WAXED, waxed);
	}

	@Override
	public void tick() {
		super.tick();

		this.updateHeadSpinTicks();
		this.updatePressButtonTicks();
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (this.level.isClientSide) {
			if (this.isWaxed() && this.tickCount % 30 == 0) {
				double d0 = Mth.nextDouble(this.getRandom(), -1.0D, 1.0D);
				double d1 = Mth.nextDouble(this.getRandom(), -1.0D, 1.0D);
				double d2 = Mth.nextDouble(this.getRandom(), -1.0D, 1.0D);
				this.level.addParticle(ParticleTypes.WAX_ON, this.getRandomX(0.8D), this.getY(0.1D), this.getRandomZ(0.8D), d0, d1, d2);
			}
		} else {
			if (this.ticksSinceButtonPress > 0) {
				--this.ticksSinceButtonPress;
			}
		}
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack itemstack = player.getItemInHand(hand);
		Item item = itemstack.getItem();
		boolean success = false;
		if (item == Items.HONEYCOMB) {
			if (!this.isWaxed()) {
				this.setWaxed(true);
				this.spawnSparkParticles(ParticleTypes.WAX_ON);
				level.playSound(player, this.getX(), this.getY(), this.getZ(), SoundEvents.HONEYCOMB_WAX_ON, SoundSource.NEUTRAL, 1.0F, 1.0F);
				itemstack.shrink(1);
				success = true;
			}
		} else if ((item instanceof AxeItem)) {
			if (this.isWaxed()) {
				this.setWaxed(false);
				this.spawnSparkParticles(ParticleTypes.WAX_OFF);
				level.playSound(player, this.getX(), this.getY(), this.getZ(), SoundEvents.AXE_WAX_OFF, SoundSource.NEUTRAL, 1.0F, 1.0F);
				success = true;
			} else if (this.deoxidize()) {
				this.spawnSparkParticles(ParticleTypes.SCRAPE);
				level.playSound(player, this.getX(), this.getY(), this.getZ(), SoundEvents.AXE_SCRAPE, SoundSource.NEUTRAL, 1.0F, 1.0F);
				success = true;
			}

			if (success) {
				itemstack.hurtAndBreak(1, player, (entity) -> {
					entity.broadcastBreakEvent(hand);
				});
			}
		}

		return success ? InteractionResult.sidedSuccess(this.level.isClientSide) : InteractionResult.PASS;
	}

	@Override
	public void thunderHit(ServerLevel level, LightningBolt lightningBolt) {
		if (this.getOxidation() != Oxidation.UNAFFECTED) {
			this.setOxidation(Oxidation.UNAFFECTED);
		}

		this.spinHead(4, false);
		this.level.broadcastEntityEvent(this, (byte)6);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source)) {
			return false;
		} else {
			this.spinHead(1, false);
			this.level.broadcastEntityEvent(this, (byte)4);
			return super.hurt(source, amount);
		}
	}

	public boolean oxidize() {
		return this.changeOxidation(1);
	}

	public boolean deoxidize() {
		return this.changeOxidation(-1);
	}

	private boolean changeOxidation(int amount) {
		Oxidation oxidation = this.getOxidation();
		this.setOxidation(Oxidation.byId(Mth.clamp(oxidation.getId() + amount, 0, 3)));
		return this.getOxidation() != oxidation;
	}

	private void spinHead(int spins, boolean resetAnimation) {
		if (this.headSpinTicks <= HEAD_SPIN_TIME / 2 || resetAnimation) {
			this.headSpinTicks = spins * HEAD_SPIN_TIME + 8;
			this.headSpinTicksO = this.headSpinTicks;
		}
	}

	private void updateHeadSpinTicks() {
		this.headSpinTicksO = this.headSpinTicks;
		if (this.headSpinTicks > 0) {
			--this.headSpinTicks;
		}
	}

	private void updatePressButtonTicks() {
		this.buttonPressTicksO = this.buttonPressTicks;
		if (this.buttonPressTicks > 0) {
			--this.buttonPressTicks;
		}
	}

	@OnlyIn(Dist.CLIENT)
	public float getHeadSpinTicks(float partialTicks) {
		return Mth.lerp(partialTicks, this.headSpinTicksO, this.headSpinTicks);
	}

	@OnlyIn(Dist.CLIENT)
	public float getPressButtonTicks(float partialTicks) {
		return Mth.lerp(partialTicks, this.buttonPressTicksO, this.buttonPressTicks);
	}

	private void spawnSparkParticles(ParticleOptions particle) {
		if (this.level.isClientSide) {
			for (int i = 0; i < 7; ++i) {
				double d0 = Mth.nextDouble(this.getRandom(), -1.0D, 1.0D);
				double d1 = Mth.nextDouble(this.getRandom(), -1.0D, 1.0D);
				double d2 = Mth.nextDouble(this.getRandom(), -1.0D, 1.0D);
				this.level.addParticle(particle, this.getRandomX(1.0D), this.getRandomY(), this.getRandomZ(1.0D), d0, d1, d2);
			}
		}
	}

	public void handleEntityEvent(byte id) {
		if (id == 4) {
			this.spinHead(1, false);
		} else if (id == 6) {
			this.spinHead(4, false);
			this.spawnSparkParticles(ParticleTypes.ELECTRIC_SPARK);
		} else if (id == 8) {
			this.buttonPressTicks = PRESS_ANIM_TIME;
			this.buttonPressTicksO = this.buttonPressTicks;
		}
		super.handleEntityEvent(id);
	}

	class RandomWalkingGoal extends WaterAvoidingRandomStrollGoal {
		public RandomWalkingGoal() {
			super(CopperGolem.this, 1.0D);
		}

		@Override
		public boolean canUse() {
			return CopperGolem.this.ticksSinceButtonPress > 0 ? false : super.canUse();
		}
	}

	class PressButtonGoal extends Goal {
		private BlockPos blockPos = BlockPos.ZERO;
		private Direction buttonDirection;
		private int nextStartTicks;
		private int tryTicks;
		private int maxStayTicks;
		private int pressWaitTicks;

		public PressButtonGoal() {
			super();
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (this.nextStartTicks > 0) {
				--this.nextStartTicks;
				return false;
			} else {
				this.nextStartTicks = 20;
				return this.findRandomButton();
			}
		}

		@Override
		public boolean canContinueToUse() {
			if (this.tryTicks >= -this.maxStayTicks && this.tryTicks <= 1200) {
				return this.isUnpressedButton(CopperGolem.this.level, this.blockPos);
			} else {
				return false;
			}
		}

		@Override
		public void start() {
			this.moveMobToBlock();
			this.tryTicks = 0;
			this.maxStayTicks = CopperGolem.this.getRandom().nextInt(CopperGolem.this.getRandom().nextInt(1200) + 1200) + 1200;
			this.pressWaitTicks = 10 + PRESS_ANIM_TIME / 2;
		}

		@Override
		public void tick() {
			Vec3i normal = this.buttonDirection.getNormal();
			CopperGolem.this.getLookControl().setLookAt(this.blockPos.getX() + 0.5D + normal.getX() * 0.5D, this.blockPos.getY() + 0.5D + normal.getY() * 0.5D, this.blockPos.getZ() + 0.5D + normal.getZ() * 0.5D, 10.0F, CopperGolem.this.getMaxHeadXRot());

			if (!this.blockPos.closerThan(CopperGolem.this.position(), 1.0D)) {
				++this.tryTicks;
				this.pressWaitTicks = 10 + PRESS_ANIM_TIME / 2;
				if (this.tryTicks % 40 == 0) {
					this.moveMobToBlock();
				}
			} else {
				--this.tryTicks;
				--this.pressWaitTicks;
				
				if (this.pressWaitTicks == PRESS_ANIM_TIME / 2) {
					CopperGolem.this.level.broadcastEntityEvent(CopperGolem.this, (byte)8);
				} else if (this.pressWaitTicks <= 0) {
					BlockState state = CopperGolem.this.level.getBlockState(this.blockPos);
					if (!state.getValue(CopperButtonBlock.POWERED)) {
						((CopperButtonBlock) state.getBlock()).press(state, CopperGolem.this.level, this.blockPos);
						CopperGolem.this.level.playSound(null, this.blockPos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
						CopperGolem.this.level.gameEvent(CopperGolem.this, GameEvent.BLOCK_PRESS, this.blockPos);
						CopperGolem.this.ticksSinceButtonPress = 80;
					}
				}
			}
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		private void moveMobToBlock() {
			CopperGolem.this.getNavigation().moveTo(CopperGolem.this.getNavigation().createPath(this.blockPos.getX() + 0.5D, this.blockPos.getY(), this.blockPos.getZ() + 0.5D, 0), 1.0D);
		}

		private boolean findRandomButton() {
			List<BlockPos> buttonpositions = Lists.newArrayList();

			for(BlockPos pos : BlockPos.betweenClosed(Mth.floor(CopperGolem.this.getX() - 8.0D), Mth.floor(CopperGolem.this.getY() - 4.0D), Mth.floor(CopperGolem.this.getZ() - 8.0D), Mth.floor(CopperGolem.this.getX() + 8.0D), Mth.floor(CopperGolem.this.getY() + 4.0D), Mth.floor(CopperGolem.this.getZ() + 8.0D))) {
				if (CopperGolem.this.isWithinRestriction(pos) && this.isUnpressedButton(CopperGolem.this.level, pos)) {
					buttonpositions.add(new BlockPos(pos));
				}
			}

			if (buttonpositions.size() > 0) {
				this.blockPos = buttonpositions.get(CopperGolem.this.getRandom().nextInt(buttonpositions.size()));
				BlockState state = CopperGolem.this.level.getBlockState(this.blockPos);
				AttachFace face = state.getValue(CopperButtonBlock.FACE);
				this.buttonDirection = face == AttachFace.CEILING ? Direction.UP : face == AttachFace.FLOOR ? Direction.DOWN : state.getValue(CopperButtonBlock.FACING).getOpposite();
				return true;
			}

			return false;
		}

		private boolean isUnpressedButton(LevelReader level, BlockPos pos) {
			BlockState state = level.getBlockState(pos);
			BlockPos belowpos = pos.below();
			BlockState belowstate = level.getBlockState(belowpos);
			return state.getBlock() instanceof CopperButtonBlock && !state.getValue(CopperButtonBlock.POWERED) && level.getBlockState(pos).isPathfindable(level, pos, PathComputationType.LAND) && belowstate.entityCanStandOn(level, belowpos, CopperGolem.this);
		}
	}

	public enum Oxidation {
		UNAFFECTED(0),
		EXPOSED(1),
		WEATHERED(2),
		OXIDIZED(3);

		private static final Oxidation[] VALUES = Arrays.stream(values()).sorted(Comparator.comparingInt(Oxidation::getId)).toArray(Oxidation[]::new);

		private final int id;
		private final LazyLoadedValue<ResourceLocation> textureLocation = new LazyLoadedValue<>(() -> new ResourceLocation(CavernsAndChasms.MOD_ID, "textures/entity/copper_golem/copper_golem_" + this.name().toLowerCase(Locale.ROOT) + ".png"));

		Oxidation(int id) {
			this.id = id;
		}

		public int getId() {
			return this.id;
		}

		public ResourceLocation getTextureLocation() {
			return this.textureLocation.get();
		}

		public static Oxidation byId(int id) {
			if (id < 0 || id >= VALUES.length) {
				id = 0;
			}

			return VALUES[id];
		}
	}
}