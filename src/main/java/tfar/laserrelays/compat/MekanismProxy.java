package tfar.laserrelays.compat;

import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.ModList;
import tfar.laserrelays.ExampleMod;
import tfar.laserrelays.NodeBlock;
import tfar.laserrelays.NodeBlockEntity;
import tfar.laserrelays.NodeType;

public class MekanismProxy {

    public static void tickGas(NodeBlockEntity nodeBlockEntity,BlockState state) {
        Direction dir1 = state.get(NodeBlock.FACING);
        TileEntity inputTileEntity = nodeBlockEntity.getWorld().getTileEntity(nodeBlockEntity.getPos().offset(dir1.getOpposite()));
        if (inputTileEntity != null) {
            inputTileEntity.getCapability(ExampleMod.GAS_HANDLER_CAPABILITY, dir1).ifPresent(iGasHandler -> {
                GasStack gasStack = iGasHandler.extractChemical(Integer.MAX_VALUE, Action.SIMULATE);
                if (!gasStack.isEmpty()) {
                    for (BlockPos pos1 : nodeBlockEntity.connections.get(NodeType.GAS)) {
                        BlockState otherNodeState = nodeBlockEntity.getWorld().getBlockState(pos1);
                        if (otherNodeState.getBlock() instanceof NodeBlock &&
                                !otherNodeState.get(NodeBlock.GAS_INPUT)) {
                            Direction dir2 = otherNodeState.get(NodeBlock.FACING);
                            TileEntity outputTileEntity = nodeBlockEntity.getWorld().getTileEntity(pos1.offset(dir2.getOpposite()));
                            if (outputTileEntity != null) {
                                IGasHandler output = outputTileEntity.getCapability(ExampleMod.GAS_HANDLER_CAPABILITY, dir2).orElse(null);
                                if (output != null) {
                                    GasStack accepted = output.insertChemical(gasStack, Action.EXECUTE);
                                    //accepted all
                                    if (accepted.isEmpty()) {
                                        iGasHandler.extractChemical(gasStack.getAmount(), Action.EXECUTE);
                                        break;
                                    } else if (gasStack.getAmount() < accepted.getAmount()) {
                                        long amountAccepted = gasStack.getAmount() - accepted.getAmount();
                                        iGasHandler.extractChemical(amountAccepted, Action.EXECUTE);
                                        gasStack.shrink(amountAccepted);
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}
