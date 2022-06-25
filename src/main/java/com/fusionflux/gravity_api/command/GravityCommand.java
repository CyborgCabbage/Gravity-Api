package com.fusionflux.gravity_api.command;

import com.fusionflux.gravity_api.GravityChangerMod;
import com.fusionflux.gravity_api.api.GravityChangerAPI;
import com.fusionflux.gravity_api.util.Gravity;
import com.fusionflux.gravity_api.util.RotationUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

import java.util.Collection;
import java.util.Collections;

public class GravityCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> literalSetDefault = literal("set");
        for (Direction direction : Direction.values())
            literalSetDefault.then(literal(direction.getName())
                    .executes(context -> executeSet(context.getSource(), direction, Collections.singleton(context.getSource().getPlayer())))
                    .then(argument("entities", EntityArgumentType.entities())
                    .executes(context -> executeSet(context.getSource(), direction, EntityArgumentType.getEntities(context, "entities")))));

        LiteralArgumentBuilder<ServerCommandSource> literalRotate = literal("rotate");
        for (FacingDirection facingDirection : FacingDirection.values())
            literalRotate.then(literal(facingDirection.getName())
                    .executes(context -> executeRotate(context.getSource(), facingDirection, Collections.singleton(context.getSource().getPlayer())))
                    .then(argument("entities", EntityArgumentType.entities())
                    .executes(context -> executeRotate(context.getSource(), facingDirection, EntityArgumentType.getEntities(context, "entities")))));

        dispatcher.register(literal("gravity").requires(source -> source.hasPermissionLevel(2))
                .then(literal("get")
                    .executes(context -> executeGet(context.getSource(), context.getSource().getPlayer()))
                    .then(argument("entities", EntityArgumentType.entity())
                    .executes(context -> executeGet(context.getSource(), EntityArgumentType.getEntity(context, "entities")))))
                .then(literal("clear")
                    .executes(context -> executeClearGravity(context.getSource(), Collections.singleton(context.getSource().getPlayer())))
                    .then(argument("entities", EntityArgumentType.entity())
                    .executes(context -> executeClearGravity(context.getSource(), EntityArgumentType.getEntities(context, "entities")))))
                .then(literalSetDefault)
                .then(literalRotate)
                .then(literal("randomise")
                    .executes(context -> executeRandomise(context.getSource(), Collections.singleton(context.getSource().getPlayer())))
                    .then(argument("entities", EntityArgumentType.entities())
                    .executes(context -> executeRandomise(context.getSource(), EntityArgumentType.getEntities(context, "entities"))))));
    }

    private static void getSendFeedback(ServerCommandSource source, Entity entity, Direction gravityDirection) {
        Text text = Text.translatable("direction." + gravityDirection.getName());
        if (source.getEntity() != null && source.getEntity() == entity) {
            source.sendFeedback(Text.translatable("commands.gravity.get.self", text), true);
        } else {
            source.sendFeedback(Text.translatable("commands.gravity.get.other", entity.getDisplayName(), text), true);
        }
    }

    private static int executeGet(ServerCommandSource source, Entity entity) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(entity);
        getSendFeedback(source, entity, gravityDirection);
        return gravityDirection.getId();
    }

    private static int executeSet(ServerCommandSource source, Direction gravityDirection, Collection<? extends Entity> entities) {
        int i = 0;
        for (Entity entity : entities) {
            //if (GravityChangerAPI.getGravityDirection(entity) != gravityDirection) {
                GravityChangerAPI.setGravity(entity,new Gravity(GravityChangerMod.GRAVITY_SOURCE_TRANSIENT, gravityDirection));
                //getSendFeedback(id, entity, gravityDirection);
                i++;
            //}
        }
        return i;
    }

    private static int executeRotate(ServerCommandSource source, FacingDirection relativeDirection, Collection<? extends Entity> entities) {
        int i = 0;
        for (Entity entity : entities) {
            Direction gravityDirection = GravityChangerAPI.getGravityDirection(entity);
            Direction combinedRelativeDirection = switch(relativeDirection) {
                case DOWN -> Direction.DOWN;
                case UP -> Direction.UP;
                case FORWARD, BACKWARD, LEFT, RIGHT -> Direction.fromHorizontal(relativeDirection.getHorizontalOffset() + Direction.fromRotation(entity.getYaw()).getHorizontal());
            };
            Direction newGravityDirection = RotationUtil.dirPlayerToWorld(combinedRelativeDirection, gravityDirection);
            GravityChangerAPI.setGravity(entity, new Gravity(GravityChangerMod.GRAVITY_SOURCE_TRANSIENT, newGravityDirection));
            //GravityChangerAPI.updateGravity(entity);
            getSendFeedback(source, entity, newGravityDirection);
            i++;
        }
        return i;
    }

    private static int executeRandomise(ServerCommandSource source, Collection<? extends Entity> entities) {
        int i = 0;
        for (Entity entity : entities) {
            Direction gravityDirection = Direction.random(source.getWorld().random);
            if (GravityChangerAPI.getGravityDirection(entity) != gravityDirection) {
                GravityChangerAPI.setGravity(entity, new Gravity(GravityChangerMod.GRAVITY_SOURCE_TRANSIENT, gravityDirection));
                //GravityChangerAPI.updateGravity(entity);
                getSendFeedback(source, entity, gravityDirection);
                i++;
            }
        }
        return i;
    }

    private static int executeClearGravity(ServerCommandSource source, Collection<? extends Entity> entities) {
        int i = 0;
        for (Entity entity : entities) {
            GravityChangerAPI.setGravity(entity, new Gravity(GravityChangerMod.GRAVITY_SOURCE_TRANSIENT, null));
            i++;
        }
        return i;
    }

    public enum FacingDirection {
        DOWN(-1, "down"),
        UP(-1, "up"),
        FORWARD(0, "forward"),
        BACKWARD(2, "backward"),
        LEFT(3, "left"),
        RIGHT(1, "right");

        private final int horizontalOffset;
        private final String name;

        FacingDirection(int horizontalOffset, String name) {
            this.horizontalOffset = horizontalOffset;
            this.name = name;
        }

        public int getHorizontalOffset() {
            return this.horizontalOffset;
        }

        public String getName() {
            return this.name;
        }
    }
}
