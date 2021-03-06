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
package codecrafter47.bungeetablistplus.section;

import codecrafter47.bungeetablistplus.api.bungee.tablist.Slot;
import codecrafter47.bungeetablistplus.api.bungee.tablist.TabListContext;

import java.util.OptionalInt;

public class ColumnSplitSection extends Section {

    private PlayerColumn[] pc = new PlayerColumn[10];
    private int columns = 10;

    @Override
    public int getMinSize() {
        return 0;
    }

    @Override
    public int getMaxSize() {
        int max = 0;
        for (int i = 0; i < pc.length; i++) {
            if (pc[i] != null) {
                int m = pc[i].getMaxSize();
                int span = 1;
                while (i + span != pc.length && pc[i + span] != null && (i + span < pc.length && pc[i + span - 1].filter.equals(pc[i + span].filter))) {
                    span++;
                }
                m = (m + span - 1) / span;
                if (max < m) {
                    max = m;
                }
                i += span - 1;
            }
        }
        return max * columns;
    }

    @Override
    public boolean isSizeConstant() {
        return false;
    }

    @Override
    public int getEffectiveSize(int proposedSize) {
        return (proposedSize / columns) * columns;
    }

    public void addColumn(int i, PlayerColumn column) {
        if (i >= pc.length) {
            PlayerColumn[] playerColumns = new PlayerColumn[i + 1];
            System.arraycopy(pc, 0, playerColumns, 0, pc.length);
            pc = playerColumns;
        }
        pc[i] = column;
    }

    @Override
    public Slot getSlotAt(TabListContext context, int pos, int size) {
        int column = pos % columns;
        int sizePerCol = size / columns;
        int columnPos = pos / columns;
        if (column >= pc.length) {
            return null;
        }
        PlayerColumn playerColumn = pc[column];
        if (playerColumn != null) {
            int span = 1;
            while (column + span < pc.length && pc[column + span] != null && pc[column + span - 1].filter.equals(pc[column + span].filter)) {
                span++;
            }
            int pre = 1;
            while (column - pre >= 0 && pc[column - pre] != null && playerColumn.filter.equals(pc[column - pre].filter)) {
                pre++;
            }
            pre--;
            span += pre;
            columnPos = columnPos * span + pre;
            return playerColumn.getSlotAt(context, columnPos, sizePerCol * span);
        }
        return null;
    }

    @Override
    public void preCalculate(TabListContext context) {
        for (PlayerColumn aPc : pc) {
            if (aPc != null) {
                aPc.precalculate(context);
            }
        }
        columns = context.getColumns();
    }

    @Override
    public OptionalInt getStartColumn() {
        return OptionalInt.of(0);
    }

}
