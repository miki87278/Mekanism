package mekanism.client.gui;

import java.util.ArrayList;
import java.util.List;
import mekanism.api.TileNetworkList;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.button.GuiButtonDisableableImage;
import mekanism.client.gui.button.GuiButtonTranslation;
import mekanism.client.gui.element.GuiScrollList;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiSideConfigurationTab;
import mekanism.client.gui.element.tab.GuiTransporterConfigTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.Mekanism;
import mekanism.common.frequency.Frequency;
import mekanism.common.frequency.FrequencyManager;
import mekanism.common.inventory.container.tile.QuantumEntangloporterContainer;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.tile.TileEntityQuantumEntangloporter;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.text.OwnerDisplay;
import mekanism.common.util.text.TextComponentUtil;
import mekanism.common.util.text.Translation;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class GuiQuantumEntangloporter extends GuiMekanismTile<TileEntityQuantumEntangloporter, QuantumEntangloporterContainer> {

    private Button publicButton;
    private Button privateButton;
    private Button setButton;
    private Button deleteButton;
    private Button checkboxButton;
    private GuiScrollList scrollList;
    private TextFieldWidget frequencyField;
    private boolean privateMode;

    public GuiQuantumEntangloporter(QuantumEntangloporterContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        ResourceLocation resource = getGuiLocation();
        addGuiElement(scrollList = new GuiScrollList(this, resource, 28, 37, 120, 4));
        addGuiElement(new GuiSideConfigurationTab(this, tileEntity, resource));
        addGuiElement(new GuiTransporterConfigTab(this, 34, tileEntity, resource));
        addGuiElement(new GuiUpgradeTab(this, tileEntity, resource));
        addGuiElement(new GuiSecurityTab<>(this, tileEntity, resource));
        if (tileEntity.frequency != null) {
            privateMode = !tileEntity.frequency.publicFreq;
        }
        ySize += 64;
    }

    @Override
    public void init() {
        super.init();
        buttons.clear();
        buttons.add(publicButton = new GuiButtonTranslation(guiLeft + 27, guiTop + 14, 60, 20, "gui.public", onPress -> {
            privateMode = false;
            updateButtons();
        }));
        buttons.add(privateButton = new GuiButtonTranslation(guiLeft + 89, guiTop + 14, 60, 20, "gui.private", onPress -> {
            privateMode = true;
            updateButtons();
        }));
        buttons.add(setButton = new GuiButtonTranslation(guiLeft + 27, guiTop + 116, 60, 20, "gui.set", onPress -> {
            int selection = scrollList.getSelection();
            if (selection != -1) {
                Frequency freq = privateMode ? tileEntity.privateCache.get(selection) : tileEntity.publicCache.get(selection);
                setFrequency(freq.name);
            }
            updateButtons();
        }));
        buttons.add(deleteButton = new GuiButtonTranslation(guiLeft + 89, guiTop + 116, 60, 20, "gui.delete", onPress -> {
            int selection = scrollList.getSelection();
            if (selection != -1) {
                Frequency freq = privateMode ? tileEntity.privateCache.get(selection) : tileEntity.publicCache.get(selection);
                TileNetworkList data = TileNetworkList.withContents(1, freq.name, freq.publicFreq);
                Mekanism.packetHandler.sendToServer(new PacketTileEntity(tileEntity, data));
                scrollList.clearSelection();
            }
            updateButtons();
        }));
        frequencyField = new TextFieldWidget(font, guiLeft + 50, guiTop + 104, 86, 11, "");
        frequencyField.setMaxStringLength(FrequencyManager.MAX_FREQ_LENGTH);
        frequencyField.setEnableBackgroundDrawing(false);
        buttons.add(checkboxButton = new GuiButtonDisableableImage(guiLeft + 137, guiTop + 103, 11, 11, xSize, 11, -11, getGuiLocation(),
              onPress -> {
                  setFrequency(frequencyField.getText());
                  frequencyField.setText("");
                  updateButtons();
              }));
        updateButtons();
    }

    public void setFrequency(String freq) {
        if (freq.isEmpty()) {
            return;
        }
        TileNetworkList data = TileNetworkList.withContents(0, freq, !privateMode);
        Mekanism.packetHandler.sendToServer(new PacketTileEntity(tileEntity, data));
    }

    public ITextComponent getSecurity(Frequency freq) {
        if (!freq.publicFreq) {
            return TextComponentUtil.build(EnumColor.DARK_RED, Translation.of("gui.private"));
        }
        return TextComponentUtil.translate("gui.public");
    }

    public void updateButtons() {
        if (tileEntity.getSecurity().getClientOwner() == null) {
            return;
        }
        List<String> text = new ArrayList<>();
        if (privateMode) {
            for (Frequency freq : tileEntity.privateCache) {
                text.add(freq.name);
            }
        } else {
            for (Frequency freq : tileEntity.publicCache) {
                text.add(freq.name + " (" + freq.clientOwner + ")");
            }
        }
        scrollList.setText(text);
        if (privateMode) {
            publicButton.active = true;
            privateButton.active = false;
        } else {
            publicButton.active = false;
            privateButton.active = true;
        }
        if (scrollList.hasSelection()) {
            Frequency freq = privateMode ? tileEntity.privateCache.get(scrollList.getSelection()) : tileEntity.publicCache.get(scrollList.getSelection());
            setButton.active = tileEntity.getFrequency(null) == null || !tileEntity.getFrequency(null).equals(freq);
            deleteButton.active = tileEntity.getSecurity().getOwnerUUID().equals(freq.ownerUUID);
        } else {
            setButton.active = false;
            deleteButton.active = false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateButtons();
        frequencyField.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        updateButtons();
        frequencyField.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    @Override
    protected ResourceLocation getGuiLocation() {
        return MekanismUtils.getResource(ResourceType.GUI, "teleporter.png");
    }

    @Override
    public boolean charTyped(char c, int i) {
        boolean returnValue = false;
        if (!frequencyField.isFocused() || i == GLFW.GLFW_KEY_ESCAPE) {
            returnValue = super.charTyped(c, i);
        } else if (i == GLFW.GLFW_KEY_ENTER && frequencyField.isFocused()) {
            setFrequency(frequencyField.getText());
            frequencyField.setText("");
            returnValue = true;
        } else if (Character.isDigit(c) || Character.isLetter(c) || isTextboxKey(c, i) || FrequencyManager.SPECIAL_CHARS.contains(c)) {
            returnValue = frequencyField.charTyped(c, i);
        }
        updateButtons();
        return returnValue;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawString(tileEntity.getName(), (xSize / 2) - (getStringWidth(tileEntity.getName()) / 2), 4, 0x404040);
        drawString(OwnerDisplay.of(tileEntity.getSecurity().getOwnerUUID(), tileEntity.getSecurity().getClientOwner()).getTextComponent(), 8, (ySize - 96) + 4, 0x404040);
        ITextComponent frequencyComponent = TextComponentUtil.build(Translation.of("gui.freq"), ": ");
        drawString(frequencyComponent, 32, 81, 0x404040);
        ITextComponent securityComponent = TextComponentUtil.build(Translation.of("gui.security"), ": ");
        drawString(securityComponent, 32, 91, 0x404040);
        Frequency frequency = tileEntity.getFrequency(null);
        if (frequency != null) {
            drawString(frequency.name, 32 + getStringWidth(frequencyComponent), 81, 0x797979);
            drawString(getSecurity(frequency), 32 + getStringWidth(securityComponent), 91, 0x797979);
        } else {
            drawString(TextComponentUtil.build(EnumColor.DARK_RED, Translation.of("gui.none")), 32 + getStringWidth(frequencyComponent), 81, 0x797979);
            drawString(TextComponentUtil.build(EnumColor.DARK_RED, Translation.of("gui.none")), 32 + getStringWidth(securityComponent), 91, 0x797979);
        }
        renderScaledText(TextComponentUtil.build(Translation.of("gui.set"), ":"), 27, 104, 0x404040, 20);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        super.drawGuiContainerBackgroundLayer(xAxis, yAxis);
        //TODO: Figure out what the parameters do
        frequencyField.renderButton(0, 0, 0);
        MekanismRenderer.resetColor();
    }
}