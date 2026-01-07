package com.ua.ukrtranslator;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Screen extends net.minecraft.client.gui.screens.Screen {
    private static final ResourceLocation BANNER = new ResourceLocation("ukrtranslator", "textures/gui/muf.png");
    private final int linesCount;

    protected Screen(int linesCount) {
        super(Component.literal("Welcome"));
        this.linesCount = linesCount;
    }

    @Override
    protected void init() {
        int btnWidth = 100;
        this.addRenderableWidget(Button.builder(Component.literal("Закрити"), (btn) -> {
            Translation.disableWelcomeScreen();
            this.onClose();
        }).bounds(this.width / 2 - btnWidth / 2, this.height - 40, btnWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int centerX = this.width / 2;

        RenderSystem.setShaderTexture(0, BANNER);
        graphics.blit(BANNER, centerX - 40, 10, 0, 0, 80, 80, 80, 80);

        graphics.drawCenteredString(this.font, "§lУкраїнізатор для MUF", centerX, 95, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "Завантажено рядків: §e" + linesCount, centerX, 125, 0xFFFFFF);

        graphics.drawCenteredString(this.font, "§6Команди:", centerX, 145, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "§7/reloadua - оновити текст з файлів (.minecraft -> config -> Ukrainizer)", centerX, 157, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "§7/resetua - скинути до заводських", centerX, 169, 0xFFFFFF);

        graphics.drawCenteredString(this.font, "§cЦЕ ПОВІДОМЛЕННЯ БІЛЬШЕ НЕ З'ЯВИТЬСЯ", centerX, this.height - 65, 0xFF5555);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}