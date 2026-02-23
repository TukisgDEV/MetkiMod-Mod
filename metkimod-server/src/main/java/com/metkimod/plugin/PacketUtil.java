package com.metkimod.plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @author Tukisg
 */
public class PacketUtil {

    public static double readDouble(DataInputStream in) throws IOException {
        return in.readDouble();
    }

    public static int readInt(DataInputStream in) throws IOException {
        return in.readInt();
    }

    public static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;
        do {
            currentByte = in.readByte();
            value |= (currentByte & 0x7F) << position;
            position += 7;
            if (position >= 32)
                throw new IOException("VarInt is too big");
        } while ((currentByte & 0x80) != 0);
        return value;
    }

    public static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        if (length < 0 || length > 32767)
            throw new IOException("String too long: " + length);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeDouble(DataOutputStream out, double value) throws IOException {
        out.writeDouble(value);
    }

    public static void writeInt(DataOutputStream out, int value) throws IOException {
        out.writeInt(value);
    }

    public static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    public static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    public static PlaceMarkerData parsePlaceMarker(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        double x = readDouble(in);
        double y = readDouble(in);
        double z = readDouble(in);
        int color = readInt(in);
        int pingTypeOrdinal = readInt(in);
        return new PlaceMarkerData(x, y, z, color, pingTypeOrdinal);
    }

    public static byte[] buildSyncMarker(double x, double y, double z,
            int color, String ownerName, int pingTypeOrdinal) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        writeDouble(out, x);
        writeDouble(out, y);
        writeDouble(out, z);
        writeInt(out, color);
        writeString(out, ownerName);
        writeInt(out, pingTypeOrdinal);
        out.flush();
        return baos.toByteArray();
    }

    public record PlaceMarkerData(double x, double y, double z, int color, int pingTypeOrdinal) {
    }
}
