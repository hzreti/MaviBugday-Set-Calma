package io.setcalma.main;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.Color;
import java.util.function.Predicate;
import net.minecraft.registry.Registries;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SetCalmaMain implements ClientModInitializer {
    private static boolean modEnabled = false;
    private static boolean setiAlmaEnabled = false;
    private static boolean hudVisible = true; // HUD başlangıçta açık
    private static KeyBinding hudToggleKey; // HUD açma/kapama tuşu
    public static List<String> TARGET_PLAYERS = new ArrayList<>();

    private static KeyBinding toggleKeyBinding;
    private static KeyBinding setiAlmaKeyBinding;
    private static KeyBinding openGuiKey;

    private static final File configFile = new File(MinecraftClient.getInstance().runDirectory, "autoquit_config.json");

    @Override
    public void onInitializeClient() {
        loadConfig();

        toggleKeyBinding = new KeyBinding("Ölme/Salma", GLFW.GLFW_KEY_K, "SetÇalma By hzreti");
        KeyBindingHelper.registerKeyBinding(toggleKeyBinding);

        setiAlmaKeyBinding = new KeyBinding("Çalma/Kaçırma", GLFW.GLFW_KEY_N, "SetÇalma By hzreti");
        KeyBindingHelper.registerKeyBinding(setiAlmaKeyBinding);

        openGuiKey = new KeyBinding("Set Listesi", GLFW.GLFW_KEY_H, "SetÇalma By hzreti");
        KeyBindingHelper.registerKeyBinding(openGuiKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKeyBinding.wasPressed()) {
                toggleMod(client);
            }
            if (setiAlmaKeyBinding.wasPressed()) {
                toggleSetiAlma(client);
            }
            if (openGuiKey.wasPressed()) {
                client.execute(() -> {
                    loadConfig();
                    client.setScreen(new SetCalmaGui(TARGET_PLAYERS));
                });
            }
            if (modEnabled && client.player != null) {
                checkNearbyPlayers(client);
            }
            if (setiAlmaEnabled && client.player != null) {
                checkForTargetItems(client);
            }
        });

        hudToggleKey = new KeyBinding("HUD Aç/Kapa", GLFW.GLFW_KEY_I, "SetÇalma By hzreti");
        KeyBindingHelper.registerKeyBinding(hudToggleKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (hudToggleKey.wasPressed()) {
                hudVisible = !hudVisible;
                client.player.sendMessage(Text.literal("HUD: " + (hudVisible ? "Açık" : "Kapalı")), false);
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!hudVisible) return; // Eğer HUD kapalıysa, çizimi atla

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            TextRenderer textRenderer = client.textRenderer;
            int screenWidth = client.getWindow().getScaledWidth();
            int xPos = screenWidth - 100;
            int yPos = 50;
            int colorActive = Color.GREEN.getRGB();
            int colorInactive = Color.RED.getRGB();

            drawContext.drawText(textRenderer, "Ölme: " + (modEnabled ? "Açık" : "Kapalı"), xPos, yPos, modEnabled ? colorActive : colorInactive, true);
            drawContext.drawText(textRenderer, "Set Çalma: " + (setiAlmaEnabled ? "Açık" : "Kapalı"), xPos, yPos + 12, setiAlmaEnabled ? colorActive : colorInactive, true);
        });
    }

    private void checkNearbyPlayers(MinecraftClient client) {
        try {
            Box playerBox = client.player.getBoundingBox().expand(2.0);
            Predicate<Entity> playerFilter = entity -> entity instanceof PlayerEntity && entity != client.player;

            client.player.getWorld().getEntitiesByClass(PlayerEntity.class, playerBox, playerFilter)
                    .forEach(playerEntity -> {
                        String playerName = playerEntity.getName().getString();
                        if (TARGET_PLAYERS.contains(playerName)) {
                            client.execute(() -> {
                                client.player.networkHandler.getConnection().disconnect(Text.literal("Ölme Başarılı ✓"));
                            });
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkForTargetItems(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        List<String> targetItems = Arrays.asList(
                "minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots",
                "minecraft:netherite_helmet", "minecraft:netherite_chestplate", "minecraft:netherite_leggings", "minecraft:netherite_boots",
                "minecraft:diamond_sword", "minecraft:netherite_sword"
        );

        for (ItemStack itemStack : client.player.getInventory().main) {
            String itemId = Registries.ITEM.getId(itemStack.getItem()).toString();

            if (targetItems.contains(itemId)) {
                triggerSpawnCommand(client);
                return;
            }
        }
    }

    private void triggerSpawnCommand(MinecraftClient client) {
        if (client.player == null) return;

        client.execute(() -> {
            client.player.networkHandler.sendCommand("spawn");
            client.player.sendMessage(Text.literal("Çalındı :)"), false);
            setiAlmaEnabled = false;
        });
    }

    public static void toggleMod(MinecraftClient client) {
        modEnabled = !modEnabled;
        client.player.sendMessage(Text.literal("Ölme Modu: " + (modEnabled ? "Açık" : "Kapalı")), false);
    }

    public static void toggleSetiAlma(MinecraftClient client) {
        setiAlmaEnabled = !setiAlmaEnabled;
        client.player.sendMessage(Text.literal("Set Çalma Modu: " + (setiAlmaEnabled ? "Açık" : "Kapalı")), false);
    }

    public static void loadConfig() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                Gson gson = new Gson();
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                JsonArray playersArray = json.getAsJsonArray("target_players");

                TARGET_PLAYERS.clear();
                for (int i = 0; i < playersArray.size(); i++) {
                    TARGET_PLAYERS.add(playersArray.get(i).getAsString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            Gson gson = new Gson();
            JsonObject json = new JsonObject();
            JsonArray playersArray = new JsonArray();

            for (String playerName : TARGET_PLAYERS) {
                playersArray.add(playerName);
            }

            json.add("target_players", playersArray);
            gson.toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
