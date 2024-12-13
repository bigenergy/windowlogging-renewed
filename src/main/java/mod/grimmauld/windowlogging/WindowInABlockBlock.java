package mod.grimmauld.windowlogging;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class WindowInABlockBlock extends IronBarsBlock implements EntityBlock {

	public WindowInABlockBlock() {
		super(Properties.copy(Blocks.STONE).noOcclusion());
	}

	private static void addBlockHitEffects(ParticleEngine manager, BlockPos pos, BlockHitResult target, BlockState blockstate, ClientLevel world) {
		VoxelShape shape = blockstate.getShape(world, pos);
		if (shape.isEmpty())
			return;
		Direction side = target.getDirection();
		int i = pos.getX();
		int j = pos.getY();
		int k = pos.getZ();
		AABB axisalignedbb = shape.bounds();
		double d0 = i + manager.random.nextDouble() * (axisalignedbb.maxX - axisalignedbb.minX - 0.2F) + 0.1F + axisalignedbb.minX;
		double d1 = j + manager.random.nextDouble() * (axisalignedbb.maxY - axisalignedbb.minY - 0.2F) + 0.1F + axisalignedbb.minY;
		double d2 = k + manager.random.nextDouble() * (axisalignedbb.maxZ - axisalignedbb.minZ - 0.2F) + 0.1F + axisalignedbb.minZ;
		if (side == Direction.DOWN) {
			d1 = j + axisalignedbb.minY - 0.1F;
		}

		if (side == Direction.UP) {
			d1 = j + axisalignedbb.maxY + 0.1F;
		}

		if (side == Direction.NORTH) {
			d2 = k + axisalignedbb.minZ - 0.1F;
		}

		if (side == Direction.SOUTH) {
			d2 = k + axisalignedbb.maxZ + 0.1F;
		}

		if (side == Direction.WEST) {
			d0 = i + axisalignedbb.minX - 0.1F;
		}

		if (side == Direction.EAST) {
			d0 = i + axisalignedbb.maxX + 0.1F;
		}

		manager.add((new TerrainParticle(world, d0, d1, d2, 0.0D, 0.0D, 0.0D, blockstate, pos)).setPower(0.2F).scale(0.6F));
	}

	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player,
									   boolean willHarvest, FluidState fluid) {

		Vec3 start = player.getEyePosition(1);
		AttributeInstance reachDistanceAttribute = player.getAttribute(ForgeMod.BLOCK_REACH.get());
		if (reachDistanceAttribute == null)
			return super.onDestroyedByPlayer(state, world, pos, null, willHarvest, fluid);
		Vec3 end = start.add(player.getLookAngle().scale(reachDistanceAttribute.getValue()));
		BlockHitResult target =
			world.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		WindowInABlockTileEntity tileEntity = getTileEntity(world, pos);
		if (tileEntity == null)
			return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
		BlockState windowBlock = tileEntity.getWindowBlock();
		CompoundTag partialBlockTileData = tileEntity.getPartialBlockTileData();
		for (AABB bb : windowBlock.getShape(world, pos).toAabbs()) {
			if (bb.inflate(.1d).contains(target.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()))) {
				windowBlock.getBlock().playerWillDestroy(world, pos, windowBlock, player);
				if (!player.isCreative())
					Block.dropResources(windowBlock, world, pos, null, player, player.getMainHandItem());
				BlockState partialBlock = tileEntity.getPartialBlock();
				world.setBlockAndUpdate(pos, partialBlock);
				for (Direction d : Direction.values()) {
					BlockPos offset = pos.relative(d);
					BlockState otherState = world.getBlockState(offset);
					partialBlock = partialBlock.updateShape(d, otherState, world, pos, offset);
					world.sendBlockUpdated(offset, otherState, otherState, 2);
				}
				if (partialBlock != world.getBlockState(pos))
					world.setBlockAndUpdate(pos, partialBlock);
				if (world.getBlockState(pos) instanceof EntityBlock) {
					BlockEntity te = world.getBlockEntity(pos);
					if (te != null) {
						te.deserializeNBT(partialBlockTileData);
						te.setChanged();
					}
				}
				return false;
			}
		}

		return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
	}

	@Override
	public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
		return false;
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return getSurroundingBlockState(reader, pos).propagatesSkylightDown(reader, pos);
	}

	@Override
	public boolean collisionExtendsVertically(BlockState state, BlockGetter world, BlockPos pos,
											  Entity collidingEntity) {
		return getSurroundingBlockState(world, pos).collisionExtendsVertically(world, pos, collidingEntity);
	}

	@Override
	public float getDestroyProgress(BlockState state, Player player, BlockGetter worldIn, BlockPos pos) {
		return getSurroundingBlockState(worldIn, pos).getDestroyProgress(player, worldIn, pos);
	}

	@Override
	public float getExplosionResistance(BlockState state, BlockGetter world, BlockPos pos, Explosion explosion) {
		return getSurroundingBlockState(world, pos).getExplosionResistance(world, pos, explosion);
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos,
									   Player player) {
		BlockState window = getWindowBlockState(world, pos);
		for (AABB bb : window.getShape(world, pos).toAabbs()) {
			if (bb.inflate(.1d).contains(target.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ())))
				return window.getCloneItemStack(target, world, pos, player);
		}
		BlockState surrounding = getSurroundingBlockState(world, pos);
		return surrounding.getCloneItemStack(target, world, pos, player);
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		BlockEntity tileentity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (!(tileentity instanceof WindowInABlockTileEntity te))
			return Collections.emptyList();

		BlockEntity partialTE = te.getPartialBlockTileEntityIfPresent();
		if (partialTE != null)
			builder.withParameter(LootContextParams.BLOCK_ENTITY, partialTE);
		List<ItemStack> drops = te.getPartialBlock().getDrops(builder);
		builder.withParameter(LootContextParams.BLOCK_ENTITY, tileentity);
		drops.addAll(te.getWindowBlock().getDrops(builder));
		return drops;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		VoxelShape shape1 = getSurroundingBlockState(worldIn, pos).getShape(worldIn, pos, context);
		VoxelShape shape2 = getWindowBlockState(worldIn, pos).getShape(worldIn, pos, context);
		return Shapes.or(shape1, shape2);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos,
										CollisionContext context) {
		return getShape(state, worldIn, pos, context);
	}

	@Override
	public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn,
								  BlockPos currentPos, BlockPos facingPos) {
		WindowInABlockTileEntity te = getTileEntity(worldIn, currentPos);
		if (te == null)
			return stateIn;
		te.setWindowBlock(
			te.getWindowBlock().updateShape(facing, facingState, worldIn, currentPos, facingPos));
		BlockState blockState =
			te.getPartialBlock().updateShape(facing, facingState, worldIn, currentPos, facingPos);
		if (blockState.getBlock() instanceof CrossCollisionBlock) {
			for (BooleanProperty side : Arrays.asList(CrossCollisionBlock.EAST, CrossCollisionBlock.NORTH, CrossCollisionBlock.SOUTH,
				CrossCollisionBlock.WEST))
				blockState = blockState.setValue(side, false);
			te.setPartialBlock(blockState);
		}
		te.requestModelDataUpdate();

		return stateIn;
	}

	public BlockState getSurroundingBlockState(BlockGetter reader, BlockPos pos) {
		WindowInABlockTileEntity te = getTileEntity(reader, pos);
		if (te != null)
			return te.getPartialBlock();
		return Blocks.AIR.defaultBlockState();
	}

	public BlockState getWindowBlockState(BlockGetter reader, BlockPos pos) {
		WindowInABlockTileEntity te = getTileEntity(reader, pos);
		if (te != null)
			return te.getWindowBlock();
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
		return false;
	}

	@Nullable
	private WindowInABlockTileEntity getTileEntity(BlockGetter world, BlockPos pos) {
		BlockEntity te = world.getBlockEntity(pos);
		if (te instanceof WindowInABlockTileEntity wte)
			return wte;
		return null;
	}

	@Override
	public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, @Nullable Entity entity) {
		WindowInABlockTileEntity te = getTileEntity(world, pos);
		return super.getSoundType(te != null ? te.getPartialBlock() : state, world, pos, entity);
	}

	@Override
	public boolean addLandingEffects(BlockState state1, ServerLevel world, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
		WindowInABlockTileEntity te = getTileEntity(world, pos);
		if (te != null) {
			te.getWindowBlock().addLandingEffects(world, pos, state2, entity, numberOfParticles / 2);
			return te.getPartialBlock().addLandingEffects(world, pos, state2, entity, numberOfParticles / 2);
		}
		return false;
	}

	@Override
	public boolean addRunningEffects(BlockState state, Level world, BlockPos pos, Entity entity) {
		WindowInABlockTileEntity te = getTileEntity(world, pos);
		if (te != null) {
			te.getWindowBlock().addRunningEffects(world, pos, entity);
			return te.getPartialBlock().addRunningEffects(world, pos, entity);
		}
		return false;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
		consumer.accept(new IClientBlockExtensions() {
			private final ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;

			@Override
			public boolean addHitEffects(BlockState state, Level world, HitResult target, ParticleEngine manager) {
				if (target.getType() != HitResult.Type.BLOCK || !(target instanceof BlockHitResult) || !(world instanceof ClientLevel))
					return false;
				BlockPos pos = ((BlockHitResult) target).getBlockPos();
				WindowInABlockTileEntity te = getTileEntity(world, pos);
				if (te != null) {
					IClientBlockExtensions.of(te.getWindowBlock()).addHitEffects(state, world, target, particleEngine);
					addBlockHitEffects(manager, pos, (BlockHitResult) target, te.getWindowBlock(), (ClientLevel) world);
					return IClientBlockExtensions.of(te.getPartialBlock()).addHitEffects(te.getPartialBlock(), world, target, particleEngine);
				}
				return false;
			}

			@Override
			public boolean addDestroyEffects(BlockState state, Level world, BlockPos pos, ParticleEngine manager) {
				WindowInABlockTileEntity te = getTileEntity(world, pos);
				if (te != null) {

					IClientBlockExtensions.of(te.getWindowBlock()).addDestroyEffects(te.getWindowBlock(), world, pos, particleEngine);
					manager.destroy(pos, te.getWindowBlock());
					return IClientBlockExtensions.of(te.getPartialBlock()).addDestroyEffects(te.getPartialBlock(), world, pos, manager);
				}
				return false;
			}
		});
	}

	@Override
	public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
		WindowInABlockTileEntity te = getTileEntity(world, pos);
		if (te != null) {
			BlockState partialState = te.getPartialBlock();
			partialState.getBlock().getLightEmission(partialState, world, pos);
		}
		return 0;
	}

	@Nullable
	@Override
	public float[] getBeaconColorMultiplier(BlockState state, LevelReader world, BlockPos pos, BlockPos beaconPos) {
		WindowInABlockTileEntity te = getTileEntity(world, pos);
		if (te != null) {
			BlockState windowState = te.getWindowBlock();
			return windowState.getBlock().getBeaconColorMultiplier(windowState, world, pos, beaconPos);
		}
		return super.getBeaconColorMultiplier(state, world, pos, beaconPos);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState blockState) {
		return new WindowInABlockTileEntity(pos, blockState);
	}
}
