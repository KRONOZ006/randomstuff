package net.kronoz.randomstuff.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.kronoz.randomstuff.Randomstuff;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {
    public static final SimpleParticleType GOLDEN_BURST_PARTICLE =
            registerParticle("golden_burst_particle", FabricParticleTypes.simple(true));

    public static final SimpleParticleType HEART_BURST_PARTICLE =
            registerParticle("heart_burst_particle", FabricParticleTypes.simple(true));

    private static SimpleParticleType registerParticle(String name, SimpleParticleType particleType) {
        return Registry.register(Registries.PARTICLE_TYPE, Identifier.of(Randomstuff.MOD_ID, name), particleType);
    }

    public static void registerParticles() {
        Randomstuff.LOGGER.info("Registering Particles for " + Randomstuff.MOD_ID);
    }
}
