package com.minecraftabnormals.caverns_and_chasms.client.render;

import com.minecraftabnormals.abnormals_core.client.ACRenderTypes;
import com.minecraftabnormals.caverns_and_chasms.client.DeeperSpriteUploader;
import com.minecraftabnormals.caverns_and_chasms.client.model.DeeperModel;
import com.minecraftabnormals.caverns_and_chasms.common.entity.DeeperEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DeeperRenderLayer extends LayerRenderer<DeeperEntity, DeeperModel<DeeperEntity>> {
	private final DeeperModel<DeeperEntity> model;

	public DeeperRenderLayer(IEntityRenderer<DeeperEntity, DeeperModel<DeeperEntity>> renderer) {
		super(renderer);
		this.model = new DeeperModel<>(true, 0.0F);
	}

	@Override
	public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, DeeperEntity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		this.model.setupAnim(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		this.model.renderToBuffer(matrixStackIn, bufferIn.getBuffer(ACRenderTypes.getEmissiveTransluscentEntity(DeeperSpriteUploader.ATLAS_LOCATION, false)), packedLightIn, LivingRenderer.getOverlayCoords(entitylivingbaseIn, 0.0F), 1, 1, 1, 1);
	}
}