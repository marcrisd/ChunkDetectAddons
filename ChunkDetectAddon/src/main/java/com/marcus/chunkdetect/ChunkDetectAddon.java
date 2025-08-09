package com.marcus.chunkdetect;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class ChunkDetectAddon extends MeteorAddon {
    @Override
    public void onInitialize() {
        Modules.get().add(new ChunkDetectModule());
    }

    @Override
    public String getPackage() {
        return "com.marcus.chunkdetect";
    }
}