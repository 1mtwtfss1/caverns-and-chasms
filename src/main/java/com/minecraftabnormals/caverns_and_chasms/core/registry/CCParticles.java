package com.minecraftabnormals.caverns_and_chasms.core.registry;

import com.minecraftabnormals.caverns_and_chasms.core.CavernsAndChasms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.LavaParticle;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class CCParticles {
	public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, CavernsAndChasms.MODID);

	public static final RegistryObject<BasicParticleType> CURSED_FLAME = createBasicParticleType(true, "cursed_flame");
	public static final RegistryObject<BasicParticleType> CURSED_AMBIENT = createBasicParticleType(true, "cursed_ambient");

	private static RegistryObject<BasicParticleType> createBasicParticleType(boolean alwaysShow, String name) {
		RegistryObject<BasicParticleType> particleType = PARTICLES.register(name, () -> new BasicParticleType(alwaysShow));
		return particleType;
	}

	@EventBusSubscriber(modid = CavernsAndChasms.MODID, bus = EventBusSubscriber.Bus.MOD)
	public static class RegisterParticleFactories {

		@SubscribeEvent(priority = EventPriority.LOWEST)
		public static void registerParticleTypes(ParticleFactoryRegisterEvent event) {
			if (checkForNonNullWithReflectionCauseForgeIsBaby(CURSED_FLAME)) {
				Minecraft.getInstance().particles.registerFactory(CURSED_FLAME.get(), FlameParticle.Factory::new);
			}

			if (checkForNonNullWithReflectionCauseForgeIsBaby(CURSED_AMBIENT)) {
				Minecraft.getInstance().particles.registerFactory(CURSED_AMBIENT.get(), LavaParticle.Factory::new);
			}
		}

	}

	private static boolean checkForNonNullWithReflectionCauseForgeIsBaby(RegistryObject<BasicParticleType> registryObject) {
		return ObfuscationReflectionHelper.getPrivateValue(RegistryObject.class, registryObject, "value") != null;
	}
}