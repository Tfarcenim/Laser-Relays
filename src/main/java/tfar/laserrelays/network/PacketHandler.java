package tfar.laserrelays.network;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import tfar.laserrelays.ExampleMod;

public class PacketHandler {
  public static SimpleChannel INSTANCE;

  public static void registerMessages(String channelName) {
    int id = 0;

    INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(ExampleMod.MODID, channelName), () -> "1.0", s -> true, s -> true);

    INSTANCE.registerMessage(id++, C2SMessageScrollSlot.class,
            C2SMessageScrollSlot::encode,
            C2SMessageScrollSlot::new,
            C2SMessageScrollSlot::handle);
  }
}
