package com.micatechnologies.minecraft.ldtd.guide;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The Hearthkeeper's Guide (Phase 10): a written book that explains LDTD, given once to each
 * player on first joining (config hearth.giveGuide) and on request with {@code /hearth guide}.
 *
 * <p>Its pages are translation components, so the client shows them in its own language, and
 * they're never stale: the book carries keys, not text.</p>
 */
@Mod.EventBusSubscriber(modid = LdtdConstants.MOD_NAMESPACE)
public final class Guide {

    /**
     * Paragraphs on each page. Page n has the lang keys {@code ldtd.guide.<n>.title} and
     * {@code ldtd.guide.<n>.<1..k>}. Paragraphs are joined with a blank line, except the tower list
     * (page 5), one line each. Lang files can't hold line breaks, so the book adds them.
     */
    private static final int[] PARAGRAPHS = {2, 2, 2, 2, 5, 2, 2, 2, 2};

    private static final String GIVEN = LdtdConstants.MOD_NAMESPACE + ":guide_given";

    private Guide() {
        throw new AssertionError("No instances.");
    }

    /** A fresh copy of the guide. */
    public static ItemStack book() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("title", "The Hearthkeeper's Guide");
        tag.setString("author", "LDTD");
        tag.setBoolean("resolved", true);
        NBTTagList pages = new NBTTagList();
        for (int i = 1; i <= PARAGRAPHS.length; i++) {
            // JSON escapes, not raw line breaks: the page is a JSON text component.
            String gap = i == 5 ? "\\n" : "\\n\\n";
            StringBuilder page = new StringBuilder("[{\"translate\":\"ldtd.guide.").append(i).append(".title\",\"bold\":true}");
            for (int k = 1; k <= PARAGRAPHS[i - 1]; k++) {
                page.append(",{\"text\":\"").append(k == 1 ? "\\n\\n" : gap).append("\",\"bold\":false}")
                    .append(",{\"translate\":\"ldtd.guide.").append(i).append('.').append(k).append("\",\"bold\":false}");
            }
            pages.appendTag(new NBTTagString(page.append(']').toString()));
        }
        tag.setTag("pages", pages);
        book.setTagCompound(tag);
        return book;
    }

    /** Gives {@code player} a copy, or drops it at their feet if their inventory is full. */
    public static void give(EntityPlayer player) {
        ItemStack book = book();
        if (!player.inventory.addItemStackToInventory(book)) {
            player.dropItem(book, false);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!LdtdConfig.giveGuide || event.player.world.isRemote) {
            return;
        }
        NBTTagCompound persisted = event.player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(GIVEN)) {
            return;
        }
        persisted.setBoolean(GIVEN, true);
        event.player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        give(event.player);
    }
}
