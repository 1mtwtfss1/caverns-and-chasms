package com.teamabnormals.caverns_and_chasms.client.render;

import com.teamabnormals.caverns_and_chasms.client.model.RatModel;
import com.teamabnormals.caverns_and_chasms.client.render.layer.RatHeldItemLayer;
import com.teamabnormals.caverns_and_chasms.common.entity.Rat;
import com.teamabnormals.caverns_and_chasms.common.entity.Rat.RatType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RatRenderer extends MobRenderer<Rat, RatModel<Rat>> {

	public RatRenderer(EntityRendererProvider.Context context) {
		super(context, new RatModel<>(RatModel.createLayerDefinition().bakeRoot()), 0.2F);
		this.addLayer(new RatHeldItemLayer(this));
	}

	@Override
	public ResourceLocation getTextureLocation(Rat entity) {
		RatType type = RatType.byId(entity.getRatType());
		return type.getTextureLocation();
	}
}