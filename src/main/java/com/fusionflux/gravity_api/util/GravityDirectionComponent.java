package com.fusionflux.gravity_api.util;

import com.fusionflux.gravity_api.GravityChangerMod;
import com.fusionflux.gravity_api.RotationAnimation;
import com.fusionflux.gravity_api.api.GravityChangerAPI;
import com.fusionflux.gravity_api.mixin.AccessorEntity;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

@SuppressWarnings({"deprecation", "CommentedOutCode"})
public class GravityDirectionComponent implements GravityComponent, AutoSyncedComponent {
    Direction gravityDirection = Direction.DOWN;
    
    ArrayList<Gravity> gravityList = new ArrayList<>();
    
    private final Entity entity;
    
    public GravityDirectionComponent(Entity _entity) {
        entity = _entity;
    }

    public Optional<Direction> getGravityDirection(Identifier id){
        return gravityList.stream()
                .filter(g -> g.id().equals(id))
                .findFirst()
                .map(Gravity::direction);
    }

    public void onGravityChanged(Direction prevGravityDirection, boolean initialGravity) {
        Direction gravityDirection = getGravityDirection();
        
        entity.fallDistance = 0;
        entity.setBoundingBox(((AccessorEntity) entity).gravity$calculateBoundingBox());
        
        if (!initialGravity) {
            adjustEntityPosition(prevGravityDirection, gravityDirection);
        }
        
        // Keep world velocity when changing gravity
        if (!GravityChangerMod.config.worldVelocity) {
            if (entity.isLogicalSideForUpdatingMovement()) {
                entity.setVelocity(RotationUtil.vecPlayerToWorld(
                    RotationUtil.vecWorldToPlayer(entity.getVelocity(), prevGravityDirection), gravityDirection)
                );
            }
        }
    }
    
    // Adjust position to avoid suffocation in blocks when changing gravity
    private void adjustEntityPosition(Direction prevGravityDirection, Direction gravityDirection) {
        if (entity instanceof AreaEffectCloudEntity || entity instanceof PersistentProjectileEntity || entity instanceof EndCrystalEntity) {
            return;
        }
        
        Box entityBoundingBox = entity.getBoundingBox();
        
        // for example, if gravity changed from down to north, move up
        // if gravity changed from down to up, also move up
        Direction movingDirection = prevGravityDirection.getOpposite();
        
        Iterable<VoxelShape> collisions = entity.world.getCollisions(entity, entityBoundingBox);
        Box totalCollisionBox = null;
        for (VoxelShape collision : collisions) {
            if (!collision.isEmpty()) {
                Box boundingBox = collision.getBoundingBox();
                if (totalCollisionBox == null) {
                    totalCollisionBox = boundingBox;
                }
                else {
                    totalCollisionBox = totalCollisionBox.union(boundingBox);
                }
            }
        }
        
        if (totalCollisionBox != null) {
            entity.setPosition(entity.getPos().add(getPositionAdjustmentOffset(
                entityBoundingBox, totalCollisionBox, movingDirection
            )));
        }
    }
    
    private static Vec3d getPositionAdjustmentOffset(
        Box entityBoundingBox, Box nearbyCollisionUnion, Direction movingDirection
    ) {
        Direction.Axis axis = movingDirection.getAxis();
        double offset = 0;
        if (movingDirection.getDirection() == Direction.AxisDirection.POSITIVE) {
            double pushing = nearbyCollisionUnion.getMax(axis);
            double pushed = entityBoundingBox.getMin(axis);
            if (pushing > pushed) {
                offset = pushing - pushed;
            }
        }
        else {
            double pushing = nearbyCollisionUnion.getMin(axis);
            double pushed = entityBoundingBox.getMax(axis);
            if (pushing < pushed) {
                offset = pushed - pushing;
            }
        }
        
        return new Vec3d(movingDirection.getUnitVector()).multiply(offset);
    }
    
    @Override
    public Direction getGravityDirection() {
        if (canChangeGravity()) {
            return gravityDirection;
        }
        return Direction.DOWN;
    }
    
