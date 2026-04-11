package com.direwolf20.buildinggadgets.common.util.ref;

import net.minecraft.resources.Identifier;

public final class Reference {

    public static final String MODID = "buildinggadgets";

    public static final class ItemReference {
        public static final Identifier TAG_TEMPLATE_CONVERTIBLE = Identifier.fromNamespaceAndPath(MODID, "template_convertible");
    }

    public static final class TileDataSerializerReference {
        public static final Identifier REGISTRY_ID_TILE_DATA_SERIALIZER = Identifier.fromNamespaceAndPath(MODID, "tile_data/serializer");
        public static final Identifier DUMMY_SERIALIZER_RL = Identifier.fromNamespaceAndPath(MODID, "dummy_serializer");
        public static final Identifier NBT_TILE_ENTITY_DATA_SERIALIZER_RL = Identifier.fromNamespaceAndPath(MODID, "nbt_tile_data_serializer");
    }

    public static final class TagReference {
        public static final Identifier BLACKLIST_COPY_PASTE = Identifier.fromNamespaceAndPath(MODID, "blacklist/copy_paste");
        public static final Identifier BLACKLIST_BUILDING = Identifier.fromNamespaceAndPath(MODID, "blacklist/building");
        public static final Identifier BLACKLIST_EXCHANGING = Identifier.fromNamespaceAndPath(MODID, "blacklist/exchanging");
        public static final Identifier BLACKLIST_DESTRUCTION = Identifier.fromNamespaceAndPath(MODID, "blacklist/destruction");
        public static final Identifier WHITELIST_COPY_PASTE = Identifier.fromNamespaceAndPath(MODID, "whitelist/copy_paste");
        public static final Identifier WHITELIST_BUILDING = Identifier.fromNamespaceAndPath(MODID, "whitelist/building");
        public static final Identifier WHITELIST_EXCHANGING = Identifier.fromNamespaceAndPath(MODID, "whitelist/exchanging");
        public static final Identifier WHITELIST_DESTRUCTION = Identifier.fromNamespaceAndPath(MODID, "whitelist/destruction");
    }
}
