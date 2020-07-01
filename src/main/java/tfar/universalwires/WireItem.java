package tfar.universalwires;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class WireItem extends Item {
	public WireItem(Properties properties) {
		super(properties);
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
		if (stack.hasTag()) {
			BlockPos pos = NBTUtil.readBlockPos(stack.getTag());
			if (pos != null)
			tooltip.add(new StringTextComponent(pos.toString()));
			String nodeType = stack.getTag().getString("node_type");
			tooltip.add(new StringTextComponent(nodeType));
		}
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
		return super.onItemRightClick(worldIn, playerIn, handIn);
	}
}
