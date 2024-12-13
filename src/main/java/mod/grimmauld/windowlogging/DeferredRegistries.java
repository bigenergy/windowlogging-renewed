package mod.grimmauld.windowlogging;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Windowlogging.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DeferredRegistries {
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Windowlogging.MODID);
	private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Windowlogging.MODID);
	public static final RegistryObject<WindowInABlockBlock> WINDOW_IN_A_BLOCK = registerBlock("window_in_a_block", WindowInABlockBlock::new);

	static {
		BLOCK_ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
		BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
	}

	private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> registerTE(String id, Supplier<BlockEntityType.Builder<T>> type) {
		return BLOCK_ENTITY_TYPES.register(id, () -> type.get().build(null));
	}	public static final RegistryObject<BlockEntityType<WindowInABlockTileEntity>> WINDOW_IN_A_BLOCK_TILE_ENTITY = registerTE(
		"window_in_a_block", () -> BlockEntityType.Builder.of(WindowInABlockTileEntity::new, Blocks.CAULDRON, WINDOW_IN_A_BLOCK.get()));

	private static <T extends Block> RegistryObject<T> registerBlock(String id, Supplier<T> blockSupplier) {
		return BLOCKS.register(id, blockSupplier);
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void clientInit(FMLClientSetupEvent event) {
		ItemBlockRenderTypes.setRenderLayer(WINDOW_IN_A_BLOCK.get(), renderType -> true);
		BlockEntityRenderers.register(WINDOW_IN_A_BLOCK_TILE_ENTITY.get(), WindowInABlockTileEntityRenderer::new);
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void registerColorProviders(RegisterParticleProvidersEvent event) {
		WindowBlockColor.registerFor(DeferredRegistries.WINDOW_IN_A_BLOCK.get());
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onModelBake(ModelEvent.ModifyBakingResult event) {
		Map<ResourceLocation, BakedModel> modelRegistry = event.getModels();
		DeferredRegistries.WINDOW_IN_A_BLOCK.get().getStateDefinition()
			.getPossibleStates()
			.stream()
			.map(state -> Optional.ofNullable(DeferredRegistries.WINDOW_IN_A_BLOCK.getId())
				.map(rl -> new ModelResourceLocation(rl, BlockModelShaper.statePropertiesToString(state.getValues()))))
			.flatMap(Optional::stream)
			.forEach(location -> modelRegistry.put(location, new WindowInABlockModel(modelRegistry.get(location))));
	}
}
