package com.fusionflux.gravity_api.util;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class GravitySources {
    public static List<Source> sourceList = new ArrayList<>();

    public static void register(Identifier id, int priority, boolean keepOnRespawn, boolean keepOnChangeDimension, Source.ClientChangeVerifier clientChangeVerifier){
        sourceList.add(new Source(id, priority, keepOnRespawn, keepOnChangeDimension, clientChangeVerifier));
    }

    public static void register(Identifier id, int priority, boolean keepOnRespawn, boolean keepOnChangeDimension){
        register(id, priority, keepOnRespawn, keepOnChangeDimension, p -> false);
    }

    public static Source getSource(Identifier id){
        return sourceList.stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
    }

    public static class Source {
        public final Identifier id;
        public final int priority;
        public final boolean keepOnRespawn;
        public final boolean keepOnChangeDimension;
        public final ClientChangeVerifier ccv;

        Source(Identifier _id, int _priority, boolean _keepOnRespawn, boolean _keepOnChangeDimension, ClientChangeVerifier _ccv) {
            id = _id;
            priority = _priority;
            keepOnRespawn = _keepOnRespawn;
            keepOnChangeDimension = _keepOnChangeDimension;
            ccv = _ccv;
        }

        private interface ClientChangeVerifier {
            boolean f(PacketByteBuf buffer);
        }
    }
}
