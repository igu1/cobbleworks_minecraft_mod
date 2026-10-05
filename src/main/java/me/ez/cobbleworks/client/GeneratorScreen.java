package me.ez.cobbleworks.client;

import me.ez.cobbleworks.common.GeneratorMenu;
import me.ez.cobbleworks.common.GeneratorRules;
import me.ez.cobbleworks.common.MachinePhase;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Compact graphite/copper panel; animations visualize synchronized server progress only. */
public final class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
    private static final int INK = 0xff121a23, BORDER = 0xff3b4d5e, TEXT = 0xffecedf0;
    private static final int MUTED = 0xff8da4b5, WATER = 0xff42cbe7, LAVA = 0xffffa24c, COPPER = 0xffd1a575;
    private final long opened = System.nanoTime();
    private ControlButton power;

    public GeneratorScreen(GeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 206);
        inventoryLabelX = 8; inventoryLabelY = 112;
    }
    @Override protected void init() {
        super.init();
        power = addRenderableWidget(new ControlButton(8, 24, 64, 0));
        updateButtons();
    }
    @Override protected void containerTick() { super.containerTick(); updateButtons(); }
    private void updateButtons() {
        power.setMessage(Component.translatable("cobbleworks.menu.power." + (menu.enabled() ? "on" : "off")));
        power.setTooltip(Tooltip.create(Component.translatable("cobbleworks.menu.power_hint")));
    }
    private boolean running() { return menu.phase() == MachinePhase.RUNNING; }
    private double time() { return (System.nanoTime() - opened) / 1_000_000_000.0; }
    private float progress(float partial) {
        return Math.clamp((menu.progress() + (running() ? partial : 0)) / menu.duration(), 0, 1);
    }
    private int statusColor() {
        return switch (menu.phase()) {
            case RUNNING -> 0xff92d9ae; case FULL -> LAVA; case PAUSED -> MUTED; default -> WATER;
        };
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        super.extractBackground(g, mouseX, mouseY, partial);
        int x = leftPos, y = topPos;
        panel(g, x + 3, y + 4, imageWidth, imageHeight, 0x65000000);
        panel(g, x, y, imageWidth, imageHeight, BORDER);
        panel(g, x + 1, y + 1, imageWidth - 2, imageHeight - 2, INK);
        g.fillGradient(x + 2, y + 2, x + 174, y + 22, 0xff283744, 0xff1b2834);
        g.fill(x + 6, y, x + 88, y + 2, WATER);
        g.fill(x + 88, y, x + 170, y + 2, LAVA);
        for (int dx : new int[]{6, 168}) for (int dy : new int[]{6, 196}) {
            g.fill(x + dx, y + dy, x + dx + 3, y + dy + 3, COPPER);
            g.fill(x + dx + 1, y + dy + 1, x + dx + 2, y + dy + 2, INK);
        }
        // Two animated reservoir columns sit directly above their reusable-bucket slots.
        reservoir(g, x + 12, y + 46, 16, 30, menu.hasWater(), WATER, 0);
        reservoir(g, x + 44, y + 46, 16, 30, menu.hasLava(), LAVA, 2);
        slot(g, x + 12, y + 80, WATER);
        slot(g, x + 44, y + 80, LAVA);
        // Animated reaction chamber and the linked progress bar.
        g.item(new ItemStack(Items.COBBLESTONE), x + 84, y + 44);
        g.fill(x + 72, y + 63, x + 113, y + 70, 0xff070f17);
        int width = Math.round(progress(partial) * 38);
        if (width > 0) g.fillGradient(x + 73, y + 64, x + 73 + width, y + 69, COPPER, LAVA);
        if (running()) {
            int beam = (int) ((time() * 26) % 38);
            g.fill(x + 73 + beam, y + 64, x + 74 + beam, y + 69, 0x80ffffff);
        }
        // Compact 3x3 output buffer.
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) slot(g, x + 120 + col * 18, y + 44 + row * 18, BORDER);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) slot(g, x + 8 + col * 18, y + 122 + row * 18, BORDER);
        for (int col = 0; col < 9; col++) slot(g, x + 8 + col * 18, y + 180, 0xff536375);
        // Small top-right status indicator: solid pulse while running, steady otherwise.
        int status = statusColor();
        int sx = x + 160, sy = y + 8;
        int pulse = running() ? 90 + (int) (Math.sin(time() * 5) * 60) : 150;
        g.fill(sx - 1, sy - 1, sx + 9, sy + 9, 0xff0b121b);
        g.fill(sx, sy, sx + 8, sy + 8, 0xff000000 | ((status & 0xffffff) & 0xffffff));
        g.fill(sx, sy, sx + 8, sy + 8, (Math.clamp(pulse, 0, 255) << 24) | (status & 0xffffff));
        g.fill(sx, sy, sx + 8, sy + 1, 0x60ffffff);
        if (mouseX >= sx - 1 && mouseX < sx + 9 && mouseY >= sy - 1 && mouseY < sy + 9) {
            g.setTooltipForNextFrame(font, Component.translatable("cobbleworks.menu.status." + menu.phase().getSerializedName()), mouseX, mouseY);
        }
        if (mouseX >= x + 72 && mouseX < x + 113 && mouseY >= y + 63 && mouseY < y + 70) {
            g.setTooltipForNextFrame(font, Component.translatable("cobbleworks.menu.progress_hint", menu.progress(), menu.duration()), mouseX, mouseY);
        }
    }
    private void reservoir(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean filled, int accent, int offset) {
        panel(g, x, y, w, h, BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xff091522);
        if (filled) {
            int top = y + 6;
            for (int dx = 0; dx < w - 4; dx++) {
                int wave = (int) Math.round(Math.sin(dx * .55 + time() * 2 + offset) * 1.5);
                g.fill(x + 2 + dx, top + wave, x + 3 + dx, y + h - 2, 0xff000000 | (accent & 0xffffff));
                g.fill(x + 2 + dx, top + wave, x + 3 + dx, top + 2 + wave, 0xffc9e8e9);
            }
            for (int b = 0; b < 2; b++) {
                int by = y + h - 5 - (int) ((time() * (5 + b) + b * 9) % (h - 8));
                g.fill(x + 4 + b * 5, by, x + 5 + b * 5, by + 2, 0xffe2eeee);
            }
        }
        g.fill(x + 2, y + 2, x + 3, y + h - 3, 0x7079b6cb);
        for (int tick = 0; tick < 2; tick++) g.fill(x + w - 4, y + 6 + tick * 9, x + w - 1, y + 7 + tick * 9, 0xff344d61);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String heading = font.plainSubstrByWidth(title.getString(), 146);
        g.text(font, heading, 8, 7, TEXT, false);
        g.text(font, Component.translatable("cobbleworks.menu.water"), 10, 99, 0xffa8f4ff, true);
        g.text(font, Component.translatable("cobbleworks.menu.lava"), 42, 99, 0xffffd88a, true);
        g.text(font, Component.literal(menu.stored() + "/" + GeneratorRules.CAPACITY), 118, 99, MUTED, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }
    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 2, y, x + w - 2, y + h, color);
        g.fill(x, y + 2, x + w, y + h - 2, color);
    }
    private static void slot(GuiGraphicsExtractor g, int x, int y, int accent) {
        g.fill(x - 1, y - 1, x + 17, y + 17, accent);
        g.fill(x, y, x + 16, y + 16, 0xff090f17);
        g.fill(x, y + 15, x + 16, y + 16, 0xff273747);
    }
    private final class ControlButton extends Button {
        private final int action;
        private float hover;
        private long frame = System.nanoTime();
        ControlButton(int x, int y, int width, int action) {
            super(leftPos + x, topPos + y, width, 16, Component.empty(), button -> {
                if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
            }, DEFAULT_NARRATION);
            this.action = action;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
            long now = System.nanoTime();
            float step = Math.min(1, (now - frame) / 100_000_000f); frame = now;
            hover += ((isHoveredOrFocused() ? 1 : 0) - hover) * step;
            int x = getX(), y = getY(), w = getWidth();
            panel(g, x, y, w, 16, BORDER);
            int shade = 25 + (int) (hover * 20);
            panel(g, x + 1, y + 1, w - 2, 14, 0xff000000 | shade << 16 | (shade + 11) << 8 | (shade + 22));
            int accent = menu.enabled() ? WATER : MUTED;
            g.fill(x + 3, y + 6, x + 5, y + 10, accent);
            // Ellipsize translated labels so longer languages never spill into adjacent controls.
            String text = getMessage().getString();
            if (font.width(text) > w - 14) text = font.plainSubstrByWidth(text, w - 22) + "…";
            g.text(font, text, x + 8, y + 4, TEXT, false);
        }
    }
}
