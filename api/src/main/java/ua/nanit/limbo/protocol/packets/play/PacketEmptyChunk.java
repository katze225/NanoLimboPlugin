package ua.nanit.limbo.protocol.packets.play;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import ua.nanit.limbo.protocol.ByteMessage;
import ua.nanit.limbo.protocol.PacketOut;
import ua.nanit.limbo.protocol.registry.Version;

import java.util.BitSet;

public class PacketEmptyChunk implements PacketOut {

    private int x;
    private int z;
    private int sections = 24;

    public void setX(int x) {
        this.x = x;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public void setSections(int sections) {
        this.sections = sections;
    }

    @Override
    public void encode(ByteMessage msg, Version version) {
        msg.writeInt(x);
        msg.writeInt(z);

        writeHeightmaps(msg, version);

        byte[] sectionData = createEmptySection(version);
        msg.writeVarInt(sectionData.length * sections);
        for (int i = 0; i < sections; i++) {
            msg.writeBytes(sectionData);
        }

        // Skip block entities
        msg.writeVarInt(0);

        writeLightData(msg);
    }

    private static void writeHeightmaps(ByteMessage msg, Version version) {
        if (version.moreOrEqual(Version.V1_21_5)) {
            msg.writeVarInt(1); // Array length
            msg.writeVarInt(4); // Motionblock type
            long[] motionBlocking = new long[37];
            msg.writeVarInt(motionBlocking.length);
            for (long data : motionBlocking) {
                msg.writeLong(data);
            }
            return;
        }

        LongArrayBinaryTag longArrayTag = LongArrayBinaryTag.longArrayBinaryTag(new long[37]);
        CompoundBinaryTag tag = CompoundBinaryTag.builder()
                .put("MOTION_BLOCKING", longArrayTag).build();
        CompoundBinaryTag rootTag = CompoundBinaryTag.builder()
                .put("root", tag).build();
        msg.writeCompoundTag(rootTag, version);
    }

    private static byte[] createEmptySection(Version version) {
        if (version.moreOrEqual(Version.V26_1)) {
            // Block count, fluid count, single block palette, single biome palette
            return new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        } else if (version.moreOrEqual(Version.V1_21_5)) {
            // Block count, single block palette, single biome palette (data array length is omitted since 1.21.5)
            return new byte[]{0, 0, 0, 0, 0, 0};
        } else {
            // Block count, single block palette, single biome palette (with data array lengths)
            return new byte[]{0, 0, 0, 0, 0, 0, 1, 0};
        }
    }

    private void writeLightData(ByteMessage msg) {
        msg.writeBitSet(null); // Sky y mask
        msg.writeBitSet(null); // Block y mask
        msg.writeBitSet(null); // Empty sky y mask

        BitSet emptyBlocksYMask = new BitSet();
        for (int i = 0; i < sections + 2; i++) {
            emptyBlocksYMask.set(i, true);
        }
        msg.writeBitSet(emptyBlocksYMask);

        // Skip sky updates
        msg.writeVarInt(0);
        // Skip block updates
        msg.writeVarInt(0);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
