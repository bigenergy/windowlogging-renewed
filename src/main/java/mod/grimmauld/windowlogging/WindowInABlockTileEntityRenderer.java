package mod.grimmauld.windowlogging;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public record WindowInABlockTileEntityRenderer(
	BlockEntityRendererProvider.Context context) implements BlockEntityRenderer<WindowInABlockTileEntity> {

	@Override
	public void render(WindowInABlockTileEntity tileEntityIn, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
		BlockEntity partialTE = tileEntityIn.getPartialBlockTileEntityIfPresent();
		if (partialTE == null)
			return;
		BlockEntityRenderer<BlockEntity> renderer = context.getBlockEntityRenderDispatcher().getRenderer(partialTE);
		if (renderer == null)
			return;
		try {
			renderer.render(partialTE, partialTicks, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
		} catch (Exception ignored) {
			// ignored
		}
	}
}
