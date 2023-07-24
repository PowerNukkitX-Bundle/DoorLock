package cn.powernukkitx.doorlock.item;

import cn.nukkit.item.customitem.CustomItemDefinition;
import cn.nukkit.item.customitem.ItemCustom;
import cn.nukkit.item.customitem.data.ItemCreativeCategory;

public class ItemLock extends ItemCustom {
    public ItemLock() {
        super("doorlock:lock", "Lock", "lock");
    }

    public CustomItemDefinition getDefinition() {
        return CustomItemDefinition
                .simpleBuilder(this, ItemCreativeCategory.ITEMS)
                .allowOffHand(false)
                .build();
    }

    public int getMaxStackSize() {
        return 1;
    }
}
