package tfar.universalwires;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ExampleMod.MODID)
public class ExampleMod {
    // Directly reference a log4j logger.

    public static final String MODID = "universalwires";

    private static final Logger LOGGER = LogManager.getLogger();

    public static Block node;

    public static Item wire;

    public static Item item_node;
    public static Item fluid_node;
    public static Item energy_node;
    public static Item gas_node;


    public static TileEntityType<?> BLOCK_ENTITY;

    public ExampleMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        // Register the setup method for modloading
        bus.addListener(this::setup);
        // Register the doClientStuff method for modloading
        bus.addListener(this::doClientStuff);
        bus.addGenericListener(Block.class,this::blocks);
        bus.addGenericListener(Item.class,this::items);
        bus.addGenericListener(TileEntityType.class,this::blockentities);
        EVENT_BUS.addListener(NodeBlock::replace);
    }

    private void blocks(final RegistryEvent.Register<Block> event) {
        node = register(new NodeBlock(Block.Properties.from(Blocks.DAYLIGHT_DETECTOR).doesNotBlockMovement()),"node",event.getRegistry());
    }


    private void items(final RegistryEvent.Register<Item> event) {

        wire = register(new WireItem(new Item.Properties().group(ItemGroup.REDSTONE)),"wire",event.getRegistry());
        register(new NodeBlockItem(node,new Item.Properties().group(ItemGroup.REDSTONE),NodeType.ITEM),
                "item_node", event.getRegistry());
        register(new NodeBlockItem(node,new Item.Properties().group(ItemGroup.REDSTONE),NodeType.FLUID),
                "fluid_node", event.getRegistry());
        register(new NodeBlockItem(node,new Item.Properties().group(ItemGroup.REDSTONE),NodeType.ENERGY),
                "energy_node", event.getRegistry());
        register(new NodeBlockItem(node,new Item.Properties().group(ItemGroup.REDSTONE),NodeType.GAS),
                "gas_node", event.getRegistry());
    }

    private void blockentities(final RegistryEvent.Register<TileEntityType<?>> event) {
        BLOCK_ENTITY = register(
                TileEntityType.Builder.create(NodeBlockEntity::new, node).build(null),
                "node",event.getRegistry());
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        EVENT_BUS.addListener(Client::render);
        RenderTypeLookup.setRenderLayer(node, RenderType.translucent());
    }

    private static <T extends IForgeRegistryEntry<T>> T register(T obj, String name, IForgeRegistry<T> registry) {
        registry.register(obj.setRegistryName(new ResourceLocation(MODID, name)));
        return obj;
    }
}
