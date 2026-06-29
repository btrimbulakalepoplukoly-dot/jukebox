package net.chaosjukebox.client.gui;

import net.chaosjukebox.network.JukeboxPackets.SetUrlPayload;
import net.chaosjukebox.network.JukeboxPackets.StopRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Вікно в стилі ванільного Minecraft: одне текстове поле для лінку + кнопки
 * "Грати" / "Стоп" / "Закрити". Відкривається ЛИШЕ на клієнті (StreamJukeboxBlock.onUse),
 * сам нікуди нічого не качає - просто відправляє лінк на сервер пакетом SetUrlPayload.
 */
public class JukeboxScreen extends Screen {

    private final BlockPos targetPos;
    private TextFieldWidget urlField;

    public JukeboxScreen(BlockPos targetPos) {
        super(Text.translatable("gui.chaosjukebox.title"));
        this.targetPos = targetPos;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.urlField = new TextFieldWidget(
                this.textRenderer,
                centerX - 120, centerY - 30,
                240, 20,
                Text.translatable("gui.chaosjukebox.url_field")
        );
        this.urlField.setMaxLength(512);
        this.urlField.setPlaceholder(Text.translatable("gui.chaosjukebox.url_placeholder"));
        this.addSelectableChild(this.urlField);
        this.setInitialFocus(this.urlField);

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.chaosjukebox.play"),
                button -> {
                    String url = this.urlField.getText().trim();
                    if (!url.isEmpty()) {
                        ClientPlayNetworking.send(new SetUrlPayload(targetPos, url));
                        this.close();
                    }
                }
        ).dimensions(centerX - 120, centerY, 115, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.chaosjukebox.stop"),
                button -> {
                    ClientPlayNetworking.send(new StopRequestPayload(targetPos));
                    this.close();
                }
        ).dimensions(centerX + 5, centerY, 115, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(
                this.textRenderer, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
        this.urlField.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
