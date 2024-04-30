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
package io.rivrs.hephaestus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.bukkit.plugin.java.JavaPlugin;

import io.rivrs.hephaestus.command.ModelCommand;
import io.rivrs.hephaestus.registry.ModelRegistry;
import io.rivrs.hephaestus.track.ModelViewPersistenceHandlerImpl;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.central.CreativeCentralProvider;
import team.unnamed.creative.central.event.pack.ResourcePackGenerateEvent;
import team.unnamed.hephaestus.Model;
import team.unnamed.hephaestus.bukkit.BukkitModelEngine;
import team.unnamed.hephaestus.bukkit.v1_20_R3.BukkitModelEngine_v1_20_R3;
import team.unnamed.hephaestus.reader.blockbench.BBModelReader;
import team.unnamed.hephaestus.view.modifier.player.rig.PlayerRig;
import team.unnamed.hephaestus.view.modifier.player.rig.PlayerRigWriter;
import team.unnamed.hephaestus.writer.ModelWriter;

@SuppressWarnings("unused") // used via reflection by the server
public final class HephaestusPlugin extends JavaPlugin {
    private BukkitModelEngine engine;
    private ModelRegistry registry;

    @Override
    public void onEnable() {
        this.registry = new ModelRegistry();
        this.engine = BukkitModelEngine_v1_20_R3.create(this, new ModelViewPersistenceHandlerImpl(registry));

        Path dataFolder = this.getDataFolder().toPath();
        Path modelsFolder = dataFolder.resolve("blueprints");

        // Load model
        getLogger().info("Loading blueprints...");
        AtomicInteger attempted = new AtomicInteger();
        if (!Files.isDirectory(modelsFolder)) {
            try {
                Files.createDirectories(modelsFolder);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create blueprints folder", e);
            }
        } else {
            try (Stream<Path> pathStream = Files.walk(modelsFolder)) {
                pathStream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".bbmodel"))
                        .forEach(path -> {
                            attempted.incrementAndGet();
                            try (InputStream input = Files.newInputStream(path)) {
                                Model model = BBModelReader.blockbench().read(input);
                                registry.registerModel(model);
                            } catch (Exception e) {
                                getLogger().severe("Failed to load blueprint " + path + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException("Failed to load blueprints", e);
            }
        }
        getLogger().info("Loaded %d/%d blueprints.".formatted(registry.models().size(), attempted.get()));

        // Generate resource pack
        CreativeCentralProvider.get()
                .eventBus()
                .listen(this, ResourcePackGenerateEvent.class, event -> {
                    getLogger().info("Generating resource pack...");
                    final ResourcePack resourcePack = event.resourcePack();

                    ModelWriter.resource("hephaestus")
                            .write(resourcePack, registry.models());
                    PlayerRigWriter.resource(PlayerRig.detailed()).write(resourcePack);
                    getLogger().info("Resource pack generated.");
                });

        // Register command
        Objects.requireNonNull(getCommand("hephaestus"), "'hephaestus' command not registered! altered plugin.yml?")
                .setExecutor(new ModelCommand(this, registry, engine));
    }

    @Override
    public void onDisable() {
        if (engine != null)
            engine.close();
    }
}
