package com.fusionflux.gravity_api;

import com.fusionflux.gravity_api.item.ModItems;
import com.fusionflux.gravity_api.command.GravityCommand;
import com.fusionflux.gravity_api.config.GravityChangerConfig;
import com.fusionflux.gravity_api.util.GravitySources;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class GravityChangerMod implements ModInitializer {
    public static final String MOD_ID = "gravity_api";
    public static final Identifier CHANNEL_GRAVITY = new Identifier(MOD_ID, "gravity");
    public static GravityChangerConfig config;

    public static final ItemGroup GravityChangerGroup = FabricItemGroupBuilder.build(id("general"), () -> new ItemStack(ModItems.GRAVITY_CHANGER_UP));

    public static final Identifier GRAVITY_SOURCE_DEFAULT = id("default");
    public static final Identifier GRAVITY_SOURCE_ENTITY = id("player");
    public static final Identifier GRAVITY_SOURCE_TRANSIENT = id("transient");
    public static final Identifier GRAVITY_SOURCE_VEHICLE = id("vehicle");

    @Override
    public void onInitialize() {
        ModItems.init();

        AutoConfig.register(GravityChangerConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(GravityChangerConfig.class).getConfig();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> GravityCommand.register(dispatcher));

        GravitySources.register(GRAVITY_SOURCE_DEFAULT, Integer.MIN_VALUE, true, true);
        GravitySources.register(GRAVITY_SOURCE_ENTITY, 0, true, true);
        GravitySources.register(GRAVITY_SOURCE_TRANSIENT, 100, false, true);
        GravitySources.register(GRAVITY_SOURCE_VEHICLE, Integer.MAX_VALUE, false, true);
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
