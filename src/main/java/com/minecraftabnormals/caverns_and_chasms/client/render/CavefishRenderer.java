package com.minecraftabnormals.caverns_and_chasms.client.render;

import com.minecraftabnormals.caverns_and_chasms.client.model.CavefishModel;
import com.minecraftabnormals.caverns_and_chasms.common.entity.CavefishEntity;
import com.minecraftabnormals.caverns_and_chasms.core.CavernsAndChasms;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CavefishRenderer extends MobRenderer<CavefishEntity, CavefishModel<CavefishEntity>> {
	private static final ResourceLocation CAVEFISH_LOCATION = new ResourceLocation(CavernsAndChasms.MOD_ID, "textures/entity/cavefish.png");

	public CavefishRenderer(EntityRendererManager renderManagerIn) {
		super(renderManagerIn, new CavefishModel<>(), 0.25F);
	}

	@Override
	public ResourceLocation getTextureLocation(CavefishEntity entity) {
		return CAVEFISH_LOCATION;
	}

	@Override
	protected void setupRotations(CavefishEntity entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
		super.setupRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
		float f = 4.3F * MathHelper.sin(0.6F * ageInTicks);
		matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(f));
		if (!entityLiving.isInWater()) {
			matrixStackIn.translate(0.1F, 0.1F, -0.1F);
			matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
		}
	}
}