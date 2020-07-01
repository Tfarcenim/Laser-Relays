package tfar.universalwires;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class NBTUtil {

	public static final String CONNECTIONS = "connections";

	public static CompoundNBT writeBlockPos(CompoundNBT nbt, @Nullable BlockPos pos) {
		if (pos == null)return nbt;
		CompoundNBT tag = new CompoundNBT();
		tag.putInt("x",pos.getX());
		tag.putInt("y",pos.getY());
		tag.putInt("z",pos.getZ());
		nbt.put("connected",tag);
		return nbt;
	}

	public static ListNBT writeBlockPosList(ListNBT nbt, Set<BlockPos> posSet) {
		if (posSet == null)posSet = new HashSet<>();
		if (posSet.isEmpty())return nbt;
		posSet.forEach(pos -> {
			CompoundNBT nbt1 = new CompoundNBT();
			nbt1.putInt("x",pos.getX());
			nbt1.putInt("y",pos.getY());
			nbt1.putInt("z",pos.getZ());
			nbt.add(nbt1);
		});
		return nbt;
	}

	public static BlockPos readBlockPos(CompoundNBT nbt) {
		if (!nbt.contains("connected"))return null;
		CompoundNBT tag = nbt.getCompound("connected");
		return new BlockPos(tag.getInt("x"),tag.getInt("y"),tag.getInt("z"));
	}

	public static void readBlockPosList(ListNBT nbt, Set<BlockPos> posSet) {
		for (INBT inbt : nbt) {
			posSet.add(new BlockPos(((CompoundNBT)inbt).getInt("x"),
							((CompoundNBT)inbt).getInt("y"),
							((CompoundNBT)inbt).getInt("z")));
		}
	}
}
