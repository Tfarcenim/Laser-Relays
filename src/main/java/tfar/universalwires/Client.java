package tfar.universalwires;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public class Client extends RenderState {

	public Client(String nameIn, Runnable setupTaskIn, Runnable clearTaskIn) {
		super(nameIn, setupTaskIn, clearTaskIn);
	}

	public static final RenderType LINES = RenderType.get("lines", DefaultVertexFormats.POSITION_COLOR, 1, 256, RenderType.State.builder().line(new RenderState.LineState(OptionalDouble.of(2))).layer(PROJECTION_LAYERING).transparency(TRANSLUCENT_TRANSPARENCY).writeMask(COLOR_WRITE).build(false));


	public static void render(RenderWorldLastEvent e) {
		List<NodeBlockEntity> blockEntities = Minecraft.getInstance().world.loadedTileEntityList
						.stream()
						.filter(NodeBlockEntity.class::isInstance)
						.map(NodeBlockEntity.class::cast)
						.collect(Collectors.toList());

		MatrixStack stack = e.getMatrixStack();
		Vec3d vec3d = TileEntityRendererDispatcher.instance.renderInfo.getProjectedView();

		stack.translate(-vec3d.x, -vec3d.y, -vec3d.z);

		IRenderTypeBuffer buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
		IVertexBuilder builder = buffer.getBuffer(LINES);

		for (NodeBlockEntity terminalBlockEntity : blockEntities) {
			BlockState state = terminalBlockEntity.getBlockState();

			boolean items = state.get(NodeBlock.ITEM_INPUT);
			boolean fluids = state.get(NodeBlock.FLUID_INPUT);
			boolean energy = state.get(NodeBlock.ENERGY_INPUT);
			boolean gas = state.get(NodeBlock.GAS_INPUT);

			if (items && state.get(NodeBlock.ITEM)) {
				for (BlockPos pos : terminalBlockEntity.connections.get(NodeType.ITEM)) {
					renderline(0, stack,builder, terminalBlockEntity, pos, NodeType.ITEM);
				}
			}
			if (fluids && state.get(NodeBlock.FLUID)) {
				for (BlockPos pos : terminalBlockEntity.connections.get(NodeType.FLUID)) {
					renderline(0, stack, builder, terminalBlockEntity, pos, NodeType.FLUID);
				}
			}
			if (energy && state.get(NodeBlock.ENERGY)) {
				for (BlockPos pos : terminalBlockEntity.connections.get(NodeType.ENERGY)) {
					renderline(0, stack, builder, terminalBlockEntity, pos, NodeType.ENERGY);
				}
			}
			if (gas && state.get(NodeBlock.GAS)) {
				for (BlockPos pos : terminalBlockEntity.connections.get(NodeType.GAS)) {
					renderline(0, stack, builder, terminalBlockEntity, pos, NodeType.GAS);
				}
			}
		}
		Minecraft.getInstance().getRenderTypeBuffers().getBufferSource().finish(LINES);
	}


	private static void renderline(float partialTicks, MatrixStack matrices, IVertexBuilder bufferIn, NodeBlockEntity nodeBlockEntity, BlockPos to, NodeType nodeType) {

		float red = nodeType == NodeType.ENERGY || nodeType == NodeType.GAS ? 1 : 0;
		float green = nodeType == NodeType.ITEM || nodeType == NodeType.GAS ? 1 : 0;
		float blue = nodeType == NodeType.FLUID ? 1 : 0;
		float alpha = 1;

		BlockPos from = nodeBlockEntity.getPos();
		BlockState stateFrom = nodeBlockEntity.getBlockState();
		BlockState stateTo = Minecraft.getInstance().world.getBlockState(to);

		double x1 = from.getX() + getXOffSet(stateFrom.get(NodeBlock.FACING),nodeType);
		double y1 = from.getY() + getYOffSet(stateFrom.get(NodeBlock.FACING),nodeType);
		double z1 = from.getZ() + getZOffSet(stateFrom.get(NodeBlock.FACING),nodeType);

		double x2 = to.getX() + getXOffSet(stateTo.get(NodeBlock.FACING),nodeType);
		double y2 = to.getY() + getYOffSet(stateTo.get(NodeBlock.FACING),nodeType);
		double z2 = to.getZ() + getZOffSet(stateTo.get(NodeBlock.FACING),nodeType);

		Matrix4f matrix4f = matrices.getLast().getPositionMatrix();
		bufferIn.pos(matrix4f, (float)x1, (float)y1, (float)z1).color(red, green, blue, alpha).endVertex();
		bufferIn.pos(matrix4f,(float)x2, (float)y2, (float)z2).color(red, green, blue, alpha).endVertex();
	}

	public static double getXOffSet(Direction direction, NodeType nodeType) {
		switch (direction) {
			case UP:
				switch (nodeType){
					case ITEM:return .25;
					case FLUID:return .75;
					case GAS:return .75;
					case ENERGY:return .25;
				}
			case DOWN:
				switch (nodeType){
					case ITEM:
					case FLUID:
					case GAS:
					case ENERGY:
				}
			case NORTH:
				switch (nodeType){
					case ITEM:return .25;
					case FLUID:
					case GAS:
					case ENERGY:
				}
			case EAST:
			case WEST:return .5;
			case SOUTH:
				switch (nodeType){
					case ITEM:return .75;
					case FLUID:
					case GAS:
					case ENERGY:return .75;
				}
		}
		return 0;
	}

	public static double getYOffSet(Direction direction,NodeType nodeType) {
		switch (direction) {
			case UP:
			case DOWN:return .5;

			case NORTH:
				switch (nodeType){
					case ITEM:return .25;
					case FLUID:
					case GAS:
					case ENERGY:return .75;
				}
			case EAST:
				switch (nodeType){
					case ITEM:return .25;
					case FLUID:
					case GAS:
					case ENERGY:return .75;
				}
			case SOUTH:
				switch (nodeType){
					case ITEM:return .25;
					case FLUID:
					case GAS:
					case ENERGY:return .75;
				}
			case WEST:
				switch (nodeType){
					case ITEM:return .25;
					case FLUID:
					case GAS:
					case ENERGY:return .75;
				}
		}
		return 0;
	}

	public static double getZOffSet(Direction direction,NodeType nodeType) {
		switch (direction) {
			case UP:
				switch (nodeType){
					case ITEM:
					case FLUID:return .25;
					case GAS:
					case ENERGY:return .75;
				}
			case DOWN:
				switch (nodeType){
					case ITEM:
					case FLUID:
					case GAS:
					case ENERGY:
				}
			case NORTH:
				switch (nodeType){
					case ITEM:
					case FLUID:
					case GAS:
					case ENERGY:return .5;
				}
			case EAST:
				switch (nodeType){
					case ITEM:return .25;
					case FLUID:
					case GAS:
					case ENERGY:
				}
			case SOUTH:
				switch (nodeType){
					case ITEM:
					case FLUID:
					case GAS:
					case ENERGY:return .5;
				}
			case WEST:
				switch (nodeType){
					case ITEM:return .75;
					case FLUID:
					case GAS:
					case ENERGY:return .75;
				}
		}
		return 0;
	}
}
