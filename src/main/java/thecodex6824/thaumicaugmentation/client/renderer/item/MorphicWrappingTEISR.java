/**
 *  Thaumic Augmentation
 *  Copyright (c) 2019 TheCodex6824.
 *
 *  This file is part of Thaumic Augmentation.
 *
 *  Thaumic Augmentation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Thaumic Augmentation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Thaumic Augmentation.  If not, see <https://www.gnu.org/licenses/>.
 */

package thecodex6824.thaumicaugmentation.client.renderer.item;

import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;

public abstract class MorphicWrappingTEISR extends TileEntityItemStackRenderer {

    protected TileEntityItemStackRenderer original;
    
    protected abstract ItemStack getMorphicItem(ItemStack stack);
    
    public MorphicWrappingTEISR(TileEntityItemStackRenderer renderer) {
        original = renderer;
    }
    
    @Override
    public void renderByItem(ItemStack stack) {
        ItemStack item = getMorphicItem(stack);
        if (item.isEmpty())
            original.renderByItem(stack);
        else
            item.getItem().getTileEntityItemStackRenderer().renderByItem(item);
    }
    
}
