package com.grinderwolf.swm.nms.v1201;

import com.grinderwolf.swm.clsm.CLSMBridge;
import com.grinderwolf.swm.clsm.ClassModifier;
import com.mojang.datafixers.util.Either;
import io.papermc.paper.chunk.system.scheduling.NewChunkHolder;
import io.papermc.paper.world.ChunkEntitySlices;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CraftCLSMBridge implements CLSMBridge {

    private final v1201SlimeNMS nmsInstance;

    @Override
    public Object getChunk(Object worldObject, int x, int z) {
        CustomWorldServer world = (CustomWorldServer) worldObject;
        return Either.left(world.getImposterChunk(x, z));
    }

    @Override
    public boolean saveChunk(Object world, Object chunkAccess) {
        if (!(world instanceof CustomWorldServer)) {
            return false; // Returning false will just run the original saveChunk method
        }

        if (!(chunkAccess instanceof ImposterProtoChunk || chunkAccess instanceof LevelChunk) || !((ChunkAccess) chunkAccess).isUnsaved()) {
            // We're only storing fully-loaded chunks that need to be saved
            return true;
        }

        LevelChunk chunk;

        if (chunkAccess instanceof ImposterProtoChunk) {
            chunk = ((ImposterProtoChunk) chunkAccess).getWrapped();
        } else {
            chunk = (LevelChunk) chunkAccess;
        }

        ((CustomWorldServer) world).saveChunk(chunk);
        chunk.setUnsaved(false);

        return true;
    }

    @Override
    public Object loadEntities(Object storage, Object chunkCoords) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean storeEntities(Object storage, Object entityList) {
        NewChunkHolder holder = (NewChunkHolder) storage;
        if (!isCustomWorld(holder.world)) {
            return false;
        }

        ((CustomWorldServer) holder.world).handleEntityUnLoad((NewChunkHolder) storage, (ChunkEntitySlices) entityList);
        return true;
    }

    @Override
    public boolean flushEntities(Object storage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCustomWorld(Object world) {
        if (world instanceof CustomWorldServer) {
            return true;
        } else if (world instanceof Level) {
            return false;
        } else {
            throw new IllegalStateException("World is probably not a world, was given %s. Check the classmodifier to ensure the correct level field is passed (check for field name changes)".formatted(world));
        }
    }

    @Override
    public Object injectCustomWorlds() {
        return nmsInstance.injectDefaultWorlds();
    }

    static void initialize(v1201SlimeNMS instance) {
        ClassModifier.setLoader(new CraftCLSMBridge(instance));
    }

    @Override
    public @NonNull Object getChunkTask(Object scheduler, Object world, int chunkX, int chunkZ, Object chunkHolder, Object priority, Object status) {
        return AswmChunkProgressionTask.create(scheduler, world, chunkX, chunkZ, chunkHolder, priority, status);
    }
}