    private boolean canChangeGravity() {
        return EntityTags.canChangeGravity(entity);
    }

    @Override
    public void tick(){
        setGravity(new Gravity(GravityChangerMod.GRAVITY_SOURCE_DEFAULT, Direction.UP));
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            Direction vehicleGravity = GravityChangerAPI.getGravityDirection(vehicle);
            setGravity(new Gravity(GravityChangerMod.GRAVITY_SOURCE_VEHICLE, vehicleGravity));
        }
        updateGravity(false);
    }

    @Override
    public void changeDimension() {
        gravityList.removeIf(g -> !GravitySources.getSource(g.id()).keepOnChangeDimension);
        updateGravity(true);
    }

    @Override
    public void respawn(ServerPlayerEntity oldPlayer) {
        gravityList = GravityChangerAPI.getGravityComponent(oldPlayer).map(GravityComponent::getGravityList).orElse(new ArrayList<>());
        gravityList.removeIf(g -> !GravitySources.getSource(g.id()).keepOnRespawn);
        updateGravity(true);
    }

    @Override
    public ArrayList<Gravity> getGravityList() {
        return gravityList;
    }

    public void updateGravity(boolean initialGravity) {
        if (canChangeGravity()) {
            Direction newGravity = getHighestPriority().map(Gravity::direction).orElse(Direction.DOWN);
            Direction oldGravity = gravityDirection;
            
            if (newGravity != oldGravity) {
                if (entity.world.isClient && entity instanceof PlayerEntity player && player.isMainPlayer()) {
                    RotationAnimation.applyRotationAnimation(newGravity, oldGravity, GravityChangerMod.config.rotationTime);
                }
                gravityDirection = newGravity;
                onGravityChanged(oldGravity, initialGravity);
            }
            GravityChangerComponents.GRAVITY_MODIFIER.sync(entity);
        }
    }

    private Optional<Gravity> getHighestPriority(){
        return gravityList.stream()
                //Pair sources with their priorities
                .map(g -> new Pair<>(g, GravitySources.getSource(g.id()).priority))
                .max(Comparator.comparingInt(Pair::getRight))
                .map(Pair::getLeft);
    }
    
    @Override
    public void setGravity(Gravity gravity) {
        if (canChangeGravity()) {
            gravityList.removeIf(g -> g.id().equals(gravity.id()));
            if(gravity.direction() != null) {
                gravityList.add(gravity);
            }
            GravityChangerComponents.GRAVITY_MODIFIER.sync(entity);
        }
    }

    private static final String ID_KEY = "Id";
    private static final String DIRECTION_KEY = "Direction";
    private static final String GRAVITY_KEY = "GravityDirection";

    @Override
    public void readFromNbt(NbtCompound nbt) {
        gravityList.clear();
        if(nbt.contains(GRAVITY_KEY, NbtElement.LIST_TYPE)) {
            for (NbtElement nbtElement : nbt.getList(GRAVITY_KEY, NbtElement.COMPOUND_TYPE)) {
                if (nbtElement instanceof NbtCompound nbtCompound) {
                    if(nbtCompound.contains(ID_KEY, NbtType.STRING) && nbtCompound.contains(DIRECTION_KEY, NbtType.INT)) {
                        setGravity(new Gravity(
                            Identifier.tryParse(nbtCompound.getString(ID_KEY)),
                            Direction.byId(nbtCompound.getInt(DIRECTION_KEY))
                        ));
                    }
                }
            }
        }
        updateGravity(true);
    }
    
    @Override
    public void writeToNbt(@NotNull NbtCompound nbt) {
        NbtList nbtList = new NbtList();
        for(Gravity gravity : gravityList){
            NbtCompound nbtCompound = new NbtCompound();
            nbtCompound.putString(ID_KEY, gravity.id().toString());
            nbtCompound.putInt(DIRECTION_KEY, gravity.direction().getId());
            nbtList.add(nbtCompound);
        }
        nbt.put(GRAVITY_KEY, nbtList);
    }
}
