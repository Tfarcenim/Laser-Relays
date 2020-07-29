package tfar.laserrelays.item;

import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import tfar.laserrelays.NodeBlock;
import tfar.laserrelays.NodeType;

import javax.annotation.Nullable;
import java.util.List;

public class ColorFilterItem extends Item {
	public ColorFilterItem(Properties properties) {
		super(properties);
	}

	public static void changeColor(ItemStack bag, boolean right) {
		//if (bag.getItem() instanceof ColorFilterItem) {
	//		NodeType nodeType =
//		}
	}

	@Override
	public ActionResultType onItemUse(ItemUseContext context) {
		World world = context.getWorld();
		if (!world.isRemote) {
			BlockPos pos = context.getPos();
			BlockState state = world.getBlockState(pos);
			if (state.getBlock() instanceof NodeBlock) {
				NodeType nodeType = NodeBlock.getNodeFromTrace(context.getHitVec(), state.get(NodeBlock.FACING));
				context.getItem().getOrCreateTag().putString("node_type",nodeType.toString());
				return ActionResultType.SUCCESS;
			}
		}
		return ActionResultType.SUCCESS;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
	}
}
