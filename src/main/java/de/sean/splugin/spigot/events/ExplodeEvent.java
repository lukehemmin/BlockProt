package de.sean.splugin.spigot.events;

/* SPlugin */
import de.sean.splugin.util.SLockUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

/* Java */
import java.util.Iterator;
import java.util.List;

/* NBT API */
import de.tr7zw.nbtapi.NBTTileEntity;

public class ExplodeEvent implements Listener {
    @EventHandler
    public void onEntityExplode(final EntityExplodeEvent e) {
        final List<Block> blocks = e.blockList();
        if (e.getEntityType() == EntityType.MINECART_TNT || e.getEntityType() == EntityType.PRIMED_TNT) {
            final Iterator<Block> it = blocks.iterator();
            while (it.hasNext()) {
                final Block b = it.next();
                if (b.getType() == Material.CHEST) {
                    final NBTTileEntity chest = new NBTTileEntity(b.getState());
                    // Someone owns this chest, block its destroying.
                    if (chest.getStringList(SLockUtil.LOCK_ATTRIBUTE) != null) {
                        it.remove();
                    }
                }
            }
        } else if (e.getEntityType() == EntityType.CREEPER) {
            // We don't want mob griefing but villagers use mob griefing to work
            // So we'll just do it like this.
            e.setCancelled(true);
        }
    }
}
