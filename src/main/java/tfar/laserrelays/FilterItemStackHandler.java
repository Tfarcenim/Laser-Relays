package tfar.laserrelays;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class FilterItemStackHandler extends ItemStackHandler {

	public boolean whitelist = true;

	public FilterItemStackHandler(int slots) {
		super(slots);
	}

	@Override
	public int getSlotLimit(int slot) {
		return 1;
	}

	public boolean isEmpty(){
		return stacks.stream().allMatch(ItemStack::isEmpty);
	}

	public boolean test(ItemStack stack) {
		if (isEmpty())return true;
		return stacks.stream().anyMatch(stack1 -> ItemStack.areItemsEqual(stack,stack1));
	}
}
