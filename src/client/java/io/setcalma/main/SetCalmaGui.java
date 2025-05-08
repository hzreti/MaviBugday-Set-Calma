package io.setcalma.main;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;
import java.util.ArrayList;

public class SetCalmaGui extends Screen {
    private TextFieldWidget nameField;
    private ButtonWidget addButton;
    private final List<String> targetPlayers;
    private final List<ClickableWidget> playerButtons = new ArrayList<>();

    public SetCalmaGui(List<String> targetPlayers) {
        super(Text.literal("SetCalma Listesi by hzreti"));
        this.targetPlayers = targetPlayers;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // İsim yazma kutusu (Her zaman boş olacak)
        this.nameField = new TextFieldWidget(this.textRenderer, centerX - 75, centerY - 40, 150, 20, Text.literal(""));
        this.addSelectableChild(this.nameField);

        // "+" Butonu (Ekleme butonu)
        this.addButton = ButtonWidget.builder(Text.literal("+"), button -> {
            String newName = nameField.getText().trim();
            if (!newName.isEmpty() && !targetPlayers.contains(newName)) {
                targetPlayers.add(newName);
                nameField.setText(""); // İsmi ekledikten sonra temizle
                SetCalmaMain.saveConfig(); // Listeyi kaydet
                updatePlayerList();
            }
        }).dimensions(centerX + 80, centerY - 40, 20, 20).build();

        this.addDrawableChild(addButton);
        updatePlayerList();
    }

    private void updatePlayerList() {
        // Önce eski butonları temizleyelim
        for (ClickableWidget widget : playerButtons) {
            this.remove(widget);
        }
        playerButtons.clear();

        int yOffset = 20;
        int centerX = this.width / 2;

        for (String playerName : targetPlayers) {
            int yPos = 60 + yOffset;

            // Oyuncu adını metin olarak göster
            ButtonWidget playerNameButton = ButtonWidget.builder(Text.literal(playerName), button -> {
                // You can add functionality here if needed, e.g., clicking a name does something.
            }).dimensions(centerX - 75, yPos, 150, 20).build();

            this.addDrawableChild(playerNameButton);
            playerButtons.add(playerNameButton);

            // "-" butonu (Silme işlemi için)
            ButtonWidget removeButton = ButtonWidget.builder(Text.literal("-"), button -> {
                targetPlayers.remove(playerName);
                SetCalmaMain.saveConfig();
                updatePlayerList(); // Listeyi güncelle
            }).dimensions(centerX + 80, yPos, 20, 20).build();

            this.addDrawableChild(removeButton);
            playerButtons.add(removeButton);

            yOffset += 25;
        }
    }

    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawContext.fillGradient(0, 0, this.width, this.height, 0xFF000000, 0xFF202020);
        drawContext.drawTextWithShadow(this.textRenderer, "SetCalma Listesi", this.width / 2 - 40, 20, 0xFFFFFF);
        nameField.render(drawContext, mouseX, mouseY, delta);
        super.render(drawContext, mouseX, mouseY, delta);
    }

    public void onClose() {
        this.client.setScreen(null);
    }
}
