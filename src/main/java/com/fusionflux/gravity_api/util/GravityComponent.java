package com.fusionflux.gravity_api.util;

import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Optional;

public interface GravityComponent extends Component {
    Direction getGravityDirection();

    void setGravity(Gravity gravity);

    Optional<Direction> getGravityDirection(Identifier id);

    void tick();

    void changeDimension();

    void respawn(ServerPlayerEntity oldPlayer);

    ArrayList<Gravity> getGravityList();
}
