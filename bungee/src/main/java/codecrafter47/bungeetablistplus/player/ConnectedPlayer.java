/*
 * BungeeTabListPlus - a BungeeCord plugin to customize the tablist
 *
 * Copyright (C) 2014 - 2015 Florian Stober
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package codecrafter47.bungeetablistplus.player;

import codecrafter47.bungeetablistplus.BungeeTabListPlus;
import codecrafter47.bungeetablistplus.Options;
import codecrafter47.bungeetablistplus.api.bungee.BungeeTabListPlusAPI;
import codecrafter47.bungeetablistplus.api.bungee.CustomTablist;
import codecrafter47.bungeetablistplus.api.bungee.Skin;
import codecrafter47.bungeetablistplus.bridge.BukkitBridge;
import codecrafter47.bungeetablistplus.common.Constants;
import codecrafter47.bungeetablistplus.data.DataCache;
import codecrafter47.bungeetablistplus.data.DataKey;
import codecrafter47.bungeetablistplus.protocol.PacketHandler;
import codecrafter47.bungeetablistplus.skin.PlayerSkin;
import codecrafter47.bungeetablistplus.tablisthandler.LegacyTabList;
import codecrafter47.bungeetablistplus.tablisthandler.LoggingTabListLogic;
import codecrafter47.bungeetablistplus.tablisthandler.PlayerTablistHandler;
import codecrafter47.bungeetablistplus.tablisthandler.logic.GetGamemodeLogic;
import codecrafter47.bungeetablistplus.tablisthandler.logic.LowMemoryTabListLogic;
import codecrafter47.bungeetablistplus.tablisthandler.logic.RewriteLogic;
import codecrafter47.bungeetablistplus.tablisthandler.logic.TabListLogic;
import codecrafter47.bungeetablistplus.util.ReflectionUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.Synchronized;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public class ConnectedPlayer implements Player {

    private final ProxiedPlayer player;
    private Skin skin = null;

    private PacketHandler packetHandler = null;
    private PlayerTablistHandler playerTablistHandler = null;

    @Setter
    private BukkitBridge.BukkitData bukkitData;

    @Getter
    private DataCache data = new DataCache();

    @Getter
    @Setter
    private CustomTablist customTablist = null;

    @Getter
    private final LocalDateTime timePointJoined = LocalDateTime.now();

    public ConnectedPlayer(ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public UUID getUniqueID() {
        return player.getUniqueId();
    }

    @Override
    public Optional<ServerInfo> getServer() {
        Server server = player.getServer();
        if (server == null) return Optional.empty();
        return Optional.of(server.getInfo());
    }

    @Override
    public int getPing() {
        return player.getPing();
    }

    @Override
    public Skin getSkin() {
        if (skin == null) {
            skin = PlayerSkin.fromIcon(BungeeTabListPlusAPI.getIconFromPlayer(player));
        }
        return skin;
    }

    @Override
    public int getGameMode() {
        Integer gamemode = data.getRawValue(BungeeTabListPlus.DATA_KEY_GAMEMODE);
        return gamemode != null ? gamemode : 0;
    }

    public ProxiedPlayer getPlayer() {
        return player;
    }

    @Override
    public <T> Optional<T> get(DataKey<T> key) {
        if (key.isBungee()) {
            return data.getValue(key);
        }
        if (key.getScope() == DataKey.Scope.SERVER) {
            return getServer().flatMap(server -> BungeeTabListPlus.getInstance().getBridge().get(server, key));
        }
        Optional<T> value = bukkitData.getValue(key);
        if (!value.isPresent()) {
            Set<DataKey> requestedData = bukkitData.getRequestedData();
            if (!requestedData.contains(key)) {
                requestedData.add(key);
                try {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ObjectOutputStream out = new ObjectOutputStream(os);
                    out.writeUTF(Constants.subchannelRequestPlayerVariable);
                    out.writeObject(key);
                    out.close();
                    Optional.ofNullable(player.getServer()).ifPresent(server -> server.sendData(Constants.channel, os.toByteArray()));
                } catch (IOException ex) {
                    BungeeTabListPlus.getInstance().getLogger().log(Level.SEVERE, "Error while requesting data from bukkit", ex);
                }
            }
        }
        return value;
    }

    public <T> void registerDataChangeListener(DataKey<T> key, Consumer<T> listener) {
        if (key.isBungee()) {
            data.registerValueChangeListener(key, listener);
        } else {
            bukkitData.registerValueChangeListener(key, listener);
        }
    }

    @Synchronized
    public PacketHandler getPacketHandler() {
        if (packetHandler == null) {
            createTabListHandler();
        }
        return packetHandler;
    }

    @Synchronized
    public PlayerTablistHandler getPlayerTablistHandler() {
        if (playerTablistHandler == null) {
            createTabListHandler();
        }
        return playerTablistHandler;
    }

    @SneakyThrows
    private void createTabListHandler() {
        if (BungeeTabListPlus.getInstance().getProtocolVersionProvider().has18OrLater(getPlayer())) {
            TabListLogic tabListLogic;
            if (Options.DEBUG) {
                tabListLogic = new LoggingTabListLogic(null, getPlayer());
            } else {
                //TabListLogic tabListLogic = new TabListLogic(null, getPlayer());
                // TODO: revert this change as soon as the underlying issue is fixed
                tabListLogic = new LowMemoryTabListLogic(null, getPlayer());
            }
            playerTablistHandler = PlayerTablistHandler.create(getPlayer(), tabListLogic);
            packetHandler = new RewriteLogic(new GetGamemodeLogic(tabListLogic, ((UserConnection) getPlayer())));
            if (ReflectionUtil.getChannelWrapper(player).getHandle().eventLoop().inEventLoop()) {
                tabListLogic.onConnected();
            } else {
                ReflectionUtil.getChannelWrapper(player).getHandle().eventLoop().submit(tabListLogic::onConnected);
            }
        } else {
            LegacyTabList legacyTabList = new LegacyTabList(getPlayer(), getPlayer().getPendingConnection().getListener().getTabListSize());
            playerTablistHandler = PlayerTablistHandler.create(getPlayer(), legacyTabList);
            packetHandler = legacyTabList;
        }
    }

    public Duration getCurrentSessionDuration() {
        return Duration.between(getTimePointJoined(), LocalDateTime.now());
    }
}
