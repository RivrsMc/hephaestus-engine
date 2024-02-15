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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import team.unnamed.creative.base.Vector3Float;
import team.unnamed.hephaestus.Bone;
import team.unnamed.hephaestus.bukkit.BoneView;
import team.unnamed.hephaestus.player.PlayerBoneType;
import team.unnamed.hephaestus.player.PlayerModel;
import team.unnamed.hephaestus.util.Quaternion;

public class PlayerModelEntity extends MinecraftModelEntity {
    public PlayerModelEntity(EntityType<? extends PathfinderMob> type, PlayerModel model, Level level, float scale) {
        super(type, model, level, scale);
    }


    @Override
    protected void animationTick() {
        animationController.tick(getYRot(), getXRot());
    }

    @Override
    public PlayerModel model() {
        return (PlayerModel) model;
    }

    protected void instantiateBone(
            Bone bone,
            Vector3Float parentPosition,
            ImmutableMap.Builder<String, BoneEntity> into
    ) {
        var position = bone.position().add(parentPosition);
        PlayerBoneType simplePlayerBoneType = model().boneTypeOf(bone.name());
        var entity = simplePlayerBoneType == null
                ? new BoneEntity(this, bone, position, Quaternion.IDENTITY.multiply(Quaternion.fromEulerDegrees(bone.rotation())), scale)
                : new PlayerBoneEntity(this, bone, position, Quaternion.IDENTITY.multiply(Quaternion.fromEulerDegrees(bone.rotation())), scale);

        into.put(bone.name(), entity);

        for (var child : bone.children()) {
            instantiateBone(child, position, into);
        }
    }
}