/*
 * This file is part of hephaestus-engine, licensed under the MIT license
 *
 * Copyright (c) 2021-2023 Unnamed Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package team.unnamed.hephaestus.bukkit.v1_20_R3;

import com.google.common.collect.ImmutableMap;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.base.Vector3Float;
import team.unnamed.hephaestus.Bone;
import team.unnamed.hephaestus.Model;
import team.unnamed.hephaestus.animation.controller.AnimationPlayer;
import team.unnamed.hephaestus.bukkit.ModelView;
import team.unnamed.hephaestus.util.Quaternion;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

class ModelViewImpl implements ModelView {
    // We need the plugin instance for some operations
    private final Plugin plugin;

    private final Model model;
    private final Location location;
    private final float scale;

    private final AnimationPlayer animationPlayer;
    private final ImmutableMap<String, BoneEntity> bones;

    private final Collection<Player> viewers = new HashSet<>();

    // Invariable:
    // - If 'base' is set, 'baseEntityId' is set and 'viewers' is unused
    // - If 'base' is null, 'baseEntityId' is set and 'viewers' is used
    private Entity base = null;

    protected ModelViewImpl(final @NotNull Plugin plugin, final @NotNull Model model, final @NotNull Location location, final float scale) {
        this.plugin = requireNonNull(plugin, "plugin");
        this.model = requireNonNull(model, "model");
        this.location = requireNonNull(location, "location");
        this.scale = scale;
        this.animationPlayer = AnimationPlayer.create(this);
        this.bones = instantiateBones();
    }

    protected void base(final @Nullable Entity base) {
        this.base = base;
    }

    private ImmutableMap<String, BoneEntity> instantiateBones() {
        // create the bone entities
        ImmutableMap.Builder<String, BoneEntity> bones = ImmutableMap.builder();
        for (Bone bone : model.bones()) {
            instantiateBone(bone, Vector3Float.ZERO, bones);
        }
        return bones.build();
    }

    protected void instantiateBone(Bone bone, Vector3Float parentPosition, ImmutableMap.Builder<String, BoneEntity> into) {
        var position = bone.position().add(parentPosition);
        var entity = new BoneEntity(this, bone, position, Quaternion.IDENTITY.multiply(Quaternion.fromEulerDegrees(bone.rotation())), scale);
        into.put(bone.name(), entity);

        for (var child : bone.children()) {
            instantiateBone(child, position, into);
        }
    }

    @Override
    public @NotNull Model model() {
        return model;
    }

    @Override
    public UUID getUniqueId() {
        return base.getUniqueId();
    }

    @Override
    public @Nullable Entity base() {
        return base;
    }

    @Override
    public @NotNull Location location() {
        return location; // should we clone it?
    }

    @Override
    public @NotNull Collection<Player> viewers() {
        if (base != null) {
            return base.getTrackedBy();
        } else {
            return viewers;
        }
    }

    @Override
    public boolean addViewer(final @NotNull Player player) {
        requireNonNull(player, "player");
        if (base != null) {
            player.showEntity(plugin, base);
        } else {
            viewers.add(player);
            // todo: send packets
        }
        return false;
    }

    @Override
    public boolean removeViewer(final @NotNull Player player) {
        requireNonNull(player, "player");
        if (base != null) {
            player.hideEntity(plugin, base);
        } else {
            viewers.remove(player);
        }
        return false;
    }

    @Override
    public void emitSound(final @NotNull Sound sound) {
        final var location = location();
        final var x = location.x();
        final var y = location.y();
        final var z = location.z();
        for (final var viewer : viewers()) {
            viewer.playSound(sound, x, y, z);
        }
    }

    @Override
    public Collection<BoneEntity> bones() {
        return bones.values();
    }

    @Override
    public @Nullable BoneEntity bone(final @NotNull String name) {
        return bones.get(name);
    }

    @Override
    public AnimationPlayer animationController() {
        return animationPlayer;
    }

    @Override
    public void tickAnimations() {
        if (base != null) {
            if (base instanceof LivingEntity livingBase) {
                animationPlayer.tick(livingBase.getBodyYaw(), livingBase.getPitch());
            } else {
                animationPlayer.tick(base.getYaw(), base.getPitch());
            }
        } else {
            animationPlayer.tick();
        }
    }
}
