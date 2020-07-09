package tfar.laserrelays;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import javax.annotation.Nullable;

public enum NodeType {
	ITEM(0x00ff00), FLUID(0x0000ff), ENERGY(0xff0000), GAS(0xffff00);
	public final int color;

	NodeType(int color) {
		this.color = color;
	}

	@Nullable
	public static NodeType getNodeFromStack(ItemStack stack) {
		CompoundNBT nbt = stack.getTag();
		return stack.getItem() instanceof ColorFilterItem && nbt != null && nbt.contains("node_type") ? NodeType.valueOf(stack.getTag().getString("node_type")) : null;
	}
}
