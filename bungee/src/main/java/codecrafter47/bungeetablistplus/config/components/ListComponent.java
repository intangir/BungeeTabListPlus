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

package codecrafter47.bungeetablistplus.config.components;

import codecrafter47.bungeetablistplus.BungeeTabListPlus;
import codecrafter47.bungeetablistplus.context.Context;
import codecrafter47.bungeetablistplus.layout.LayoutException;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Data
public class ListComponent extends Component {
    private final List<Component> list;

    public ListComponent(List<Component> list) {
        this.list = list;
    }

    @Override
    public boolean hasConstantSize() {
        return list.stream().allMatch(Component::hasConstantSize);
    }

    @Override
    public int getSize() {
        return list.stream().mapToInt(Component::getSize).sum();
    }

    @Override
    public Instance toInstance(Context context) {
        return new Instance(context, this);
    }

    public static class Instance extends Component.Instance {
        private final List<Component.Instance> components;
        private int minSize;
        private int preferredSize;
        private int maxSize;
        private boolean blockAlignment;

        protected Instance(Context context, ListComponent component) {
            super(context);
            components = new ArrayList<>(component.getList().size());
            for (Component component1 : component.getList()) {
                components.add(component1.toInstance(context));
            }
        }

        @Override
        public void activate() {
            super.activate();
            components.forEach(Component.Instance::activate);
        }

        @Override
        public void deactivate() {
            super.deactivate();
            components.forEach(Component.Instance::deactivate);
        }

        @Override
        public void update1stStep() {
            super.update1stStep();
            for (int i = 0; i < components.size(); i++) {
                Component.Instance component = components.get(i);
                component.update1stStep();
            }
            minSize = 0;
            for (int i = 0; i < components.size(); i++) {
                Component.Instance component = components.get(i);
                if (component.isBlockAligned()) {
                    minSize = ((minSize + context.get(Context.KEY_COLUMNS) - 1) / context.get(Context.KEY_COLUMNS)) * context.get(Context.KEY_COLUMNS);
                }
                minSize += component.getMinSize();
            }
            preferredSize = 0;
            for (int i = 0; i < components.size(); i++) {
                Component.Instance component = components.get(i);
                if (component.isBlockAligned()) {
                    preferredSize = ((preferredSize + context.get(Context.KEY_COLUMNS) - 1) / context.get(Context.KEY_COLUMNS)) * context.get(Context.KEY_COLUMNS);
                }
                preferredSize += component.getPreferredSize();
            }
            maxSize = 0;
            for (int i = 0; i < components.size(); i++) {
                Component.Instance component = components.get(i);
                if (component.isBlockAligned()) {
                    maxSize = ((maxSize + context.get(Context.KEY_COLUMNS) - 1) / context.get(Context.KEY_COLUMNS)) * context.get(Context.KEY_COLUMNS);
                }
                maxSize += component.getMaxSize();
            }
            blockAlignment = false;
            for (int i = 0; i < components.size(); i++) {
                Component.Instance component = components.get(i);
                if (component.isBlockAligned()) {
                    blockAlignment = true;
                }
            }

        }

        @Override
        public void update2ndStep() {
            super.update2ndStep();
            try {
                updateLayout();
            } catch (LayoutException e) {
                BungeeTabListPlus.getInstance().getLogger().log(Level.WARNING, "Config error.", e);
                return;
            }
            for (int i = 0; i < components.size(); i++) {
                Component.Instance component = components.get(i);
                component.update2ndStep();
            }
        }

        void updateLayout() throws LayoutException {
            int[] sectionSize = new int[components.size()];
            for (int i = 0; i < components.size(); i++) {
                sectionSize[i] = components.get(i).getMinSize();
            }

            int sizeNeeded = 0;
            for (int i = 0; i < components.size(); i++) {
                Component.Instance component = components.get(i);
                if (component.isBlockAligned()) {
                    sizeNeeded = ((sizeNeeded + context.get(Context.KEY_COLUMNS) - 1) / context.get(Context.KEY_COLUMNS)) * context.get(Context.KEY_COLUMNS);
                }
                sizeNeeded += sectionSize[i];
            }
            if (sizeNeeded > size) {
                throw new LayoutException(String.format("Minimum size the given layout would need is %d but tab_size is only %d", sizeNeeded, size));
            }

            boolean repeat;
            boolean max = false;
            do {
                repeat = false;

                for (int i = 0; i < components.size(); i++) {
                    Component.Instance component = components.get(i);
                    int oldSectionSize = sectionSize[i];
                    if (oldSectionSize >= (max ? component.getMaxSize() : component.getPreferredSize())) {
                        continue;
                    }
                    sectionSize[i] += component.isBlockAligned() ? context.get(Context.KEY_COLUMNS) : 1;

                    sizeNeeded = 0;
                    for (int j = 0; j < components.size(); j++) {
                        Component.Instance component1 = components.get(j);
                        if (component1.isBlockAligned()) {
                            sizeNeeded = ((sizeNeeded + context.get(Context.KEY_COLUMNS) - 1) / context.get(Context.KEY_COLUMNS)) * context.get(Context.KEY_COLUMNS);
                        }
                        sizeNeeded += sectionSize[j];
                    }

                    if (sizeNeeded <= size) {
                        repeat = true;
                    } else {
                        sectionSize[i] = oldSectionSize;
                    }
                }

                if (!repeat && !max) {
                    max = true;
                    repeat = true;
                }
            } while (repeat);

            int pos = 0;
            for (int i = 0; i < components.size(); i++) {
                Component.Instance component = components.get(i);
                if (component.isBlockAligned()) {
                    pos = ((pos + context.get(Context.KEY_COLUMNS) - 1) / context.get(Context.KEY_COLUMNS)) * context.get(Context.KEY_COLUMNS);
                }
                component.setPosition(row + (pos / context.get(Context.KEY_COLUMNS)), column + (pos % context.get(Context.KEY_COLUMNS)), sectionSize[i]);
                pos += sectionSize[i];
            }
        }

        @Override
        public int getMinSize() {
            return minSize;
        }

        @Override
        public int getPreferredSize() {
            return preferredSize;
        }

        @Override
        public int getMaxSize() {
            return maxSize;
        }

        @Override
        public boolean isBlockAligned() {
            return blockAlignment;
        }
    }
}
