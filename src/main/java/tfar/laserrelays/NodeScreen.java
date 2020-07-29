package tfar.laserrelays;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.recipebook.GhostRecipe;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public class NodeScreen extends ContainerScreen<NodeContainer> {

	private static final ResourceLocation DISPENSER_GUI_TEXTURES = new ResourceLocation("textures/gui/container/dispenser.png");

	public NodeScreen(NodeContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
		super(screenContainer, inv, titleIn);
	}

	protected void init() {
		super.init();
		this.titleX = (this.xSize - this.font.func_238414_a_(this.title)) / 2;
		//addButton(new Button(1,1,1,1,new StringTextComponent("BlackList"),this::onPress));
	}

	private void onPress(Button button) {

	}

	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		this.func_230459_a_(matrixStack, mouseX, mouseY);
	}

	//should actually be called drawSlot
	public void moveItems(MatrixStack matrixStack, Slot slot) {

		if (slot instanceof FilterSlot) {
			ItemStack stack = slot.getStack();
			int i = slot.xPos;
			int j = slot.yPos;
			/*if (i == 0 && p_238922_5_) {
				AbstractGui.fill(p_238922_1_, j - 4, k - 4, j + 20, k + 20, 822018048);
			} else {
				AbstractGui.fill(p_238922_1_, j, k, j + 16, k + 16, 822018048);
			}*/

			ItemRenderer itemrenderer = this.itemRenderer;
			itemrenderer.func_239390_c_(stack, i, j);
			RenderSystem.depthFunc(516);
			AbstractGui.fill(matrixStack, i, j,  i+ 16, j + 16, 822083583);
			RenderSystem.depthFunc(515);
			if (i == 0) {
				itemrenderer.renderItemOverlays(this.font, stack, i, j);
			}
		} else {

			int i = slot.xPos;
			int j = slot.yPos;
			ItemStack itemstack = slot.getStack();
			boolean flag = false;
			boolean flag1 = slot == this.clickedSlot && !this.draggedStack.isEmpty() && !this.isRightMouseClick;
			ItemStack itemstack1 = this.minecraft.player.inventory.getItemStack();
			String s = null;
			if (slot == this.clickedSlot && !this.draggedStack.isEmpty() && this.isRightMouseClick && !itemstack.isEmpty()) {
				itemstack = itemstack.copy();
				itemstack.setCount(itemstack.getCount() / 2);
			} else if (this.dragSplitting && this.dragSplittingSlots.contains(slot) && !itemstack1.isEmpty()) {
				if (this.dragSplittingSlots.size() == 1) {
					return;
				}

				if (Container.canAddItemToSlot(slot, itemstack1, true) && this.container.canDragIntoSlot(slot)) {
					itemstack = itemstack1.copy();
					flag = true;
					Container.computeStackSize(this.dragSplittingSlots, this.dragSplittingLimit, itemstack, slot.getStack().isEmpty() ? 0 : slot.getStack().getCount());
					int k = Math.min(itemstack.getMaxStackSize(), slot.getItemStackLimit(itemstack));
					if (itemstack.getCount() > k) {
						s = TextFormatting.YELLOW.toString() + k;
						itemstack.setCount(k);
					}
				} else {
					this.dragSplittingSlots.remove(slot);
					this.updateDragSplitting();
				}
			}

			this.setBlitOffset(100);
			this.itemRenderer.zLevel = 100.0F;
			if (itemstack.isEmpty() && slot.isEnabled()) {
				Pair<ResourceLocation, ResourceLocation> pair = slot.getBackground();
				if (pair != null) {
					TextureAtlasSprite textureatlassprite = this.minecraft.getAtlasSpriteGetter(pair.getFirst()).apply(pair.getSecond());
					this.minecraft.getTextureManager().bindTexture(textureatlassprite.getAtlasTexture().getTextureLocation());
					blit(matrixStack, i, j, this.getBlitOffset(), 16, 16, textureatlassprite);
					flag1 = true;
				}
			}

			if (!flag1) {
				if (flag) {
					fill(matrixStack, i, j, i + 16, j + 16, -2130706433);
				}

				RenderSystem.enableDepthTest();
				this.itemRenderer.renderItemAndEffectIntoGUI(this.minecraft.player, itemstack, i, j);
				this.itemRenderer.renderItemOverlayIntoGUI(this.font, itemstack, i, j, s);
			}

			this.itemRenderer.zLevel = 0.0F;
			this.setBlitOffset(0);
		}
	}

	protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.minecraft.getTextureManager().bindTexture(DISPENSER_GUI_TEXTURES);
		int i = (this.width - this.xSize) / 2;
		int j = (this.height - this.ySize) / 2;
		this.blit(matrixStack, i, j, 0, 0, this.xSize, this.ySize);
	}
}
