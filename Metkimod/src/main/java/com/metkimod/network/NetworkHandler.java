package com.metkimod.network;

import com.metkimod.MetkiModClient;
import com.metkimod.group.GroupManager;
import com.metkimod.marker.Marker;
import com.metkimod.marker.MarkerManager;
import com.metkimod.marker.PingType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

public class NetworkHandler {

    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(NetworkPayloads.PlaceMarkerC2S.ID, NetworkPayloads.PlaceMarkerC2S.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkPayloads.SyncMarkerS2C.ID, NetworkPayloads.SyncMarkerS2C.CODEC);
    }

    public static void registerServerHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(NetworkPayloads.PlaceMarkerC2S.ID, (payload, context) -> {
            ServerPlayerEntity sender = context.player();
            UUID senderId = sender.getUuid();
            String senderName = sender.getName().getString();
            int groupColor = GroupManager.getPlayerColor(senderId);

            List<ServerPlayerEntity> groupMembers = GroupManager.getGroupMembersExcluding(senderId, sender.getServer());

            context.server().execute(() -> {
                for (ServerPlayerEntity member : groupMembers) {
                    ServerPlayNetworking.send(member, new NetworkPayloads.SyncMarkerS2C(
                            payload.x(), payload.y(), payload.z(),
                            groupColor, senderName, payload.pingTypeOrdinal()));
                }
            });
        });
    }

    public static void registerClientHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkPayloads.SyncMarkerS2C.ID, (payload, context) -> {
            context.client().execute(() -> {
                PingType type = PingType.fromOrdinal(payload.pingTypeOrdinal());

                Marker marker = new Marker(
                        UUID.nameUUIDFromBytes(payload.ownerName().getBytes()),
                        payload.ownerName(),
                        payload.x(), payload.y(), payload.z(),
                        payload.color(),
                        10000,
                        type);
                MarkerManager.addMarker(marker);
                MetkiModClient.playMarkerReceivedSound();
            });
        });
    }
}
