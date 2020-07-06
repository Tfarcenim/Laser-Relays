package tfar.laserrelays;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NodeBlockEntity extends TileEntity implements ITickableTileEntity {

	protected Map<NodeType, Set<BlockPos>> connections = new EnumMap<>(NodeType.class);

	public int max_slots = 27;

	public int index = 0;

	public NodeBlockEntity() {
		super(ExampleMod.BLOCK_ENTITY);
		for (NodeType nodeType : NodeType.values()) {
			connections.put(nodeType, new HashSet<>());
		}
	}

	public void connect(BlockPos other, NodeType node) {
		if (!connections.get(node).contains(other)) {
			connections.get(node).add(other);
			markDirty();
		}
	}

	public void disconnect(BlockPos other,NodeType node) {
		if (connections.get(node).contains(other)) {
			connections.get(node).remove(other);
			markDirty();
		}
	}

	public void disconnectNode(NodeType node) {
		if (!connections.get(node).isEmpty()) {
			connections.get(node).clear();
			markDirty();
		}
	}

	@Override
	public void func_230337_a_(BlockState state,CompoundNBT compound) {
		super.func_230337_a_(state,compound);
		CompoundNBT nbt = compound.getCompound(NBTUtil.CONNECTIONS);
		for (NodeType nodeType : NodeType.values()) {
			connections.get(nodeType).clear();
			ListNBT listNBT = nbt.getList(nodeType.toString(), Constants.NBT.TAG_COMPOUND);
			Set<BlockPos> blockPosSet = new HashSet<>();
			NBTUtil.readBlockPosList(listNBT, blockPosSet);
			connections.get(nodeType).addAll(blockPosSet);
		}
	}

	@Override
	public void markDirty() {
		super.markDirty();
		world.notifyBlockUpdate(pos, getBlockState(), getBlockState(), 3);
	}

	@Override
	public CompoundNBT write(CompoundNBT compound) {
		CompoundNBT compound1 = new CompoundNBT();
		for (NodeType nodeType : NodeType.values()) {
			ListNBT listNBT = new ListNBT();
			Set<BlockPos> posSet = connections.get(nodeType);
			NBTUtil.writeBlockPosList(listNBT, posSet);
			compound1.put(nodeType.toString(), listNBT);
		}
		compound.put(NBTUtil.CONNECTIONS, compound1);
		return super.write(compound);
	}

	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		return write(new CompoundNBT());
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(getPos(), 1, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
		this.func_230337_a_(null,packet.getNbtCompound());
	}

	@Override
	public void tick() {
		if (!connections.isEmpty() && world.getGameTime() % 5 == 0 && !world.isRemote) {
			BlockState state = getBlockState();
			if (state.get(NodeBlock.ITEM) && state.get(NodeBlock.ITEM_INPUT)) {
				Direction dir1 = state.get(NodeBlock.FACING).getOpposite();
				TileEntity inputTileEntity = world.getTileEntity(pos.offset(dir1));
				if (inputTileEntity != null) {
					inputTileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir1).ifPresent(itemHandler -> {
						for (int i = 0; i < itemHandler.getSlots(); i++) {
							ItemStack testStack = itemHandler.extractItem(i, Integer.MAX_VALUE, true);
							if (!testStack.isEmpty()) {
								ItemStack remainder = testStack.copy();
								for (BlockPos pos1 : connections.get(NodeType.ITEM)) {
									BlockState otherNodeState = world.getBlockState(pos1);
									if (otherNodeState.getBlock() instanceof NodeBlock &&
													!otherNodeState.get(NodeBlock.ITEM_INPUT)) {
										Direction dir2 = otherNodeState.get(NodeBlock.FACING).getOpposite();
										TileEntity outputTileEntity = world.getTileEntity(pos1.offset(dir2));
										if (outputTileEntity != null) {
											IItemHandler output = outputTileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir2.getOpposite()).orElse(null);
											for (int j = 0; j < output.getSlots(); j++) {
												ItemStack inserted = output.insertItem(j, remainder, false);
												if (inserted.isEmpty()) {
													int countInserted = remainder.getCount();
													itemHandler.extractItem(i, countInserted, false);
													break;
												}
												if (inserted.getCount() < remainder.getCount()) {
													int countInserted = remainder.getCount() - inserted.getCount();
													remainder.shrink(countInserted);
													itemHandler.extractItem(i, countInserted, false);
												}
											}
											if (remainder.isEmpty()) break;
										}
									}
								}
							}
						}
					});
				}
			}
			if (state.get(NodeBlock.FLUID) && state.get(NodeBlock.FLUID_INPUT)) {
				Direction dir1 = state.get(NodeBlock.FACING).getOpposite();
				TileEntity inputTileEntity = world.getTileEntity(pos.offset(dir1));
				if (inputTileEntity != null) {
					inputTileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir1).ifPresent(iFluidHandler1 -> {

						FluidStack testFluid = iFluidHandler1.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
						if (!testFluid.isEmpty()) {
							for (BlockPos pos1 : connections.get(NodeType.FLUID)) {
								if (testFluid.isEmpty())break;
								BlockState otherNodeState = world.getBlockState(pos1);
								if (otherNodeState.getBlock() instanceof NodeBlock &&
												!otherNodeState.get(NodeBlock.FLUID_INPUT)) {
									Direction dir2 = otherNodeState.get(NodeBlock.FACING);
									TileEntity outputTileEntity = world.getTileEntity(pos1.offset(dir2.getOpposite()));
									if (outputTileEntity != null) {
										outputTileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir2).ifPresent(iFluidHandler2 -> {
											int filled = iFluidHandler2.fill(testFluid, IFluidHandler.FluidAction.EXECUTE);
											iFluidHandler1.drain(filled, IFluidHandler.FluidAction.EXECUTE);
											testFluid.shrink(filled);
										});
									}
								}
							}
						}
					});
				}
			}

			if (state.get(NodeBlock.ENERGY) && state.get(NodeBlock.ENERGY_INPUT)) {
				Direction dir1 = state.get(NodeBlock.FACING).getOpposite();
				TileEntity inputTileEntity = world.getTileEntity(pos.offset(dir1));
				if (inputTileEntity != null) {
					inputTileEntity.getCapability(CapabilityEnergy.ENERGY, dir1).ifPresent(energyInput -> {
						int testEnergy = energyInput.extractEnergy(Integer.MAX_VALUE, true);
						for (BlockPos pos1 : connections.get(NodeType.ENERGY)) {
							BlockState otherNodeState = world.getBlockState(pos1);
							if (otherNodeState.getBlock() instanceof NodeBlock &&
											!otherNodeState.get(NodeBlock.ENERGY_INPUT)) {
								Direction dir2 = otherNodeState.get(NodeBlock.FACING).getOpposite();
								TileEntity outputTileEntity = world.getTileEntity(pos1.offset(dir2));
								if (outputTileEntity != null) {
									IEnergyStorage output = outputTileEntity.getCapability(CapabilityEnergy.ENERGY, dir2).orElse(null);
									if (output != null) {
										int accepted = output.receiveEnergy(testEnergy, false);
										if (accepted > 0) {
											testEnergy -= accepted;
											energyInput.extractEnergy(accepted, false);
											if (testEnergy <= 0) break;
										}
									}
								}
							}
						}
					});
				}
			}

			if (state.get(NodeBlock.GAS) && state.get(NodeBlock.GAS_INPUT)) {
				Direction dir1 = state.get(NodeBlock.FACING);
				TileEntity inputTileEntity = world.getTileEntity(pos.offset(dir1.getOpposite()));
				if (inputTileEntity != null) {/*
					inputTileEntity.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, dir1).ifPresent(iGasHandler -> {
						GasStack gasStack = iGasHandler.extractGas(Integer.MAX_VALUE, Action.SIMULATE);
						if (!gasStack.isEmpty()) {
							for (BlockPos pos1 : connections.get(NodeType.GAS)) {
								BlockState otherNodeState = world.getBlockState(pos1);
								if (otherNodeState.getBlock() instanceof NodeBlock &&
												!otherNodeState.get(NodeBlock.GAS_INPUT)) {
									Direction dir2 = otherNodeState.get(NodeBlock.FACING);
									TileEntity outputTileEntity = world.getTileEntity(pos1.offset(dir2.getOpposite()));
									if (outputTileEntity != null) {
										IGasHandler output = outputTileEntity.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, dir2).orElse(null);
										if (output != null){
										GasStack accepted = output.insertGas(gasStack, Action.EXECUTE);
										if (accepted.isEmpty()) {
											iGasHandler.extractGas(accepted.getAmount(), Action.EXECUTE);break;
										} else if (gasStack.getAmount() < accepted.getAmount()) {
											long amountAccepted = gasStack.getAmount() - accepted.getAmount();
											iGasHandler.extractGas(amountAccepted, Action.EXECUTE);
											gasStack.shrink(amountAccepted);
										}
										}
									}
								}
							}
						}
					});*/
				}
			}
		}
	}
}

