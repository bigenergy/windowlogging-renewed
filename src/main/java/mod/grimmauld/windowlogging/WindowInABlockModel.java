package mod.grimmauld.windowlogging;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

import static mod.grimmauld.windowlogging.WindowInABlockTileEntity.WINDOWLOGGED_TE;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class WindowInABlockModel extends BakedModelWrapper<BakedModel> {
	private static final BlockRenderDispatcher DISPATCHER = Minecraft.getInstance().getBlockRenderer();

	public WindowInABlockModel(BakedModel original) {
		super(original);
	}

	private static void fightZfighting(BakedQuad q) {
		int[] data = q.getVertices();
		Vec3i vec = q.getDirection().getNormal();
		int dirX = vec.getX();
		int dirY = vec.getY();
		int dirZ = vec.getZ();
		for (int i = 0; i < 4; ++i) {
			int j = data.length / 4 * i;
			float x = Float.intBitsToFloat(data[j]);
			float y = Float.intBitsToFloat(data[j + 1]);
			float z = Float.intBitsToFloat(data[j + 2]);
			double offset = q.getDirection().getAxis().choose(x, y, z);

			if (offset < 1 / 1024d || offset > 1023 / 1024d) {
				data[j] = Float.floatToIntBits(x - 1 / 512f * dirX);
				data[j + 1] = Float.floatToIntBits(y - 1 / 512f * dirY);
				data[j + 2] = Float.floatToIntBits(z - 1 / 512f * dirZ);
			}
		}
	}

	private static boolean hasSolidSide(BlockState state, BlockGetter worldIn, BlockPos pos, Direction side) {
		return !state.is(BlockTags.LEAVES) && Block.isFaceFull(state.getBlockSupportShape(worldIn, pos), side);
	}

	@Override
	@Nonnull
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
		List<BakedQuad> quads = new ArrayList<>();

		WindowInABlockTileEntity windowInABlockTileEntity = data.get(WINDOWLOGGED_TE);
		if (windowInABlockTileEntity == null)
			return quads;
		BlockState partialState = windowInABlockTileEntity.getPartialBlock();
		BlockState windowState = windowInABlockTileEntity.getWindowBlock();
		BlockPos position = windowInABlockTileEntity.getBlockPos();
		BlockEntity partialTE = windowInABlockTileEntity.getPartialBlockTileEntityIfPresent();
		Level world = windowInABlockTileEntity.getLevel();
		if (world == null)
			return quads;

		if (true) { // ItemBlockRenderTypes.canRenderInLayer(partialState, renderType) && partialState.getRenderShape() == RenderShape.MODEL) {
			BakedModel partialModel = DISPATCHER.getBlockModel(partialState);
			quads.addAll(partialModel.getQuads(partialState, side, rand, partialModel.getModelData(world, position, partialState,
					partialTE == null ? ModelData.EMPTY : partialTE.getModelData()), renderType));
		}
		if (true) { // ItemBlockRenderTypes.canRenderInLayer(windowState, renderType)) {
			DISPATCHER.getBlockModel(windowState).getQuads(windowState, side, rand, DISPATCHER.getBlockModel(windowState).getModelData(world, position, windowState, ModelData.EMPTY), renderType)
					.forEach(bakedQuad -> {
						if (!hasSolidSide(partialState, world, position, bakedQuad.getDirection())) {
							fightZfighting(bakedQuad);
							quads.add(bakedQuad);
						}
					});
		}
		return quads;
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		WindowInABlockTileEntity windowInABlockTileEntity = data.get(WINDOWLOGGED_TE);
		if (windowInABlockTileEntity == null)
			return super.getParticleIcon(data);

		BlockEntity partialTE = windowInABlockTileEntity.getPartialBlockTileEntityIfPresent();
		return DISPATCHER.getBlockModel(windowInABlockTileEntity.getPartialBlock()).getParticleIcon(partialTE == null ? data : partialTE.getModelData());
	}


	@Override
	public boolean useAmbientOcclusion(BlockState state, RenderType renderType) {
		return renderType == RenderType.solid();
	}
}
