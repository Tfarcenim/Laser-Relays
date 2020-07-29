package tfar.laserrelays;

import mekanism.api.chemical.gas.IGasHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import tfar.laserrelays.item.ColorFilterItem;
import tfar.laserrelays.item.NodeBlockItem;
import tfar.laserrelays.item.WireItem;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ExampleMod.MODID)
public class ExampleMod {
    // Directly reference a log4j logger.

    @CapabilityInject(IGasHandler.class)
    public static Capability<IGasHandler> GAS_HANDLER_CAPABILITY = null;

    public static final String MODID = "laserrelays";

    public static final ITag.INamedTag<Item> HIGHLIGHT = ItemTags.makeWrapperTag(MODID+":highlight");

    public static Block node;

    public static Item wire;
    public static Item color_filter;
    public static Item wire_cutters;
    public static Item wrench;

    public static Item item_node;
    public static Item fluid_node;
    public static Item energy_node;
    public static Item gas_node;


    public static TileEntityType<?> BLOCK_ENTITY;
    public static ContainerType<NodeContainer> MENU;

    public ExampleMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        // Register the setup method for modloading
        bus.addListener(this::setup);
        // Register the doClientStuff method for modloading
        bus.addListener(this::doClientStuff);
        bus.addGenericListener(Block.class,this::blocks);
        bus.addGenericListener(Item.class,this::items);
        bus.addGenericListener(TileEntityType.class,this::blockentities);
        bus.addGenericListener(ContainerType.class,this::menus);
        EVENT_BUS.addListener(NodeBlock::replace);
    }



    private void blocks(final RegistryEvent.Register<Block> event) {
        node = register(new NodeBlock(Block.Properties.from(Blocks.DAYLIGHT_DETECTOR).doesNotBlockMovement()),"node",event.getRegistry());
    }


    private void items(final RegistryEvent.Register<Item> event) {
        wrench = register(new Item(new Item.Properties().group(ItemGroup.REDSTONE)),"wrench",event.getRegistry());
        wire = register(new WireItem(new Item.Properties().group(ItemGroup.REDSTONE)),"wire",event.getRegistry());
        wire_cutters = register(new Item(new Item.Properties().group(ItemGroup.REDSTONE)),"wire_cutters",event.getRegistry());
        color_filter = register(new ColorFilterItem(new Item.Properties().group(ItemGroup.REDSTONE)),"color_filter",event.getRegistry());
        item_node = register(new NodeBlockItem(node,new Item.Properties().group(ItemGroup.REDSTONE),NodeType.ITEM),
                "item_node", event.getRegistry());
        fluid_node = register(new NodeBlockItem(node,new Item.Properties().group(ItemGroup.REDSTONE),NodeType.FLUID),
                "fluid_node", event.getRegistry());
        energy_node = register(new NodeBlockItem(node,new Item.Properties().group(ItemGroup.REDSTONE),NodeType.ENERGY),
                "energy_node", event.getRegistry());
        gas_node = register(new NodeBlockItem(node,new Item.Properties().group(ItemGroup.REDSTONE),NodeType.GAS),
                "gas_node", event.getRegistry());
    }

    private void blockentities(final RegistryEvent.Register<TileEntityType<?>> event) {
        BLOCK_ENTITY = register(
                TileEntityType.Builder.create(NodeBlockEntity::new, node).build(null),
                "node",event.getRegistry());
    }

    private void menus(final RegistryEvent.Register<ContainerType<?>> event) {
        MENU = (ContainerType<NodeContainer>) register(
                IForgeContainerType.create(NodeContainer::new),
                "node",event.getRegistry());
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        ScreenManager.registerFactory(MENU,NodeScreen::new);
        EVENT_BUS.addListener(Client::render);
        EVENT_BUS.addListener(Client::scroll);
        RenderTypeLookup.setRenderLayer(node, RenderType.getTranslucent());
        Minecraft.getInstance().getItemColors().register(ExampleMod::getNodeColor, wire,color_filter);
    }

    public static int getNodeColor(ItemStack stack,int i) {
        CompoundNBT nbt = stack.getTag();
        if (nbt != null && nbt.contains("node_type")) {
            NodeType nodeType = NodeType.valueOf(stack.getTag().getString("node_type"));
            return nodeType.color;
        }
        return 0xffffff;
    }

    private static <T extends IForgeRegistryEntry<T>> T register(T obj, String name, IForgeRegistry<T> registry) {
        registry.register(obj.setRegistryName(new ResourceLocation(MODID, name)));
        return obj;
    }
}
