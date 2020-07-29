package tfar.laserrelays;

import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class NodeBlockEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {

	protected Map<NodeType, Set<BlockPos>> connections = new EnumMap<>(NodeType.class);

	protected HashSet<NodeType> whitelist = new HashSet<>();

	public FilterItemStackHandler filter = new FilterItemStackHandler(9) {
		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			markDirty();
		}
	};

	public int max_slots = 9;

	public int index = 0;
	public int lastIndex = 0;

	int tick_rate = 5;

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

	public void disconnect(BlockPos other, NodeType node) {
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
	public void read(BlockState state, CompoundNBT compound) {
		super.read(state, compound);
		CompoundNBT nbt = compound.getCompound(NBTUtil.CONNECTIONS);
		for (NodeType nodeType : NodeType.values()) {
			connections.get(nodeType).clear();
			ListNBT listNBT = nbt.getList(nodeType.toString(), Constants.NBT.TAG_COMPOUND);
			Set<BlockPos> blockPosSet = new HashSet<>();
			NBTUtil.readBlockPosList(listNBT, blockPosSet);
			connections.get(nodeType).addAll(blockPosSet);
		}
		filter.deserializeNBT(compound.getCompound("filter"));
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
		compound.put("filter",filter.serializeNBT());
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
		this.read(null, packet.getNbtCompound());
	}

	@Override
	public void tick() {
		if (!connections.isEmpty() && world.getGameTime() % 5 == 0 && !world.isRemote) {
			BlockState state = getBlockState();
			if (state.get(NodeBlock.ITEM) && state.get(NodeBlock.ITEM_INPUT)) {
				tickItems(state);
			}
			if (state.get(NodeBlock.FLUID) && state.get(NodeBlock.FLUID_INPUT)) {
				Direction dir1 = state.get(NodeBlock.FACING).getOpposite();
				TileEntity inputTileEntity = world.getTileEntity(pos.offset(dir1));
				if (inputTileEntity != null) {
					inputTileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir1).ifPresent(iFluidHandler1 -> {

						FluidStack testFluid = iFluidHandler1.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
						if (!testFluid.isEmpty()) {
							for (BlockPos pos1 : connections.get(NodeType.FLUID)) {
								if (testFluid.isEmpty()) break;
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
				if (inputTileEntity != null) {
					inputTileEntity.getCapability(ExampleMod.GAS_HANDLER_CAPABILITY, dir1).ifPresent(iGasHandler -> {
						GasStack gasStack = iGasHandler.extractChemical(Integer.MAX_VALUE, Action.SIMULATE);
						if (!gasStack.isEmpty()) {
							for (BlockPos pos1 : connections.get(NodeType.GAS)) {
								BlockState otherNodeState = world.getBlockState(pos1);
								if (otherNodeState.getBlock() instanceof NodeBlock &&
												!otherNodeState.get(NodeBlock.GAS_INPUT)) {
									Direction dir2 = otherNodeState.get(NodeBlock.FACING);
									TileEntity outputTileEntity = world.getTileEntity(pos1.offset(dir2.getOpposite()));
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
	}

	public void tickItems(BlockState state) {
		Direction dir1 = state.get(NodeBlock.FACING);
		TileEntity inputTileEntity = world.getTileEntity(pos.offset(dir1.getOpposite()));
		if (inputTileEntity != null) {
			inputTileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir1).ifPresent(itemHandler -> {
				int slots = itemHandler.getSlots();
				int j = lastIndex;
				int toScan = Math.min(max_slots,slots);
				for (int i = 0; i < toScan; i++) {
					if (j >= slots) {
						j = 0;
					}
					ItemStack testStack = itemHandler.extractItem(j, Integer.MAX_VALUE, true);
					if (!testStack.isEmpty() && checkInputFilter(testStack)) {
						ItemStack remainder = testStack.copy();
						for (BlockPos pos1 : connections.get(NodeType.ITEM)) {
							BlockState otherNodeState = world.getBlockState(pos1);
							if (otherNodeState.getBlock() instanceof NodeBlock &&
											!otherNodeState.get(NodeBlock.ITEM_INPUT)) {
								Direction dir2 = otherNodeState.get(NodeBlock.FACING).getOpposite();
								TileEntity outputTileEntity = world.getTileEntity(pos1.offset(dir2));
								if (outputTileEntity != null) {
									IItemHandler output = outputTileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir2.getOpposite()).orElse(null);
									if (output != null) {
										for (int k = 0; k < output.getSlots(); k++) {
											ItemStack inserted = output.insertItem(k, remainder, false);
											if (inserted.isEmpty()) {
												int countInserted = remainder.getCount();
												itemHandler.extractItem(j, countInserted, false);
												break;
											}
											if (inserted.getCount() < remainder.getCount()) {
												int countInserted = remainder.getCount() - inserted.getCount();
												remainder.shrink(countInserted);
												itemHandler.extractItem(j, countInserted, false);
											}
										}
										if (remainder.isEmpty()) break;
									}
								}
							}
						}
					}
					j++;
				}
				lastIndex = j;
			});
		}
	}

	protected boolean checkInputFilter(ItemStack testStack) {
		return filter.test(testStack);
	}

	@Override
	public ITextComponent getDisplayName() {
		return new StringTextComponent("filter");
	}

	@Nullable
	@Override
	public Container createMenu(int p_createMenu_1_, PlayerInventory p_createMenu_2_, PlayerEntity p_createMenu_3_) {
		return new NodeContainer(p_createMenu_1_, p_createMenu_2_, pos);
	}
}

