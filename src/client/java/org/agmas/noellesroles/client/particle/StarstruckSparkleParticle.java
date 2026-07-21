package org.agmas.noellesroles.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.AnimatedParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

/**
 * 星界使者能力粒子。
 *
 * <p>这是从 StarryExpress 的 SimpleAnimatedParticle 搬到 Yarn 命名后的实现。
 * 粒子本身只在客户端渲染，服务端只负责通过 {@code spawnParticles} 发送出现位置。</p>
 */
public class StarstruckSparkleParticle extends AnimatedParticle {
    private final SpriteProvider sprites;

    StarstruckSparkleParticle(ClientWorld world,
                              double x,
                              double y,
                              double z,
                              double velocityX,
                              double velocityY,
                              double velocityZ,
                              SpriteProvider sprites) {
        super(world, x, y, z, sprites, 0.0125F);
        this.sprites = sprites;
        this.velocityMultiplier = 0.0F;
        this.gravityStrength = 0.0F;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.scale *= 0.75F;
        this.maxAge = 30 + this.random.nextInt(12);
        this.collidesWithWorld = true;
        this.setTargetColor(Color.WHITE.getRGB());
        this.setSpriteForAge(sprites);
    }

    @Override
    public void tick() {
        this.setSpriteForAge(this.sprites);
        if (this.age++ >= this.maxAge) {
            this.markDead();
        }
    }

    @Override
    public void move(double dx, double dy, double dz) {
        this.setBoundingBox(this.getBoundingBox().offset(dx, dy, dz));
        this.repositionFromBoundingBox();
    }

    @Environment(EnvType.CLIENT)
    public static class Provider implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;

        public Provider(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType parameters,
                                       @NotNull ClientWorld world,
                                       double x,
                                       double y,
                                       double z,
                                       double velocityX,
                                       double velocityY,
                                       double velocityZ) {
            return new StarstruckSparkleParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.sprites);
        }
    }
}
