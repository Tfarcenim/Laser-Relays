package tfar.laserrelays.network;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import tfar.laserrelays.ColorFilterItem;

import java.util.function.Supplier;


public class C2SMessageScrollSlot {

  boolean right;

  public C2SMessageScrollSlot() {
  }

  public C2SMessageScrollSlot(boolean right) {
    this.right = right;
  }

  //decode
  public C2SMessageScrollSlot(PacketBuffer buf) {
    this.right = buf.readBoolean();
  }

  public void encode(PacketBuffer buf) {
    buf.writeBoolean(right);
  }

  public void handle(Supplier<NetworkEvent.Context> ctx) {
    PlayerEntity player = ctx.get().getSender();
    ctx.get().enqueueWork(() -> {
      ItemStack bag = player.getHeldItemMainhand();
      ColorFilterItem.changeColor(bag, right);
    });
    ctx.get().setPacketHandled(true);
  }
}

