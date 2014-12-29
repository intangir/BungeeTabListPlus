/*
 * BungeeTabListPlus - a bungeecord plugin to customize the tablist
 *
 * Copyright (C) 2014 Florian Stober
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
package codecrafter47.bungeetablistplus.variables;

import codecrafter47.bungeetablistplus.BungeeTabListPlus;
import codecrafter47.bungeetablistplus.player.IPlayer;
import codecrafter47.bungeetablistplus.api.PlayerVariable;
import codecrafter47.bungeetablistplus.player.BungeePlayer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class TabNameVariable implements PlayerVariable {

    @Override
    public String getReplacement(ProxiedPlayer viewer, IPlayer player, String args) {
        String vname = BungeeTabListPlus.getInstance().getBridge().
                getPlayerInformation(player, "tabName");
        if (vname != null) {
            return vname;
        }
        if (player instanceof BungeePlayer)
            return ((BungeePlayer) player).getPlayer().getDisplayName();
        return player.getName();
    }

}