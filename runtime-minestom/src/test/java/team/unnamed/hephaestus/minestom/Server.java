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
package team.unnamed.hephaestus.minestom;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.ChunkGenerator;
import net.minestom.server.instance.ChunkPopulator;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.batch.ChunkBatch;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.metadata.Metadata;
import team.unnamed.creative.metadata.PackMeta;
import team.unnamed.creative.server.ResourcePackServer;
import team.unnamed.hephaestus.Model;
import team.unnamed.hephaestus.reader.blockbench.BBModelReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class Server {

    public static void main(String[] args) throws Exception {

        MinecraftServer server = MinecraftServer.init();
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();

        instance.setChunkGenerator(new ChunkGenerator() {
            @Override
            public void generateChunkData(@NotNull ChunkBatch batch, int chunkX, int chunkZ) {
                for (byte x = 0; x < Chunk.CHUNK_SIZE_X; x++)
                    for (byte z = 0; z < Chunk.CHUNK_SIZE_Z; z++)
                        batch.setBlock(x, 70, z, Block.GRASS_BLOCK);
            }
            @Override
            public List<ChunkPopulator> getPopulators() { return null; }
        });

        MinecraftServer.getExtensionManager().setLoadOnStartup(false);

        GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(PlayerLoginEvent.class, event -> {
            Player player = event.getPlayer();
            player.setGameMode(GameMode.CREATIVE);
            event.setSpawningInstance(instance);
            player.setRespawnPoint(new Pos(0, 72, 0));
        });

        ModelClickListener.register(eventHandler);

        ModelRegistry registry = new ModelRegistry();
        registry.model(modelFromResource("redstone_monstrosity.bbmodel"));

        ResourcePackProvider provider = new ResourcePackProvider(tree -> {
            tree.write(Metadata.builder()
                    .add(PackMeta.of(8, "Hephaestus generated resource pack"))
                    .build());

            tree.write("pack.png", Writable.resource(Server.class.getClassLoader(), "hephaestus.png"));

            // write models
            registry.write(tree);
        });
        ResourcePackServer resourcePackServer = ResourcePackServer.builder()
                .address("127.0.0.1", 7270)
                .handler(provider)
                .build();

        MinecraftServer.getCommandManager().register(new HephaestusCommand(registry, provider));

        resourcePackServer.start();
        server.start("127.0.0.1", 25565);

        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> resourcePackServer.stop(10));
    }

    private static Model modelFromResource(String name) throws IOException {
        try (InputStream input = Server.class.getClassLoader().getResourceAsStream(name)) {
            Objects.requireNonNull(input, "Model not found: " + name);
            return BBModelReader.blockbench().read(input);
        }
    }

}
