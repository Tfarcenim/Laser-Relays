package tfar.laserrelays;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public class Client extends RenderState {

	public Client(String nameIn, Runnable setupTaskIn, Runnable clearTaskIn) {
		super(nameIn, setupTaskIn, clearTaskIn);
	}

	public static final RenderType LINES = RenderType.makeType("lines", DefaultVertexFormats.POSITION_COLOR, 1, 256, RenderType.State.getBuilder().
					line(new RenderState.LineState(OptionalDouble.of(2))).layer(field_239235_M_).transparency(TRANSLUCENT_TRANSPARENCY).writeMask(COLOR_WRITE).build(false));

	public static final Minecraft mc = Minecraft.getInstance();

	public static void render(RenderWorldLastEvent e) {
		List<NodeBlockEntity> blockEntities = Minecraft.getInstance().world.loadedTileEntityList
						.stream()
						.filter(NodeBlockEntity.class::isInstance)
						.map(NodeBlockEntity.class::cast)
						.collect(Collectors.toList());

		MatrixStack matrices = e.getMatrixStack();
		Vector3d vec3d = TileEntityRendererDispatcher.instance.renderInfo.getProjectedView();

		matrices.translate(-vec3d.x, -vec3d.y, -vec3d.z);

		IRenderTypeBuffer buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
		IVertexBuilder builder = buffer.getBuffer(LINES);

		for (NodeBlockEntity terminalBlockEntity : blockEntities) {
			BlockState state = terminalBlockEntity.getBlockState();

			NodeType whitelistNode = NodeType.getNodeFromStack(Minecraft.getInstance().player.getHeldItem(Hand.MAIN_HAND));

			boolean items = state.get(NodeBlock.ITEM_INPUT) && (whitelistNode == null || whitelistNode == NodeType.ITEM);
			boolean fluids = state.get(NodeBlock.FLUID_INPUT) && (whitelistNode == null || whitelistNode == NodeType.FLUID);
			boolean energy = state.get(NodeBlock.ENERGY_INPUT) && (whitelistNode == null || whitelistNode == NodeType.ENERGY);
			boolean gas = state.get(NodeBlock.GAS_INPUT) && (whitelistNode == null || whitelistNode == NodeType.GAS);

			if (items && state.get(NodeBlock.ITEM)) {
				for (BlockPos pos : terminalBlockEntity.connections.get(NodeType.ITEM)) {
					renderline(matrices, builder, terminalBlockEntity, pos, NodeType.ITEM);
				}
			}
			if (fluids && state.get(NodeBlock.FLUID)) {
				for (BlockPos pos : terminalBlockEntity.connections.get(NodeType.FLUID)) {
					renderline(matrices, builder, terminalBlockEntity, pos, NodeType.FLUID);
				}
			}
			if (energy && state.get(NodeBlock.ENERGY)) {
				for (BlockPos pos : terminalBlockEntity.connections.get(NodeType.ENERGY)) {
					renderline(matrices, builder, terminalBlockEntity, pos, NodeType.ENERGY);
				}
			}
			if (gas && state.get(NodeBlock.GAS)) {
				for (BlockPos pos : terminalBlockEntity.connections.get(NodeType.GAS)) {
					renderline(matrices, builder, terminalBlockEntity, pos, NodeType.GAS);
				}
			}
		}

		ItemStack stack1 = mc.player.getHeldItem(Hand.MAIN_HAND);
		if (stack1.getItem() instanceof WireItem) {
			if (stack1.getTag() != null && stack1.getTag().contains("node_type")) {
				NodeBlockEntity nodeBlockEntity = ((NodeBlockEntity)mc.world.getTileEntity(NBTUtil.readBlockPos(stack1.getTag())));
				if (nodeBlockEntity != null) {

				}
			}
		}
		Minecraft.getInstance().getRenderTypeBuffers().getBufferSource().finish(LINES);
	}

	private static void renderlineToPlayer(MatrixStack matrices, IVertexBuilder bufferIn, NodeBlockEntity nodeBlockEntity, Vector3d to, NodeType nodeType) {

		float red = nodeType == NodeType.ENERGY || nodeType == NodeType.GAS ? 1 : 0;
		float green = nodeType == NodeType.ITEM || nodeType == NodeType.GAS ? 1 : 0;
		float blue = nodeType == NodeType.FLUID ? 1 : 0;
		float alpha = 1;

		BlockPos from = nodeBlockEntity.getPos();
		BlockState stateFrom = nodeBlockEntity.getBlockState();

		double x1 = from.getX() + getXOffSet(stateFrom.get(NodeBlock.FACING), nodeType);
		double y1 = from.getY() + getYOffSet(stateFrom.get(NodeBlock.FACING), nodeType);
		double z1 = from.getZ() + getZOffSet(stateFrom.get(NodeBlock.FACING), nodeType);

		double x2 = to.x;
		double y2 = to.y +1;
		double z2 = to.z;

		Matrix4f matrix4f = matrices.getLast().getMatrix();
		bufferIn.pos(matrix4f, (float) x1, (float) y1, (float) z1).color(red, green, blue, alpha).endVertex();
		bufferIn.pos(matrix4f, (float) x2, (float) y2, (float) z2).color(red, green, blue, alpha).endVertex();
	}

	private static void renderline(MatrixStack matrices, IVertexBuilder bufferIn, NodeBlockEntity nodeBlockEntity, BlockPos to, NodeType nodeType) {

		float red = nodeType == NodeType.ENERGY || nodeType == NodeType.GAS ? 1 : 0;
		float green = nodeType == NodeType.ITEM || nodeType == NodeType.GAS ? 1 : 0;
		float blue = nodeType == NodeType.FLUID ? 1 : 0;
		float alpha = 1;

		BlockPos from = nodeBlockEntity.getPos();
		BlockState stateFrom = nodeBlockEntity.getBlockState();
		BlockState stateTo = Minecraft.getInstance().world.getBlockState(to);

		if (stateTo.getBlock() instanceof NodeBlock) {

			double x1 = from.getX() + getXOffSet(stateFrom.get(NodeBlock.FACING), nodeType);
			double y1 = from.getY() + getYOffSet(stateFrom.get(NodeBlock.FACING), nodeType);
			double z1 = from.getZ() + getZOffSet(stateFrom.get(NodeBlock.FACING), nodeType);

			double x2 = to.getX() + getXOffSet(stateTo.get(NodeBlock.FACING), nodeType);
			double y2 = to.getY() + getYOffSet(stateTo.get(NodeBlock.FACING), nodeType);
			double z2 = to.getZ() + getZOffSet(stateTo.get(NodeBlock.FACING), nodeType);

			Matrix4f matrix4f = matrices.getLast().getMatrix();
			bufferIn.pos(matrix4f, (float) x1, (float) y1, (float) z1).color(red, green, blue, alpha).endVertex();
			bufferIn.pos(matrix4f, (float) x2, (float) y2, (float) z2).color(red, green, blue, alpha).endVertex();
		}
	}

	public static double getXOffSet(Direction direction, NodeType nodeType) {
		switch (direction) {
			case UP:
				switch (nodeType) {
					case ITEM:
					case ENERGY:
						return .25;
					case FLUID:
					case GAS:
						return .75;
				}
			case DOWN:
				switch (nodeType) {
					case ITEM:
					case ENERGY:
						return .25;
					case FLUID:
					case GAS:
						return .75;
				}
			case NORTH:
				switch (nodeType) {
					case ITEM:
					case ENERGY:
						return .25;
					case FLUID:
					case GAS:
						return .75;
				}
			case EAST:
			case WEST:
				return .5;
			case SOUTH:
				switch (nodeType) {
					case ITEM:
					case ENERGY:
						return .75;
					case FLUID:
					case GAS:
						return .25;
				}
		}
		return 0;
	}

	public static double getYOffSet(Direction direction, NodeType nodeType) {
		switch (direction) {
			case UP:
			case DOWN:
				return .5;

			case NORTH:
				switch (nodeType) {
					case ITEM:
					case FLUID:
						return .25;
					case GAS:
					case ENERGY:
						return .75;
				}
			case EAST:
				switch (nodeType) {
					case ITEM:
					case FLUID:
						return .25;
					case GAS:
					case ENERGY:
						return .75;
				}
			case SOUTH:
				switch (nodeType) {
					case ITEM:
					case FLUID:
						return .25;
					case GAS:
					case ENERGY:
						return .75;
				}
			case WEST:
				switch (nodeType) {
					case ITEM:
					case FLUID:
						return .25;
					case GAS:
					case ENERGY:
						return .75;
				}
		}
		return 0;
	}

	public static double getZOffSet(Direction direction, NodeType nodeType) {
		switch (direction) {
			case UP:
				switch (nodeType) {
					case ITEM:
					case FLUID:
						return .25;
					case GAS:
					case ENERGY:
						return .75;
				}
			case DOWN:
				switch (nodeType) {
					case ITEM:
					case FLUID:
						return .75;
					case GAS:
					case ENERGY:
						return .25;
				}
			case NORTH:
			case SOUTH:
				return .5;
			case EAST:
				switch (nodeType) {
					case ITEM:
					case ENERGY:
						return .25;
					case FLUID:
					case GAS:
						return .75;
				}
			case WEST:
				switch (nodeType) {
					case ITEM:
					case ENERGY:
						return .75;
					case FLUID:
					case GAS:
						return .25;
				}
		}
		return 0;
	}

	public static void scroll(InputEvent.MouseScrollEvent e) {
		PlayerEntity player = Minecraft.getInstance().player;
		if (player != null) {
			ItemStack held = player.getHeldItem(Hand.MAIN_HAND);
			if (held.getItem() instanceof ColorFilterItem && player.isSneaking()) {
				e.setCanceled(true);
			}
		}
	}
}
