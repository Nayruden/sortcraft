package net.sortcraft.audit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Provides a shared Gson instance configured for audit logging.
 * Includes custom TypeAdapters for Minecraft types and Java time types.
 */
public final class AuditGsonHelper {
    private AuditGsonHelper() {}

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * Gson instance configured for audit logging (compact output).
     */
    public static final Gson GSON = createGson(false);

    /**
     * Gson instance configured for audit logging (pretty printed).
     */
    public static final Gson GSON_PRETTY = createGson(true);

    private static Gson createGson(boolean prettyPrint) {
        GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapter(BlockPos.class, new BlockPosAdapter())
                .registerTypeAdapter(Instant.class, new InstantAdapter());
        // Note: We do NOT use serializeNulls() so that null fields (like metadata for plain items) are omitted

        if (prettyPrint) {
            builder.setPrettyPrinting();
        }

        return builder.create();
    }

    /**
     * TypeAdapter for Minecraft BlockPos.
     * Serializes as {"x": 0, "y": 64, "z": 0}
     */
    private static class BlockPosAdapter extends TypeAdapter<BlockPos> {
        @Override
        public void write(JsonWriter out, BlockPos pos) throws IOException {
            if (pos == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            out.name("x").value(pos.getX());
            out.name("y").value(pos.getY());
            out.name("z").value(pos.getZ());
            out.endObject();
        }

        @Override
        public BlockPos read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            int x = 0, y = 0, z = 0;
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                switch (name) {
                    case "x" -> x = in.nextInt();
                    case "y" -> y = in.nextInt();
                    case "z" -> z = in.nextInt();
                    default -> in.skipValue();
                }
            }
            in.endObject();
            return new BlockPos(x, y, z);
        }
    }

    /**
     * TypeAdapter for Java Instant.
     * Serializes as ISO-8601 string (e.g., "2024-01-15T10:30:00Z")
     */
    private static class InstantAdapter extends TypeAdapter<Instant> {
        @Override
        public void write(JsonWriter out, Instant instant) throws IOException {
            if (instant == null) {
                out.nullValue();
                return;
            }
            out.value(ISO_FORMATTER.format(instant));
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return Instant.parse(in.nextString());
        }
    }
}

