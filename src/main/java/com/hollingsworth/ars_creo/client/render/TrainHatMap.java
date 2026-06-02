package com.hollingsworth.ars_creo.client.render;

import com.hollingsworth.arsnouveau.setup.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrainHatMap {

    private static Map<EntityType<?>, TrainData> map = new ConcurrentHashMap<>();

    static {
        put(ModEntities.STARBUNCLE_TYPE.get(), "head", new Vector3f(0.56f), new Vector3f(0f, 0.1f, 0.22f));
        put(ModEntities.ENTITY_FAMILIAR_STARBUNCLE.get(), "head", new Vector3f(0.56f), new Vector3f(0f, 0.1f, 0.22f));
        put(ModEntities.AMETHYST_GOLEM.get(), "body", new Vector3f(1.4f), new Vector3f(0, 0.4f, 0));
        put(ModEntities.ENTITY_DRYGMY.get(), "head", new Vector3f(0.76f), new Vector3f(0f, 0.4f, 0.0f));
        put(ModEntities.ENTITY_FAMILIAR_DRYGMY.get(), "head", new Vector3f(0.76f), new Vector3f(0f, 0.4f, 0.0f));
        put(ModEntities.ALAKARKINOS_TYPE.get(), "hat", new Vector3f(1.42f), new Vector3f(0f, 0.6f, 0.060f));
        put(ModEntities.WHIRLISPRIG_TYPE.get(), "head", new Vector3f(0.7f), new Vector3f(0f, 0.3f, 0.02f));
        put(ModEntities.ENTITY_FAMILIAR_SYLPH.get(), "head", new Vector3f(0.7f), new Vector3f(0f, 0.3f, 0.02f));
        put(ModEntities.ENTITY_BOOKWYRM_TYPE.get(), "head", new Vector3f(0.55f), new Vector3f(0f, 0.20f, 0.3f));
        put(ModEntities.ENTITY_FAMILIAR_BOOKWYRM.get(), "head", new Vector3f(0.55f), new Vector3f(0f, 0.20f, 0.3f));
    }

    public static void put(EntityType<?> entityType, String bone, Vector3f hatScale, Vector3f hatTranslation) {
        map.put(entityType, new TrainData(bone, hatScale, hatTranslation));
    }

    public static TrainData get(EntityType<?> type) {
        return map.get(type);
    }

    public record TrainData(String bone, Vector3f hatScale, Vector3f hatTranslation) {
    }
}
