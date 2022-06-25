package com.fusionflux.gravity_api.util;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import javax.annotation.Nullable;

public record Gravity(Identifier id, @Nullable Direction direction) {
}
