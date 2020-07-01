package tfar.universalwires;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class NodeBlockItem extends BlockItem {

	public final NodeType nodeType;

	public NodeBlockItem(Block blockIn, Properties builder, NodeType nodeType) {
		super(blockIn, builder);
		this.nodeType = nodeType;
	}

	@Override
	public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
		if (this.isInGroup(group)) {
			items.add(new ItemStack(this));
		}
	}
}
