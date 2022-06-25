package com.fusionflux.gravity_api.item;

import com.fusionflux.gravity_api.GravityChangerMod;
import com.fusionflux.gravity_api.api.GravityChangerAPI;
import com.fusionflux.gravity_api.util.Gravity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

public class GravityChangerItemAOE extends Item {
    public final Direction gravityDirection;

    public GravityChangerItemAOE(Settings settings, Direction gravityDirection) {
        super(settings);

        this.gravityDirection = gravityDirection;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        Box box = user.getBoundingBox().expand(3);
            List<Entity> list = world.getEntitiesByClass(Entity.class, box, e -> !(e instanceof PlayerEntity));
            for (Entity entity : list) {
                if (!(entity instanceof PlayerEntity))
                    GravityChangerAPI.setGravity(entity, new Gravity(GravityChangerMod.GRAVITY_SOURCE_TRANSIENT, gravityDirection));
            }
        return TypedActionResult.success(user.getStackInHand(hand));
    }



}
