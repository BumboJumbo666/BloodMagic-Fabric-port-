package WayofTime.bloodmagic.item;

import WayofTime.bloodmagic.BloodMagic;
import WayofTime.bloodmagic.registry.ModBlocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;

public class ItemBucketEssence extends ItemBucket {

    public ItemBucketEssence() {
        super(ModBlocks.lifeEssence);

        setUnlocalizedName(BloodMagic.MODID + ".bucket.lifeEssence");
        setContainerItem(Items.bucket);
        setCreativeTab(BloodMagic.tabBloodMagic);
    }
}
