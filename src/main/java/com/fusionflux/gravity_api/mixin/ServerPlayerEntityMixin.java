package com.fusionflux.gravity_api.mixin;

import com.fusionflux.gravity_api.GravityChangerMod;
import com.fusionflux.gravity_api.accessor.ServerPlayerEntityAccessor;
import com.fusionflux.gravity_api.api.GravityChangerAPI;
import com.fusionflux.gravity_api.util.GravityComponent;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements  ServerPlayerEntityAccessor {
    @Shadow public ServerPlayNetworkHandler networkHandler;

    @Override
    public void gravitychanger$sendGravityPacket(Direction gravityDirection, boolean initialGravity) {
        if(this.networkHandler == null) return;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(gravityDirection);
        buf.writeBoolean(initialGravity);
        this.networkHandler.sendPacket(new CustomPayloadS2CPacket(GravityChangerMod.CHANNEL_GRAVITY, buf));
    }

    @Inject(
            method = "moveToWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private void inject_moveToWorld_sendPacket_1(CallbackInfoReturnable<ServerPlayerEntity> cir) {
        Optional<GravityComponent> gravityComponent = GravityChangerAPI.getGravityComponent((ServerPlayerEntity)(Object)this);
        gravityComponent.ifPresent(GravityComponent::changeDimension);
    }

    @Inject(
            method = "teleport",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void inject_teleport_sendPacket_0(CallbackInfo ci) {
        Optional<GravityComponent> gravityComponent = GravityChangerAPI.getGravityComponent((ServerPlayerEntity)(Object)this);
        gravityComponent.ifPresent(GravityComponent::changeDimension);
    }

    @Inject(
            method = "copyFrom",
            at = @At("TAIL")
    )
    private void inject_copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        Optional<GravityComponent> gravityComponent = GravityChangerAPI.getGravityComponent((ServerPlayerEntity)(Object)this);
        gravityComponent.ifPresent(gc -> gc.respawn(oldPlayer));
    }
}
