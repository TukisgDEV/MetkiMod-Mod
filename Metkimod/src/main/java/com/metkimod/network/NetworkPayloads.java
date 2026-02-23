package com.metkimod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class NetworkPayloads {

        public record PlaceMarkerC2S(double x, double y, double z, int color, int pingTypeOrdinal) implements CustomPayload {

    public static final Id<PlaceMarkerC2S> ID = new Id<>(Identifier.of("metkimod", "place_marker"));

    public static final PacketCodec<RegistryByteBuf, PlaceMarkerC2S> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, PlaceMarkerC2S::x,
            PacketCodecs.DOUBLE, PlaceMarkerC2S::y,
            PacketCodecs.DOUBLE, PlaceMarkerC2S::z,
            PacketCodecs.INTEGER, PlaceMarkerC2S::color,
            PacketCodecs.INTEGER, PlaceMarkerC2S::pingTypeOrdinal,
            PlaceMarkerC2S::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    }

        public record SyncMarkerS2C(double x, double y, double z, int color,
                                String ownerName, int pingTypeOrdinal) implements CustomPayload {

    public static final Id<SyncMarkerS2C> ID = new Id<>(Identifier.of("metkimod", "sync_marker"));

    public static final PacketCodec<RegistryByteBuf, SyncMarkerS2C> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, SyncMarkerS2C::x,
            PacketCodecs.DOUBLE, SyncMarkerS2C::y,
            PacketCodecs.DOUBLE, SyncMarkerS2C::z,
            PacketCodecs.INTEGER, SyncMarkerS2C::color,
            PacketCodecs.STRING, SyncMarkerS2C::ownerName,
            PacketCodecs.INTEGER, SyncMarkerS2C::pingTypeOrdinal,
            SyncMarkerS2C::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}}
