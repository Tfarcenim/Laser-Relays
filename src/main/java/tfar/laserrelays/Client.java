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
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;
import tfar.laserrelays.item.ColorFilterItem;

import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public class Client extends RenderType {

	public static final RenderType LINES2 = makeType("lines", DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 256,
					RenderType.State.getBuilder().line(new RenderState.LineState(OptionalDouble.of(2))).layer(field_239235_M_).transparency(TRANSLUCENT_TRANSPARENCY)
					.target(field_241712_U_).writeMask(COLOR_DEPTH_WRITE).build(false));

	public static RenderType getCubeType() {
		RenderType.State renderTypeState = RenderType.State.getBuilder().transparency(TRANSLUCENT_TRANSPARENCY).cull(CullState.CULL_DISABLED).build(true);
		return RenderType.makeType("cube", DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL, GL11.GL_QUADS, 256, true, true, renderTypeState);
	}

	public static final Minecraft mc = Minecraft.getInstance();

	public Client(String nameIn, VertexFormat formatIn, int drawModeIn, int bufferSizeIn, boolean useDelegateIn, boolean needsSortingIn, Runnable setupTaskIn, Runnable clearTaskIn) {
		super(nameIn, formatIn, drawModeIn, bufferSizeIn, useDelegateIn, needsSortingIn, setupTaskIn, clearTaskIn);
	}

	public static void render(RenderWorldLastEvent e) {
		List<NodeBlockEntity> nodes = Minecraft.getInstance().world.loadedTileEntityList
						.stream()
						.filter(NodeBlockEntity.class::isInstance)
						.map(NodeBlockEntity.class::cast)
						.collect(Collectors.toList());

		MatrixStack matrices = e.getMatrixStack();
		Vector3d vec3d = TileEntityRendererDispatcher.instance.renderInfo.getProjectedView();

		matrices.translate(-vec3d.x, -vec3d.y, -vec3d.z);

		IRenderTypeBuffer buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
		IVertexBuilder builder = buffer.getBuffer(LINES2);

		for (NodeBlockEntity terminalBlockEntity : nodes) {
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
		Minecraft.getInstance().getRenderTypeBuffers().getBufferSource().finish(LINES2);

		ItemStack stack1 = mc.player.getHeldItem(Hand.MAIN_HAND);
		if (stack1.getItem().isIn(ExampleMod.HIGHLIGHT)) {
			if (stack1.getTag() != null && stack1.getTag().contains("node_type")) {
				BlockPos pos = NBTUtil.readBlockPos(stack1.getTag());
				if (pos != null) {
					builder = buffer.getBuffer(getCubeType());
					renderCubeOutline(matrices,builder,pos,NodeType.valueOf(stack1.getTag().getString("node_type")).color);
					Minecraft.getInstance().getRenderTypeBuffers().getBufferSource().finish(getCubeType());
				}
			}
		}
	}

	public static void renderCubeOutline(MatrixStack matrices,IVertexBuilder builder,BlockPos pos,int color) {
		Matrix4f matrix4f = matrices.getLast().getMatrix();
		int alpha = 0x80000000;
		drawVerticalFace(builder,matrix4f,pos.getX(),1,pos.getY(),1,pos.getZ(),0,alpha | color);
		drawVerticalFace(builder,matrix4f,pos.getX(),1,pos.getY(),1,pos.getZ() + 1,0,alpha | color);

		drawVerticalFace(builder,matrix4f,pos.getX()+1,0,pos.getY(),1,pos.getZ() ,1,alpha | color);
		drawVerticalFace(builder,matrix4f,pos.getX(),0,pos.getY(),1,pos.getZ() ,1,alpha | color);

		drawHorizontalFace(builder,matrix4f,pos.getX(),1,pos.getY()+1, pos.getZ(),1,alpha | color);
		drawHorizontalFace(builder,matrix4f,pos.getX(),1,pos.getY(), pos.getZ() ,1,alpha | color);

	}

	public static void drawVerticalFace(IVertexBuilder builder, Matrix4f matrix4f, float u, float width, float v, float height, float z, float depth, int aarrggbb) {
		float a = (aarrggbb >> 24 & 0xff) / 255f;
		float r = (aarrggbb >> 16 & 0xff) / 255f;
		float g = (aarrggbb >> 8 & 0xff) / 255f;
		float b = (aarrggbb & 0xff) / 255f;

		drawVerticalFace(builder, matrix4f, u, width, v, height,z,depth, r, g, b, a);
	}

	public static void drawVerticalFace(IVertexBuilder builder, Matrix4f matrix4f, float x, float width, float y, float height, float z, float depth, float r, float g, float b, float a) {
		builder.pos(matrix4f, x, y, z).tex(0.0F, 0.0F).color(r, g, b, a).normal(Vector3f.YP.getX(), Vector3f.YP.getY(), Vector3f.YP.getZ()).endVertex();
		builder.pos(matrix4f, x, y + height, z).tex(0.0F, 0.5F).color(r, g, b, a).normal(Vector3f.YP.getX(), Vector3f.YP.getY(), Vector3f.YP.getZ()).endVertex();
		builder.pos(matrix4f, x + width, y + height, z + depth).tex(1.0F, 0.5F).color(r, g, b, a).normal(Vector3f.YP.getX(), Vector3f.YP.getY(), Vector3f.YP.getZ()).endVertex();
		builder.pos(matrix4f, x + width, y, z + depth).tex(1.0F, 0.0F).color(r, g, b, a).normal(Vector3f.YP.getX(), Vector3f.YP.getY(), Vector3f.YP.getZ()).endVertex();
	}

	public static void drawHorizontalFace(IVertexBuilder builder, Matrix4f matrix4f, float u, float width, float v, float z, float depth, int aarrggbb) {
		float a = (aarrggbb >> 24 & 0xff) / 255f;
		float r = (aarrggbb >> 16 & 0xff) / 255f;
		float g = (aarrggbb >> 8 & 0xff) / 255f;
		float b = (aarrggbb & 0xff) / 255f;

		drawHorizontalFace(builder, matrix4f, u, width, v, z,depth, r, g, b, a);
	}

	public static void drawHorizontalFace(IVertexBuilder builder, Matrix4f matrix4f, float x, float width, float y, float z, float depth, float r, float g, float b, float a) {
		builder.pos(matrix4f, x, y, z).tex(0.0F, 0.0F).color(r, g, b, a).normal(Vector3f.YP.getX(), Vector3f.YP.getY(), Vector3f.YP.getZ()).endVertex();
		builder.pos(matrix4f, x + width, y, z).tex(0.0F, 0.5F).color(r, g, b, a).normal(Vector3f.YP.getX(), Vector3f.YP.getY(), Vector3f.YP.getZ()).endVertex();
		builder.pos(matrix4f, x + width, y, z + depth).tex(1.0F, 0.0F).color(r, g, b, a).normal(Vector3f.YP.getX(), Vector3f.YP.getY(), Vector3f.YP.getZ()).endVertex();
		builder.pos(matrix4f, x, y, z + depth).tex(1.0F, 0.5F).color(r, g, b, a).normal(Vector3f.YP.getX(), Vector3f.YP.getY(), Vector3f.YP.getZ()).endVertex();
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
