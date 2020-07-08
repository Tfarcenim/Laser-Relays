package tfar.laserrelays;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class NodeBlock extends Block {


	public static final VoxelShape DOWN = Block.makeCuboidShape(5,9,5,11,16,11);

	public static final VoxelShape BLUE_UP = Block.makeCuboidShape(3,0,3,5,9,5);

	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	public static final BooleanProperty ITEM_INPUT = BooleanProperty.create("item_input");
	public static final BooleanProperty FLUID_INPUT = BooleanProperty.create("fluid_input");
	public static final BooleanProperty ENERGY_INPUT = BooleanProperty.create("energy_input");
	public static final BooleanProperty GAS_INPUT = BooleanProperty.create("gas_input");


	public static final BooleanProperty ITEM = BooleanProperty.create("item");
	public static final BooleanProperty FLUID = BooleanProperty.create("fluid");
	public static final BooleanProperty ENERGY = BooleanProperty.create("energy");
	public static final BooleanProperty GAS = BooleanProperty.create("gas");

	public static final List<BooleanProperty> props = Lists.newArrayList(ITEM,FLUID,ENERGY,ITEM);

	public NodeBlock(Properties properties) {
		super(properties);
		this.setDefaultState(this.stateContainer.getBaseState()
						.with(ITEM_INPUT,true)
						.with(FLUID_INPUT,true)
						.with(ENERGY_INPUT,true)
						.with(GAS_INPUT,true)

						.with(ITEM,false)
						.with(FLUID,false)
						.with(ENERGY,false)
						.with(GAS,false));
	}

	@Override
	public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult result) {

		ItemStack stack = player.getHeldItem(handIn);
		if (!worldIn.isRemote) {
			if (stack.getItem() == ExampleMod.wire) {
				if (stack.getOrCreateTag().contains("connected")) {

					NodeType nodeTypeFromWire = NodeType.valueOf(stack.getTag().getString("node_type"));

					BlockPos other = NBTUtil.readBlockPos(stack.getTag());
					if (pos.equals(other)){
						player.sendMessage(new StringTextComponent("cannot connect a node to itself"),player.getUniqueID());
					} else {

						NodeBlockEntity thisTerminal = (NodeBlockEntity) worldIn.getTileEntity(pos);
						thisTerminal.connect(other,nodeTypeFromWire);

						NodeBlockEntity otherTerminal = (NodeBlockEntity) worldIn.getTileEntity(other);
						otherTerminal.connect(pos,nodeTypeFromWire);
						stack.setTag(null);
					}
				} else {
					NodeType nodeType = getNodeFromTrace(result,state.get(FACING));
					NBTUtil.writeBlockPos(stack.getOrCreateTag(), pos);
					stack.getTag().putString("node_type",nodeType.toString());
				}
			} else if (player.getHeldItem(handIn).isEmpty()) {
				NodeType nodeType = getNodeFromTrace(result,state.get(FACING));
				switch (nodeType) {
					case ITEM:worldIn.setBlockState(pos,state.with(ITEM_INPUT,!state.get(ITEM_INPUT)));break;
					case FLUID:worldIn.setBlockState(pos,state.with(FLUID_INPUT,!state.get(FLUID_INPUT)));break;
					case ENERGY:worldIn.setBlockState(pos,state.with(ENERGY_INPUT,!state.get(ENERGY_INPUT)));break;
					case GAS:worldIn.setBlockState(pos,state.with(GAS_INPUT,!state.get(GAS_INPUT)));break;

				}
			}
		}
		return ActionResultType.SUCCESS;
	}

	public static double trim(double a) {
		return a - Math.floor(a);
	}

	public static NodeType getNodeFromTrace(BlockRayTraceResult result,Direction stateFacing){

		Vector3d hitVec = result.getHitVec();

		Vector3d frac = new Vector3d(trim(hitVec.x),trim(hitVec.y),trim(hitVec.z));

		switch (stateFacing){
			case WEST:
				if (frac.y > .5 && frac.z > .5)return NodeType.ENERGY;
				if (frac.y > .5 && frac.z < .5)return NodeType.GAS;
				if (frac.y < .5 && frac.z > .5)return NodeType.ITEM;
				if (frac.y < .5 && frac.z < .5)return NodeType.FLUID;

			case SOUTH:
				if (frac.x > .5 && frac.y > .5)return NodeType.ENERGY;
				if (frac.x > .5 && frac.y < .5)return NodeType.ITEM;
				if (frac.x < .5 && frac.y > .5)return NodeType.GAS;
				if (frac.x < .5 && frac.y < .5)return NodeType.FLUID;

			case EAST:
				if (frac.y > .5 && frac.z > .5)return NodeType.GAS;
				if (frac.y > .5 && frac.z < .5)return NodeType.ENERGY;
				if (frac.y < .5 && frac.z > .5)return NodeType.FLUID;
				if (frac.y < .5 && frac.z < .5)return NodeType.ITEM;

			case NORTH: {
				if (frac.x > .5 && frac.y > .5) return NodeType.GAS;
				if (frac.x > .5 && frac.y < .5) return NodeType.FLUID;
				if (frac.x < .5 && frac.y > .5) return NodeType.ENERGY;
				if (frac.x < .5 && frac.y < .5) return NodeType.ITEM;
			}

			case DOWN:
				if (frac.x > .5 && frac.z > .5)return NodeType.FLUID;
				if (frac.x > .5 && frac.z < .5)return NodeType.GAS;
				if (frac.x < .5 && frac.z > .5)return NodeType.ITEM;
				if (frac.x < .5 && frac.z < .5)return NodeType.ENERGY;

			case UP: {
				if (frac.x > .5 && frac.z < .5) return NodeType.FLUID;
				if (frac.x < .5 && frac.z < .5) return NodeType.ITEM;
				if (frac.x < .5 && frac.z > .5) return NodeType.ENERGY;
				if (frac.x > .5 && frac.z > .5) return NodeType.GAS;
			}
		}
		System.out.println("miss");
		return NodeType.ITEM;
	}

	@Override
	public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		NodeBlockEntity thisNode = (NodeBlockEntity)worldIn.getTileEntity(pos);
		//block is gone
		if (newState.getBlock() != state.getBlock()) {
			for (NodeType nodeType : NodeType.values()) {
				for (BlockPos otherPos : thisNode.connections.get(nodeType)) {
					NodeBlockEntity otherNode = (NodeBlockEntity) worldIn.getTileEntity(otherPos);
					otherNode.disconnect(pos, nodeType);
				}
			}
		}
		//nodes changed
		else if (newState != state) {
			if (isBreak(state,newState)) {
				NodeType nodeType = getBreak(state,newState);
					for (BlockPos otherPos : thisNode.connections.get(nodeType)) {
						NodeBlockEntity otherNode = (NodeBlockEntity) worldIn.getTileEntity(otherPos);
						otherNode.disconnect(pos, nodeType);
						thisNode.disconnectNode(nodeType);
				}
			}
		}
		super.onReplaced(state, worldIn, pos, newState, isMoving);
	}

	public boolean isBreak(BlockState old, BlockState newState) {
		return props.stream().anyMatch(bool -> old.get(bool) && !newState.get(bool));
	}

	public NodeType getBreak(BlockState old, BlockState newState) {
		return props.stream().filter(property -> old.get(property) && !newState.get(property)).findFirst()
						.map(property -> NodeType.valueOf(property.getName().toUpperCase()))
						.orElseThrow(IllegalStateException::new);
	}

	@Override
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
		worldIn.getPendingBlockTicks().scheduleTick(pos, this, 5);
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		List<VoxelShape> shapes = new ArrayList<>();
		switch (state.get(FACING)) {
			case UP: {
				if (state.get(ITEM)) {
					shapes.add(Block.makeCuboidShape(3,0,3,5,9,5));
				}
				if (state.get(FLUID)) {
					shapes.add(Block.makeCuboidShape(11,0,3,13,9,5));
				}
				if (state.get(ENERGY)) {
					shapes.add(Block.makeCuboidShape(3,0,11,5,9,13));
				}
				if (state.get(GAS)) {
					shapes.add(Block.makeCuboidShape(11,0,11,13,9,13));
				}
				return VoxelShapes.or(VoxelShapes.empty(),shapes.toArray(new VoxelShape[0]));
			}

			case DOWN: {
				if (state.get(ITEM)) {
					shapes.add(Block.makeCuboidShape(3,7,11,5,16,13));
				}
				if (state.get(FLUID)) {
					shapes.add(Block.makeCuboidShape(11,7,11,13,16,13));
				}
				if (state.get(ENERGY)) {
					shapes.add(Block.makeCuboidShape(3,7,3,5,16,5));
				}
				if (state.get(GAS)) {
					shapes.add(Block.makeCuboidShape(11,7,3,13,16,5));
				}
				return VoxelShapes.or(VoxelShapes.empty(),shapes.toArray(new VoxelShape[0]));
			}

			case NORTH: {
				if (state.get(ITEM)) {
					shapes.add(Block.makeCuboidShape(3,3,7,5,5,16));
				}
				if (state.get(FLUID)) {
					shapes.add(Block.makeCuboidShape(11,3,7,13,5,16));
				}
				if (state.get(ENERGY)) {
					shapes.add(Block.makeCuboidShape(3,11,7,5,13,16));
				}
				if (state.get(GAS)) {
					shapes.add(Block.makeCuboidShape(11,11,7,13,13,16));
				}
				return VoxelShapes.or(VoxelShapes.empty(),shapes.toArray(new VoxelShape[0]));
			}

			case SOUTH: {
				if (state.get(ITEM)) {
					shapes.add(Block.makeCuboidShape(11,3,0,13,5,9));
				}
				if (state.get(FLUID)) {
					shapes.add(Block.makeCuboidShape(3,3,0,5,5,9));
				}
				if (state.get(ENERGY)) {
					shapes.add(Block.makeCuboidShape(11,11,0,13,13,9));
				}
				if (state.get(GAS)) {
					shapes.add(Block.makeCuboidShape(3,11,0,5,13,9));
				}
				return VoxelShapes.or(VoxelShapes.empty(),shapes.toArray(new VoxelShape[0]));
			}

			case EAST: {
				if (state.get(ITEM)) {
					shapes.add(Block.makeCuboidShape(0, 3, 3, 9, 5, 5));
				}
				if (state.get(FLUID)) {
					shapes.add(Block.makeCuboidShape(0, 3, 11, 9, 5, 13));
				}
				if (state.get(ENERGY)) {
					shapes.add(Block.makeCuboidShape(0, 11, 3, 9, 13, 5));
				}
				if (state.get(GAS)) {
					shapes.add(Block.makeCuboidShape(0, 11, 11, 9, 13, 13));
				}
				return VoxelShapes.or(VoxelShapes.empty(), shapes.toArray(new VoxelShape[0]));
			}

				case WEST: {
					if (state.get(ITEM)) {
						shapes.add(Block.makeCuboidShape(7,3,11,16,5,13));
					}
					if (state.get(FLUID)) {
						shapes.add(Block.makeCuboidShape(7,3,3,16,5,5));
					}
					if (state.get(ENERGY)) {
						shapes.add(Block.makeCuboidShape(7,11,11,16,13,13));
					}
					if (state.get(GAS)) {
						shapes.add(Block.makeCuboidShape(7,11,3,16,13,5));
					}

				return VoxelShapes.or(VoxelShapes.empty(),shapes.toArray(new VoxelShape[0]));
			}

		}
		return super.getShape(state, worldIn, pos, context);
	}

	public static void replace(BlockEvent.BreakEvent e) {
		BlockState state = e.getState();
		PlayerEntity player = e.getPlayer();
		BlockPos pos = e.getPos();
		IWorld world = e.getWorld();

		if (state.getBlock() instanceof NodeBlock) {
			RayTraceResult result = player.pick(player.getAttribute(ForgeMod.REACH_DISTANCE.get()).getValue(), 0, false);
			if (result instanceof BlockRayTraceResult) {
				NodeType nodeType = getNodeFromTrace((BlockRayTraceResult)result,state.get(FACING));
				BlockState newState = breakNode(state,nodeType);
				if (!newState.isAir(world, pos)) {
					world.setBlockState(pos,newState,3);
					ItemStack stack = new ItemStack(getItemFromNode(nodeType));
					world.addEntity(new ItemEntity((World) world,e.getPos().getX(),e.getPos().getY() + .5,e.getPos().getZ(),stack));
					e.setCanceled(true);
				}
			}
		}
	}

	public static Item getItemFromNode(NodeType node) {
		switch (node) {
			case ITEM:return ExampleMod.item_node;
			case FLUID:return ExampleMod.fluid_node;
			case ENERGY:return ExampleMod.energy_node;
			case GAS:return ExampleMod.gas_node;
		}
		throw new IllegalStateException();
	}

	public static BlockState breakNode(BlockState original,NodeType nodeType) {
		boolean broken = false;
		BlockState newState = original;
		switch (nodeType) {
			case ITEM: {
				if (original.get(FLUID) || original.get(ENERGY) || original.get(GAS))
				newState = newState.with(ITEM,false);
				else broken = true;
			}break;
			case FLUID: {
				if (original.get(ITEM) || original.get(ENERGY) || original.get(GAS))
					newState = newState.with(FLUID,false);
				else broken = true;
			}break;
			case ENERGY: {
				if (original.get(ITEM) || original.get(FLUID) || original.get(GAS))
					newState = newState.with(ENERGY,false);
				else broken = true;
			}break;
			case GAS: {
				if (original.get(ITEM) || original.get(FLUID) || original.get(ENERGY))
					newState = newState.with(GAS,false);
				else broken = true;
			}
		}

		if (broken) {
			return Blocks.AIR.getDefaultState();
		} else {
			return newState;
		}
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		ItemStack stack = context.getItem();
		if (stack.getItem() instanceof NodeBlockItem) {
			BlockState curState = context.getWorld().getBlockState(context.getPos());
			NodeType nodeType = ((NodeBlockItem) stack.getItem()).nodeType;

			if (!(curState.getBlock() instanceof NodeBlock)) {
				BlockState newState = super.getStateForPlacement(context);
				newState = newState.with(FACING,context.getFace());
				switch (nodeType) {
					case ITEM:
							return newState.with(ITEM, true);
					case FLUID:
							return newState.with(FLUID, true);

					case ENERGY:
							return newState.with(ENERGY, true);
					case GAS:
						return newState.with(GAS, true);
				}
				return null;
			}
			else {
				switch (nodeType) {
					case ITEM:
						if (!curState.get(ITEM)) {
							return curState.with(ITEM, true);
						}
					case FLUID:
						if (!curState.get(FLUID)) {
							return curState.with(FLUID, true);
						}

					case ENERGY:
						if (!curState.get(ENERGY)) {
							return curState.with(ENERGY, true);
						}
					case GAS:
						if (!curState.get(GAS)) {
							return curState.with(GAS, true);
						}
				}
				return null;
			}
		}
		return super.getStateForPlacement(context);
	}

	public boolean isReplaceable(BlockState state, BlockItemUseContext useContext) {
		Item item = useContext.getItem().getItem();

		if (item instanceof NodeBlockItem) {
			NodeType type = ((NodeBlockItem)item).nodeType;
			switch (type) {
				case ITEM:return !state.get(ITEM);
				case FLUID:return !state.get(FLUID);
				case ENERGY:return !state.get(ENERGY);
				case GAS:return !state.get(GAS);
			}
		}
		return super.isReplaceable(state, useContext);
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING,ITEM,FLUID,ENERGY,GAS,ITEM_INPUT,FLUID_INPUT,ENERGY_INPUT,GAS_INPUT);
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new NodeBlockEntity();
	}

}
