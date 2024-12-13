package mod.grimmauld.windowlogging;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WindowInABlockTileEntity extends BlockEntity {
	public static final ModelProperty<WindowInABlockTileEntity> WINDOWLOGGED_TE = new ModelProperty<>();
	private final ModelData modelData = ModelData.builder().with(WINDOWLOGGED_TE, this).build();
	private BlockState partialBlock = Blocks.AIR.defaultBlockState();
	private BlockState windowBlock = Blocks.AIR.defaultBlockState();
	private CompoundTag partialBlockTileData = new CompoundTag();
	private BlockEntity partialBlockTileEntity = null;

	public WindowInABlockTileEntity(BlockPos pos, BlockState blockState) {
		super(DeferredRegistries.WINDOW_IN_A_BLOCK_TILE_ENTITY.get(), pos, blockState);
		setPartialBlockTileData(new CompoundTag());
	}

	public CompoundTag getPartialBlockTileData() {
		return partialBlockTileData;
	}

	public void setPartialBlockTileData(CompoundTag partialBlockTileData) {
		this.partialBlockTileData = partialBlockTileData;
	}

	@Override
	public void load(CompoundTag compound) {
		super.load(compound);
		partialBlock = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), compound.getCompound("PartialBlock"));
		windowBlock = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), compound.getCompound("WindowBlock"));
		setPartialBlockTileData(compound.getCompound("PartialData"));
		requestModelDataUpdate();
	}

	@Override
	public void saveAdditional(CompoundTag compound) {
		compound.put("PartialBlock", NbtUtils.writeBlockState(getPartialBlock()));
		compound.put("WindowBlock", NbtUtils.writeBlockState(getWindowBlock()));
		compound.put("PartialData", partialBlockTileData);
	}

	public void updateWindowConnections() {
		if (level == null)
			return;
		for (Direction side : Direction.values()) {
			BlockPos offsetPos = worldPosition.relative(side);
			windowBlock = getWindowBlock().updateShape(side, level.getBlockState(offsetPos), level, worldPosition,
				offsetPos);
		}
		level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2 | 16);
		setChanged();
	}

	@Override
	@Nonnull
	public ModelData getModelData() {
		return modelData;
	}

	public BlockState getPartialBlock() {
		return partialBlock;
	}

	public void setPartialBlock(BlockState partialBlock) {
		this.partialBlock = partialBlock;
	}

	public BlockState getWindowBlock() {
		return windowBlock;
	}

	public void setWindowBlock(BlockState windowBlock) {
		this.windowBlock = windowBlock;
	}

	@Override
	public CompoundTag getUpdateTag() {
		return saveWithoutMetadata();
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Nullable
	public BlockEntity getPartialBlockTileEntityIfPresent() {
		if (!(getPartialBlock() instanceof EntityBlock entityBlock) || level == null)
			return null;
		if (partialBlockTileEntity == null) {
			try {
				partialBlockTileEntity = entityBlock.newBlockEntity(worldPosition, partialBlock);
				if (partialBlockTileEntity != null) {
					partialBlockTileEntity.blockState = getPartialBlock();
					partialBlockTileEntity.deserializeNBT(partialBlockTileData);
					partialBlockTileEntity.setLevel(level);
				}
			} catch (Exception e) {
				partialBlockTileEntity = null;
			}
		}
		return partialBlockTileEntity;
	}
}
