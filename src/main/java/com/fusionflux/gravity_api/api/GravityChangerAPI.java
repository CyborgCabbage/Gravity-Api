package com.fusionflux.gravity_api.api;

import java.util.ArrayList;
import java.util.Optional;

import com.fusionflux.gravity_api.util.RotationUtil;
import com.fusionflux.gravity_api.util.EntityTags;
import com.fusionflux.gravity_api.util.Gravity;
import org.jetbrains.annotations.Nullable;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import com.fusionflux.gravity_api.accessor.EntityAccessor;
import com.fusionflux.gravity_api.util.GravityComponent;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public abstract class GravityChangerAPI {
    public static final ComponentKey<GravityComponent> GRAVITY_COMPONENT =
            ComponentRegistry.getOrCreate(new Identifier("gravityapi", "gravity_direction"), GravityComponent.class);
    /**
     * Returns the applied gravity direction for the given player
     * This is the direction that directly affects everything this mod changes
     * If the player is riding a vehicle this will be the applied gravity direction of the vehicle
     * Otherwise it will be the main gravity gravity direction of the player itself
     */
    public static Direction getAppliedGravityDirection(Entity entity) {
        return ((EntityAccessor) entity).gravitychanger$getAppliedGravityDirection();
    }
    
    // workaround for a CCA bug; maybeGet throws an NPE in internal code if the DataTracker isn't initialized
    // null check the component container to avoid it
    private static <C extends Component, V> Optional<C> maybeGetSafe(ComponentKey<C> key, @Nullable V provider) {
        if (provider instanceof ComponentProvider p) {
            var cc = p.getComponentContainer();
            if (cc != null) {
                return Optional.ofNullable(key.getInternal(cc));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the main gravity direction for the given player
     * This may not be the applied gravity direction for the player, see GravityChangerAPI#getAppliedGravityDirection
     */
    public static Direction getGravityDirection(Entity entity) {
        if (!entity.getType().getRegistryEntry().isIn(EntityTags.FORBIDDEN_ENTITIES)) {
            return maybeGetSafe(GRAVITY_COMPONENT, entity).map(GravityComponent::getTrackedGravityDirection).orElse(Direction.DOWN);
        }
        return Direction.DOWN;
    }

    public static ArrayList<Gravity> getGravityList(Entity entity) {
        if (!entity.getType().getRegistryEntry().isIn(EntityTags.FORBIDDEN_ENTITIES)) {
            return maybeGetSafe(GRAVITY_COMPONENT, entity).map(GravityComponent::getGravity).orElse(new ArrayList<Gravity>());
        }
        return new ArrayList<Gravity>();
    }

    public static Direction getPrevGravtityDirection(Entity entity) {
        if (!entity.getType().getRegistryEntry().isIn(EntityTags.FORBIDDEN_ENTITIES)) {
            return maybeGetSafe(GRAVITY_COMPONENT, entity).map(GravityComponent::getPrevTrackedGravityDirection).orElse(Direction.DOWN);
        }
        return Direction.DOWN;
    }

    public static Direction getDefaultGravityDirection(Entity entity) {
        if (!entity.getType().getRegistryEntry().isIn(EntityTags.FORBIDDEN_ENTITIES)) {
            return maybeGetSafe(GRAVITY_COMPONENT, entity).map(GravityComponent::getDefaultTrackedGravityDirection).orElse(Direction.DOWN);
        }
        return Direction.DOWN;
        }

    public static boolean getIsInverted(Entity entity) {
        if (!entity.getType().getRegistryEntry().isIn(EntityTags.FORBIDDEN_ENTITIES)) {
            return maybeGetSafe(GRAVITY_COMPONENT, entity).map(GravityComponent::getInvertGravity).orElse(false);
        }
        return false;
    }

    /**
     * Sets the main gravity direction for the given player
     * If the player is a ServerPlayerEntity and gravity direction changed also syncs the direction to the clients
     * If the player is either a ServerPlayerEntity or a ClientPlayerEntity also slightly adjusts player position
     * This may not immediately change the applied gravity direction for the player, see GravityChangerAPI#getAppliedGravityDirection
     */
    public static void addGravity(Entity entity, Gravity gravity) {
        if (!entity.getType().getRegistryEntry().isIn(EntityTags.FORBIDDEN_ENTITIES)) {
            maybeGetSafe(GRAVITY_COMPONENT, entity).ifPresent(gc -> gc.addGravity(gravity,false));
        }
    }
    public static void updateGravity(Entity entity) {
        if (!entity.getType().getRegistryEntry().isIn(EntityTags.FORBIDDEN_ENTITIES)) {
            maybeGetSafe(GRAVITY_COMPONENT, entity).ifPresent(gc -> gc.updateGravity(false));
        }
    }

    public static void setGravity(Entity entity, ArrayList<Gravity> gravity) {
        if (!entity.getType().getRegistryEntry().isIn(EntityTags.FORBIDDEN_ENTITIES)) {
            maybeGetSafe(GRAVITY_COMPONENT, entity).ifPresent(gc -> gc.setGravity(gravity,false));
        }
    }

    public static void setIsInverted(Entity entity, boolean isInverted) {
        if (!entity.getType().getRegistryEntry().isIn(EntityTags.FORBIDDEN_ENTITIES)) {
            maybeGetSafe(GRAVITY_COMPONENT, entity).ifPresent(gc -> gc.invertGravity(isInverted));
        }
    }

    public static void clearGravity(Entity entity) {
        if (!entity.getType().getRegistryEntry().isIn(EntityTags.FORBIDDEN_ENTITIES)) {
            maybeGetSafe(GRAVITY_COMPONENT, entity).ifPresent(GravityComponent::clearGravity);
        }
    }

    public static void setDefaultGravityDirection(Entity entity, Direction gravityDirection) {
        if (!entity.getType().getRegistryEntry().isIn(EntityTags.FORBIDDEN_ENTITIES)) {
            maybeGetSafe(GRAVITY_COMPONENT, entity).ifPresent(gc -> gc.setDefaultTrackedGravityDirection(gravityDirection));
        }
    }

    //public static void resetGravity(Entity entity) {
    //    if (!entity.getType().getRegistryEntry().isIn(EntityTags.FORBIDDEN_ENTITIES)) {
    //        maybeGetSafe(GRAVITY_COMPONENT, entity).ifPresent(GravityComponent::resetGravity);
    //    }
    //}

    /**
     * Returns the world relative velocity for the given player
     * Using minecraft's methods to get the velocity of a the player will return player relative velocity
     */
    public static Vec3d getWorldVelocity(Entity playerEntity) {
        return RotationUtil.vecPlayerToWorld(playerEntity.getVelocity(), ((EntityAccessor) playerEntity).gravitychanger$getAppliedGravityDirection());
    }

    /**
     * Sets the world relative velocity for the given player
     * Using minecraft's methods to set the velocity of a the player will set player relative velocity
     */
    public static void setWorldVelocity(Entity entity, Vec3d worldVelocity) {
        entity.setVelocity(RotationUtil.vecWorldToPlayer(worldVelocity, ((EntityAccessor) entity).gravitychanger$getAppliedGravityDirection()));
    }

    /**
     * Returns eye position offset from feet position for the given player
     */
    public static Vec3d getEyeOffset(Entity entity) {
        return RotationUtil.vecPlayerToWorld(0, (double) entity.getStandingEyeHeight(), 0, ((EntityAccessor) entity).gravitychanger$getAppliedGravityDirection());
    }
}
