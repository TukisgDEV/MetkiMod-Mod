package com.metkimod;

import com.metkimod.gui.GroupScreen;
import com.metkimod.gui.PingWheelScreen;
import com.metkimod.marker.Marker;
import com.metkimod.marker.MarkerManager;
import com.metkimod.marker.PingType;
import com.metkimod.network.NetworkHandler;
import com.metkimod.network.NetworkPayloads;
import com.metkimod.render.MarkerRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class MetkiModClient implements ClientModInitializer {

    public static KeyBinding placeMarkerKey;
    public static KeyBinding openGroupMenuKey;
    public static int soloColor = 0x44AAFF;
    public static int soloColorIndex = 1;
    private static final double MARKER_REACH = 200.0;
    private static final int MARKER_LIFETIME_MS = 10000;
    private static int markerKeyHoldTicks = 0;

    @Override
    public void onInitializeClient() {
        MetkiMod.LOGGER.info("[MetkiMod] Initializing client...");

        placeMarkerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.metkimod.place_marker",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.metkimod"));

        openGroupMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.metkimod.open_group_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.metkimod"));

        NetworkHandler.registerClientHandlers();
        MarkerRenderer.register();
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        MetkiMod.LOGGER.info("[MetkiMod] Client ready! [V] = Ping, [Hold V] = Ping Wheel, [G] = Groups");
    }

    private void onClientTick(MinecraftClient client) {
        MarkerManager.tick();

        if (client.player == null || client.world == null)
            return;
        while (openGroupMenuKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new GroupScreen());
            }
        }
        if (client.currentScreen != null) {
            markerKeyHoldTicks = 0;
            while (placeMarkerKey.wasPressed()) {
            }
            return;
        }
        if (placeMarkerKey.isPressed()) {
            markerKeyHoldTicks++;
            if (markerKeyHoldTicks >= 5) { 
                client.setScreen(new PingWheelScreen());
                markerKeyHoldTicks = 0; 
            }
        } else {
            if (markerKeyHoldTicks > 0 && markerKeyHoldTicks < 5) {
                placeMarkerWithType(PingType.DEFAULT);
            }
            markerKeyHoldTicks = 0;
        }
        while (placeMarkerKey.wasPressed()) {
        }
    }

        public static void placeMarkerWithType(PingType type) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return;
        HitResult hitResult = client.player.raycast(MARKER_REACH, 0, false);
        double markerX, markerY, markerZ;

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            markerX = blockHit.getBlockPos().getX();
            markerY = blockHit.getBlockPos().getY() + 1;
            markerZ = blockHit.getBlockPos().getZ();
        } else {
            var lookVec = client.player.getRotationVec(1.0f);
            var eyePos = client.player.getEyePos();
            markerX = eyePos.x + lookVec.x * MARKER_REACH;
            markerY = eyePos.y + lookVec.y * MARKER_REACH;
            markerZ = eyePos.z + lookVec.z * MARKER_REACH;
        }
        int color = (type == PingType.DEFAULT) ? soloColor : type.getDefaultColor();

        Marker marker = new Marker(
                client.player.getUuid(),
                client.player.getName().getString(),
                markerX, markerY, markerZ,
                color,
                MARKER_LIFETIME_MS,
                type);
        MarkerManager.addMarker(marker);
        if (type == PingType.ENEMY || type == PingType.DANGER || type == PingType.FULL_STACK) {
            client.world.playSound(client.player, client.player.getBlockPos(),
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    SoundCategory.PLAYERS, 0.8f, 0.8f);
        } else if (type == PingType.HELP || type == PingType.CHASING) {
            client.world.playSound(client.player, client.player.getBlockPos(),
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    SoundCategory.PLAYERS, 0.9f, 1.2f);
        } else {
            client.world.playSound(client.player, client.player.getBlockPos(),
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    SoundCategory.PLAYERS, 0.6f, 1.8f);
        }
        if (ClientPlayNetworking.canSend(NetworkPayloads.PlaceMarkerC2S.ID)) {
            ClientPlayNetworking.send(new NetworkPayloads.PlaceMarkerC2S(
                    markerX, markerY, markerZ, color, type.ordinal()));
        }
        String msg = type == PingType.DEFAULT
                ? "📍 Метка!"
                : "📍 " + type.getMessage();
        double distance = marker.distanceTo(client.player.getX(), client.player.getY(), client.player.getZ());
        client.player.sendMessage(
                Text.literal(msg + " ")
                        .formatted(Formatting.GREEN)
                        .append(Text.literal(String.format("(%.0f блоков)", distance))
                                .formatted(Formatting.GRAY)),
                true);
    }

        public static void playMarkerReceivedSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            client.world.playSound(client.player, client.player.getBlockPos(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                    SoundCategory.PLAYERS, 0.7f, 1.4f);
        }
    }

        public static void cycleSoloColor() {
        soloColorIndex = (soloColorIndex + 1) % MetkiMod.PRESET_COLORS.length;
        soloColor = MetkiMod.PRESET_COLORS[soloColorIndex];
    }

        public static void setSoloColor(int index) {
        soloColorIndex = index % MetkiMod.PRESET_COLORS.length;
        soloColor = MetkiMod.PRESET_COLORS[soloColorIndex];
    }
}
