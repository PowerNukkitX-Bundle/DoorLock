package cn.powernukkitx.doorlock;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockDoor;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.DoorToggleEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.PlaySoundPacket;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.powernukkitx.doorlock.item.ItemLock;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static cn.nukkit.level.ParticleEffect.TOTEM;

public class DoorLock extends PluginBase implements Listener {
    private static DoorLock instance;
    private Map<String, String> doorLockMap;
    private Map<String, Integer> originDoorTypeMap;

    public DoorLock() {
        instance = this;
    }

    public static DoorLock getInstance() {
        return instance;
    }

    public void onLoad() {
        Item.registerCustomItem(ItemLock.class);
    }

    public void onEnable() {
        Server.getInstance().getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
        Config config = this.getConfig();
        this.doorLockMap = (Map) config.get("locks", new HashMap());
        this.originDoorTypeMap = (Map) config.get("originDoorType", new HashMap());
    }

    public void onDisable() {
        Config config = this.getConfig();
        config.set("locks", this.doorLockMap);
        config.set("originDoorType", this.originDoorTypeMap);
        config.save();
    }

    @EventHandler
    private void onDoorToggle(DoorToggleEvent event) {
        BlockDoor door = (BlockDoor) event.getBlock();
        if (door.isTop()) {
            door = (BlockDoor) door.down();
        }

        String lockOwner = this.doorLockMap.get(this.encodeBlockVec3(door.asBlockVector3()));
        if (event.getPlayer().getInventory().getItemInHand() instanceof ItemLock) {
            this.tryLockDoor(event, door, lockOwner);
        }

    }

    @EventHandler
    private void onDoorBreak(BlockBreakEvent event) {
        BlockVector3 pos = event.getBlock().asBlockVector3();
        this.doorLockMap.remove(this.encodeBlockVec3(pos));
        this.originDoorTypeMap.remove(this.encodeBlockVec3(pos));
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        Block touchedBlock = event.getBlock();
        if (touchedBlock instanceof BlockDoor door) {
            if (door.isTop()) {
                door = (BlockDoor) door.down();
            }

            if (!this.doorLockMap.containsKey(this.encodeBlockVec3(door.asBlockVector3()))) {
                return;
            }

            String lockOwner = this.doorLockMap.get(this.encodeBlockVec3(door.asBlockVector3()));
            this.tryUnlockDoor(event.getPlayer(), door, lockOwner);
        }

    }

    private void tryLockDoor(DoorToggleEvent event, BlockDoor door, @Nullable String lockOwner) {
        event.setCancelled();
        if (lockOwner != null) {
            event.getPlayer().sendToast("无法上锁", "这个门已经被 " + lockOwner + " 上锁了！");
            event.setCancelled();
        } else {
            this.doorLockMap.put(this.encodeBlockVec3(door.asBlockVector3()), event.getPlayer().getName());
            this.originDoorTypeMap.put(this.encodeBlockVec3(door.asBlockVector3()), door.getId());
            event.getPlayer().sendToast("成功上锁", "门已经被上锁！");
            this.toIronDoor(door);
            event.getPlayer().getInventory().setItemInHand(Item.get(0));
            door.getLevel().addParticleEffect(new Vector3(door.x + 0.5, door.y + 1, door.z + 0.5), TOTEM);
        }
    }

    private void tryUnlockDoor(Player player, BlockDoor door, @Nullable String lockOwner) {
        if (lockOwner != null) {
            if (lockOwner.equals(player.getName())) {
                this.doorLockMap.remove(this.encodeBlockVec3(door.asBlockVector3()));
                this.toOriginDoorType(door);
                player.sendToast("成功解锁", "门锁已打开！");
                Item[] failed = player.getInventory().addItem(new ItemLock());
                if (failed.length != 0) {
                    player.getLevel().dropItem(door.up(), failed[0]);
                }
                door.getLevel().addParticleEffect(new Vector3(door.x + 0.5, door.y + 1, door.z + 0.5), TOTEM);
            } else {
                player.sendToast("无法打开", "这个门已经被 " + lockOwner + " 上锁了！");
            }
        }

    }

    private void toIronDoor(BlockDoor door) {
        BlockDoor ironDoor = (BlockDoor) Block.get(71);
        this.syncAndSetDoor(door, ironDoor);
    }

    private void toOriginDoorType(BlockDoor door) {
        Integer id = this.originDoorTypeMap.remove(this.encodeBlockVec3(door.asBlockVector3()));
        if (id == null) {
            id = 64;
        }

        BlockDoor originDoor = (BlockDoor) Block.get(id);
        this.syncAndSetDoor(door, originDoor);
    }

    private void syncAndSetDoor(BlockDoor oldDoor, BlockDoor newDoor) {
        newDoor.setOpen(false);
        newDoor.setBlockFace(oldDoor.getBlockFace());
        newDoor.setRightHinged(oldDoor.isRightHinged());
        oldDoor.getLevel().setBlock(oldDoor, newDoor, true, false);
        newDoor.setTop(true);
        oldDoor.getLevel().setBlock(oldDoor.up(), newDoor, true, false);
    }

    private String encodeBlockVec3(BlockVector3 vec) {
        return vec.x + "-" + vec.y + "-" + vec.z;
    }
}
