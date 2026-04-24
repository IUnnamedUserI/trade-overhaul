package com.unnameduser.tradeoverhaul.common.network;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import com.unnameduser.tradeoverhaul.common.config.ProfessionTradeFile;
import com.unnameduser.tradeoverhaul.common.config.TradeConfigLoader;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record ConfigSyncPayload(
        String configsHash,
        Map<Identifier, String> professionJsons,
        String settingsJson
) {
    // Используем обычный Identifier вместо Id<CustomPayload>
    public static final Identifier ID = new Identifier(TradeOverhaulMod.MOD_ID, "config_sync");

    public void write(PacketByteBuf buf) {
        buf.writeString(configsHash);
        buf.writeMap(professionJsons,
                (b, id) -> b.writeIdentifier(id),
                (b, json) -> b.writeString(json, 32767)
        );
        buf.writeString(settingsJson, 32767);
    }

    public static ConfigSyncPayload read(PacketByteBuf buf) {
        String hash = buf.readString();
        Map<Identifier, String> profs = buf.readMap(
                PacketByteBuf::readIdentifier,
                b -> b.readString(32767)
        );
        String settings = buf.readString(32767);
        return new ConfigSyncPayload(hash, profs, settings);
    }

    public static ConfigSyncPayload fromServerConfigs() {
        Map<Identifier, String> jsonMap = new HashMap<>();
        for (Map.Entry<Identifier, ProfessionTradeFile> entry : TradeConfigLoader.getAllProfessions().entrySet()) {
            jsonMap.put(entry.getKey(), TradeConfigLoader.professionToJson(entry.getValue()));
        }
        return new ConfigSyncPayload(
                TradeConfigLoader.getConfigsHash(),
                jsonMap,
                TradeConfigLoader.settingsToJson()
        );
    }
}