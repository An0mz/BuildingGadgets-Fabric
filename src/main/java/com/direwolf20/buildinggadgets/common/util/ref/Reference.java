package com.direwolf20.buildinggadgets.common.util.ref;

import net.minecraft.resources.ResourceLocation;

public final class Reference {

    public static final String MODID = "buildinggadgets";

    public static final class ItemReference {
        public static final ResourceLocation TAG_TEMPLATE_CONVERTIBLE = ResourceLocation.fromNamespaceAndPath(MODID, "template_convertible");
    }

    public static final class TileDataSerializerReference {
        public static final ResourceLocation REGISTRY_ID_TILE_DATA_SERIALIZER = ResourceLocation.fromNamespaceAndPath(MODID, "tile_data/serializer");
        public static final ResourceLocation DUMMY_SERIALIZER_RL = ResourceLocation.fromNamespaceAndPath(MODID, "dummy_serializer");
        public static final ResourceLocation NBT_TILE_ENTITY_DATA_SERIALIZER_RL = ResourceLocation.fromNamespaceAndPath(MODID, "nbt_tile_data_serializer");
    }

    public static final class TagReference {
        public static final ResourceLocation BLACKLIST_COPY_PASTE = ResourceLocation.fromNamespaceAndPath(MODID, "blacklist/copy_paste");
        public static final ResourceLocation BLACKLIST_BUILDING = ResourceLocation.fromNamespaceAndPath(MODID, "blacklist/building");
        public static final ResourceLocation BLACKLIST_EXCHANGING = ResourceLocation.fromNamespaceAndPath(MODID, "blacklist/exchanging");
        public static final ResourceLocation BLACKLIST_DESTRUCTION = ResourceLocation.fromNamespaceAndPath(MODID, "blacklist/destruction");
        public static final ResourceLocation WHITELIST_COPY_PASTE = ResourceLocation.fromNamespaceAndPath(MODID, "whitelist/copy_paste");
        public static final ResourceLocation WHITELIST_BUILDING = ResourceLocation.fromNamespaceAndPath(MODID, "whitelist/building");
        public static final ResourceLocation WHITELIST_EXCHANGING = ResourceLocation.fromNamespaceAndPath(MODID, "whitelist/exchanging");
        public static final ResourceLocation WHITELIST_DESTRUCTION = ResourceLocation.fromNamespaceAndPath(MODID, "whitelist/destruction");
    }
}
