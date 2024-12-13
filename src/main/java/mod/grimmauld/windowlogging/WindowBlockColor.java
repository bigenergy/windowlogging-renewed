package mod.grimmauld.windowlogging;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class WindowBlockColor implements BlockColor {
	public static final BlockColors BLOCK_COLORS = Minecraft.getInstance().getBlockColors();

	public static void registerFor(WindowInABlockBlock block) {
		BLOCK_COLORS.register(new WindowBlockColor(), block);
	}

	@Override
	public int getColor(BlockState state, @Nullable BlockAndTintGetter world, @Nullable BlockPos pos, int color) {
		if (!(state.getBlock() instanceof WindowInABlockBlock windowInABlockBlock) || world == null || pos == null)
			return -1;
		return BLOCK_COLORS.getColor(windowInABlockBlock.getSurroundingBlockState(world, pos), world, pos, color);
	}
}
