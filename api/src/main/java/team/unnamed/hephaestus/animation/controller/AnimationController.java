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
package team.unnamed.hephaestus.animation.controller;

import team.unnamed.creative.base.Vector3Float;
import team.unnamed.hephaestus.animation.Animation;
import team.unnamed.hephaestus.util.Quaternion;
import team.unnamed.hephaestus.view.BaseModelView;

/**
 * Represents the object responsible to animate
 * a single model or group of models
 *
 * @since 1.0.0
 */
public interface AnimationController {

    /**
     * Queues the given {@link Animation} so that
     * it will be played in the next ticks
     *
     * @param animation The queued animation
     * @param transitionTicks The animation transition ticks
     * @since 1.0.0
     */
    void queue(Animation animation, int transitionTicks);

    /**
     * Queues the given {@link Animation} so that
     * it will be played in the next ticks, similar
     * to calling {@link AnimationController#queue}
     * using zero transition ticks
     *
     * @param animation The queued animation
     * @since 1.0.0
     */
    default void queue(Animation animation) {
        queue(animation, 0);
    }

    /**
     * Clears the animation queue and stops current
     * animation
     *
     * @since 1.0.0
     */
    void clearQueue();

    /**
     * Passes to the next animation frame using
     * the given model rotation and position
     *
     * @param initialRotation The initial model rotation
     * @param initialPosition The initial model position
     * @since 1.0.0
     */
    void tick(Quaternion initialRotation, Vector3Float initialPosition);

    /**
     * Passes to the next animation frame
     *
     * @since 1.0.0
     */
    default void tick() {
        tick(Quaternion.IDENTITY, Vector3Float.ZERO);
    }

    default void tick(Quaternion initialRotation) {
        tick(initialRotation, Vector3Float.ZERO);
    }

    default void tick(Vector3Float initialPosition) {
        tick(Quaternion.IDENTITY, initialPosition);
    }

    default void tick(float yaw, float pitch) {
        tick(yaw, pitch, Vector3Float.ZERO);
    }

    default void tick(float yaw, float pitch, Vector3Float initialPosition) {
        tick(
                Quaternion.fromEulerDegrees(new Vector3Float(
                        pitch,
                        (360 - yaw),
                        0
                )),
                initialPosition
        );
    };

    /**
     * ONLY USE WHEN USING AREA EFFECT CLOUDS SO THERE IS NO VISIBLE DELAY BETWEEN THE BONES
     * @param view the model view to use
     * @return Animation controller
     */
    static AnimationController nonDelayed(BaseModelView<?> view) {
        return new NormalAnimationController(view);
    }

}
