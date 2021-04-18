package de.sean.blockprot.bukkit.inventories

import de.sean.blockprot.bukkit.nbt.BlockLockHandler
import de.sean.blockprot.bukkit.nbt.LockUtil
import de.sean.blockprot.bukkit.nbt.LockUtil.applyToDoor
import de.sean.blockprot.bukkit.nbt.LockUtil.getDoubleChest
import de.sean.blockprot.util.ItemUtil
import de.sean.blockprot.util.Strings
import de.tr7zw.nbtapi.NBTTileEntity
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import java.util.*

object BlockLockInventory : BlockProtInventory {
    override val size = 9 * 1
    override val inventoryName = Strings.getString("inventories.block_lock.name", "Block Lock")

    override fun onInventoryClick(event: InventoryClickEvent, state: InventoryState?) {
        val player = event.whoClicked as Player
        val item = event.currentItem ?: return
        val inv: Inventory
        val playersCol = Bukkit.getOnlinePlayers()
        val handler: BlockLockHandler
        val owner: String

        if (state == null) return
        val block: Block = state.block ?: return

        when (item.type) {
            in LockUtil.lockableTileEntities, in LockUtil.lockableBlocks -> {
                handler = BlockLockHandler(block)
                val doubleChest = getDoubleChest(block, player.world)
                val ret = handler.lockBlock(
                    player,
                    player.isOp,
                    if (doubleChest != null) NBTTileEntity(doubleChest) else null
                )
                if (ret.success) {
                    applyToDoor(handler, block)
                    player.closeInventory()
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, *TextComponent.fromLegacyText(ret.message))
                }
                event.isCancelled = true
            }
            Material.REDSTONE, Material.GUNPOWDER -> {
                handler = BlockLockHandler(block)
                val doubleChest = getDoubleChest(block, player.world)
                val ret = handler.lockRedstoneForBlock(
                    player.uniqueId.toString(),
                    if (doubleChest != null) NBTTileEntity(doubleChest) else null
                )
                if (ret.success) {
                    applyToDoor(handler, block)
                    player.closeInventory()
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, *TextComponent.fromLegacyText(ret.message))
                }
                event.isCancelled = true
            }
            Material.PLAYER_HEAD -> {
                handler = BlockLockHandler(block)
                owner = handler.getOwner()
                val friendsToAdd = FriendAddInventory.filterFriendsList(handler.getAccess(), playersCol.toList(), owner)
                inv = FriendAddInventory.createInventoryAndFill(friendsToAdd)
                player.closeInventory()
                player.openInventory(inv)
                event.isCancelled = true
            }
            Material.ZOMBIE_HEAD -> {
                handler = BlockLockHandler(block)
                val friends = handler.getAccess()
                inv = FriendRemoveInventory.createInventoryAndFill(friends)
                player.closeInventory()
                player.openInventory(inv)
                event.isCancelled = true
            }
            Material.OAK_SIGN -> {
                player.closeInventory()
                inv = BlockInfoInventory.createInventory()
                handler = BlockLockHandler(block)
                owner = handler.getOwner()
                val access = handler.getAccess()
                var i = 0
                inv.clear() // If any items are still in the inventory from last request, clear them
                while (i < access.size && i < 9) {
                    inv.setItem(9 + i, ItemUtil.getPlayerSkull(Bukkit.getOfflinePlayer(UUID.fromString(access[i]))))
                    i++
                }
                if (owner.isNotEmpty()) inv.setItem(
                    0,
                    ItemUtil.getPlayerSkull(Bukkit.getOfflinePlayer(UUID.fromString(owner)))
                )
                inv.setItem(8, ItemUtil.getItemStack(1, Material.BLACK_STAINED_GLASS_PANE, Strings.BACK))
                player.openInventory(inv)
            }
            Material.BLACK_STAINED_GLASS_PANE -> player.closeInventory()
            else -> player.closeInventory()
        }
    }

    fun createInventoryAndFill(player: Player, material: Material, handler: BlockLockHandler): Inventory {
        val inv = createInventory()
        val playerUuid = player.uniqueId.toString()
        val owner = handler.getOwner()
        val redstone = handler.getRedstone()
        if (owner == playerUuid || player.isOp || player.hasPermission(Strings.BLOCKPROT_ADMIN))
            inv.setItem(0, ItemUtil.getItemStack(1, material, Strings.UNLOCK))
        if (owner == playerUuid) {
            inv.setItem(
                1,
                ItemUtil.getItemStack(
                    1,
                    if (redstone) Material.REDSTONE else Material.GUNPOWDER,
                    if (redstone) Strings.BLOCK_LOCK_REDSTONE_DEACTIVATE
                    else Strings.BLOCK_LOCK_REDSTONE_ACTIVATE
                )
            )
            inv.setItem(2, ItemUtil.getItemStack(1, Material.PLAYER_HEAD, Strings.BLOCK_LOCK_ADD_FRIENDS))
            inv.setItem(3, ItemUtil.getItemStack(1, Material.ZOMBIE_HEAD, Strings.BLOCK_LOCK_REMOVE_FRIENDS))
        }
        if (player.isOp || player.hasPermission(Strings.BLOCKPROT_INFO) || player.hasPermission(Strings.BLOCKPROT_ADMIN))
            inv.setItem(7, ItemUtil.getItemStack(1, Material.OAK_SIGN, Strings.BLOCK_LOCK_INFO))
        inv.setItem(8, ItemUtil.getItemStack(1, Material.BLACK_STAINED_GLASS_PANE, Strings.BACK))
        return inv
    }
}