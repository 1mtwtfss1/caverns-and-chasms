package com.minecraftabnormals.caverns_and_chasms.client.render;

import com.minecraftabnormals.caverns_and_chasms.client.model.MimeArmorModel;
import com.minecraftabnormals.caverns_and_chasms.client.model.MimeModel;
import com.minecraftabnormals.caverns_and_chasms.common.entity.MimeEntity;
import com.minecraftabnormals.caverns_and_chasms.core.CavernsAndChasms;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public class MimeRenderer extends BipedRenderer<MimeEntity, MimeModel<MimeEntity>> {
	private static final ResourceLocation MIME_LOCATION = new ResourceLocation(CavernsAndChasms.MODID, "textures/entity/mime.png");

	public MimeRenderer(EntityRendererManager renderManagerIn) {
		super(renderManagerIn, new MimeModel<MimeEntity>(0.0F), 0.5F);
		this.addLayer(new BipedArmorLayer<>(this, new MimeArmorModel<MimeEntity>(0.5F), new MimeArmorModel<MimeEntity>(1.0F)));
	}

	@Override
	public ResourceLocation getEntityTexture(MimeEntity entity) {
		return MIME_LOCATION;
	}

	@Override
	protected void applyRotations(MimeEntity entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks) {
		float f = entityLiving.getSwimAnimation(partialTicks);
		super.applyRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
		if (f > 0.0F) {
			float f3 = entityLiving.isInWater() ? -90.0F - entityLiving.rotationPitch : -90.0F;
			float f4 = MathHelper.lerp(f, 0.0F, f3);
			matrixStackIn.rotate(Vector3f.XP.rotationDegrees(f4));
			if (entityLiving.isActualySwimming()) {
				matrixStackIn.translate(0.0D, -1.0D, (double)0.3F);
			}
		}
	}
}