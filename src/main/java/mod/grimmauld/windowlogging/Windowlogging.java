package mod.grimmauld.windowlogging;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static mod.grimmauld.windowlogging.Windowlogging.MODID;

@Mod(Windowlogging.MODID)
public class Windowlogging {
	public static final TagKey<Block> WINDOWABLE = BlockTags.create(new ResourceLocation(Windowlogging.MODID, "windowable"));
	public static final TagKey<Block> WINDOW = BlockTags.create(new ResourceLocation(Windowlogging.MODID, "window"));

	public static final String MODID = "windowlogging";
	public static final Logger LOGGER = LogManager.getLogger(Windowlogging.MODID);
	public static final ResourceLocation WindowableBlockTagLocation = new ResourceLocation(Windowlogging.MODID, "windowable");
	public static final ResourceLocation WindowBlockTagLocation = new ResourceLocation(Windowlogging.MODID, "window");

	public Windowlogging() {
		MinecraftForge.EVENT_BUS.register(new EventListener());
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			//FMLJavaModLoadingContext.get().getModEventBus().addListener(EventListener::clientInit);
			//FMLJavaModLoadingContext.get().getModEventBus().addListener(EventListener::onModelBake);
		});
	}
}
