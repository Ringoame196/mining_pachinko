package com.github.Ringoame196.Listeners

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class Events(private val plugin: Plugin) : Listener {
    val break_countlist: MutableMap<Location, Int> = mutableMapOf() // ブロックごとに変数を保存する

    // 下のブロックを管理するためのリスト
    val blockBelow1_list: MutableList<Material> = mutableListOf(Material.BEDROCK, Material.REDSTONE_BLOCK, Material.EMERALD_BLOCK)

    @EventHandler // イベント処理
    fun onBlockBreak(e: BlockBreakEvent) {
        val block = e.block
        val player = e.player
        val blockBelow = block.location.subtract(0.0, 1.0, 0.0).block
        val checkblock = block.location.subtract(0.0, 2.0, 0.0).block.type == Material.OBSERVER
        val mainhand = player.inventory.itemInMainHand
        val damageable_setting: Int

        if (block.type != Material.EMERALD_ORE) {
            return
        }
        if (!checkblock) {
            return
        }
        e.isCancelled = true

        val itemMeta = mainhand.itemMeta
        if (itemMeta == null) { // 素手で壊した場合
            NoHavePicaxe(player)
            return
        }
        // 鉄のピッケル以外で壊した場合
        itemMeta.let {
            if (mainhand.type != Material.IRON_PICKAXE) {
                NoHavePicaxe(player)
                return
            }
        }

        val display_name = itemMeta.displayName
        // パチンコ1号機の処理
        if (blockBelow1_list.contains(blockBelow.type)) {
            if (display_name != ChatColor.GOLD.toString() + "採掘パチンコ(1号機)") {
                NoHavePicaxe(player)
                return
            }
            damageable_setting = 5
            Pachinko1(block, blockBelow, player, e)
        } else { // パチンコに設定していないブロックだった場合 処理停止
            return
        }

        // 専用ピッケルで破壊したときに音を鳴らす
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f)
        // 耐久値を減らす
        durabilityitem(mainhand, damageable_setting, player)
    }
    @EventHandler
    fun onPlayerInteractEvent(e: PlayerInteractEvent) {
        // ブロックをクリックしたときの処理
        val player = e.player
        val block = e.clickedBlock
        val checkblock = block?.location?.subtract(0.0, 2.0, 0.0)?.block?.type == Material.OBSERVER

        // エメラルド鉱石以外なら処理しない
        if (block?.type != Material.EMERALD_ORE) {
            return
        }
        // 右クリックじゃなかったら処理しない
        if (e.action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return
        }
        if (!checkblock) {
            return
        }

        // コマブロでクリックした場合 GIVE GUIを開く
        if (player.inventory.itemInMainHand.type == Material.COMMAND_BLOCK) {
            if (!player.isOp) {
                return
            }
            e.isCancelled = true
            // GUI作成
            val giveGUI = Bukkit.createInventory(null, 9, ChatColor.DARK_AQUA.toString() + "パチンコピッケル")

            for (i in 1..giveGUI.size) {
                val item = ItemStack(Material.IRON_PICKAXE)
                val itemMeta: ItemMeta? = item.itemMeta
                itemMeta?.setDisplayName(ChatColor.YELLOW.toString() + i.toString() + "号機のピッケル")
                item.setItemMeta(itemMeta)

                giveGUI.setItem(i - 1, item)
            }
            // プレイヤーにGUIを表示します
            player.openInventory(giveGUI)
            return
        }

        // 連続のハズレ数を取得して titleに流す
        val break_count = break_countlist[block.getLocation()] ?: 0
        title(player, "", ChatColor.GOLD.toString() + "連続" + break_count.toString() + "回ハズレ")
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        // GUIをクリックしたときのイベント
        if (e.currentItem == null) {
            return
        }
        if (e.view.title != ChatColor.DARK_AQUA.toString() + "パチンコピッケル") {
            return
        }
        e.isCancelled = true

        val click_item = e.currentItem?.type == Material.IRON_PICKAXE
        if (!click_item) {
            return
        }
        val click_number = e.slot // クリックしたスロットの番号取得
        var command: String? = null
        val player = e.whoClicked as Player

        if (click_number == 0) {
            command = "{display:{Name:\"{\\\"text\\\":\\\"採掘パチンコ(1号機)\\\",\\\"color\\\":\\\"gold\\\"}\",Lore:[\"採掘パチンコ用で使うピッケル\"]},CanDestroy:[\"minecraft:emerald_ore\"]}"
        } else {
            message(player, ChatColor.RED.toString() + "現在ピッケル登録されていません")
        }
        if (command != null) { Bukkit.dispatchCommand(player, "give @s iron_pickaxe" + command) }

        player.closeInventory()
    }

    fun Pachinko1(block: Block, blockBelow: Block, player: Player, e: BlockBreakEvent) {
        // パチンコ1号機の処理
        val random = Random()

        e.isCancelled = true

        val block_type = blockBelow.type

        val blockLocation = block.location

        if (block_type == Material.BEDROCK) { // 通常時
            // 確率
            val currentValue = break_countlist.getOrDefault(blockLocation, 0)
            break_countlist[blockLocation] = currentValue + 1

            // 抽選
            val emerald_lottery = random.nextInt(120) == 0
            val redstone_lottery = random.nextInt(60) == 0

            if (emerald_lottery) { // エメラルドブロックが当たったとき
                staging(
                    "当たり濃厚！！",
                    "鉱石を壊せ！！！",
                    player,
                    block,
                    Material.EMERALD_BLOCK,
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP
                )
            } else if (redstone_lottery) { // レッドストーンブロックが当たったとき
                player.playSound(player, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f)
                staging(
                    "チャンス",
                    "鉱石を壊せ！！！",
                    player,
                    block,
                    Material.REDSTONE_BLOCK,
                    null
                )
            }
        } else {
            if (blockBelow.type == Material.EMERALD_BLOCK) { // エメラルドブロックの処理
                staging(
                    "当たった！！！",
                    "",
                    player,
                    block,
                    Material.BEDROCK,
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP
                )
                hit(player, block)
            } else { // レッドストーンブロックの処理
                val redstone_ranodm = random.nextInt(2) == 0
                if (redstone_ranodm) {
                    // レッドストーンが1/2であったときの繰り返し処理
                    if (!redstone_ranodm) {
                        return
                    }
                    staging(
                        "当たり！！",
                        "",
                        player,
                        block,
                        Material.BEDROCK,
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP
                    )
                    hit(player, block)
                }
            }
            blockBelow.setType(Material.BEDROCK)
        }
    }

    fun NoHavePicaxe(player: Player) {
        // 専用ピッケルじゃないときの処理まとめ
        message(player, ChatColor.RED.toString() + "専用ピッケルを購入してください")
    }

    fun hit(player: Player, block: Block) {
        // 当たりのエメラルド
        val hit_emerald = ItemStack(Material.EMERALD)
        val meta = hit_emerald.itemMeta
        meta?.let {
            it.setDisplayName(ChatColor.GOLD.toString() + "ラッキーエメラルド")
            hit_emerald.setItemMeta(it)

            // 連続ハズレをリセットする
            val blockLocation = block.location
            break_countlist[blockLocation] = 0
        }

        player.spawnParticle(Particle.EXPLOSION_LARGE, block.getLocation(), 100, 0.5, 0.5, 0.5, 0.1)
        player.inventory.addItem(hit_emerald)
        player.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f)
    }

    fun message(player: Player, message: String) {
        // player.sendMessageを テンプレート化
        player.sendMessage(message)
    }
    fun title(player: Player, title: String, subtitle: String) {
        // titleを テンプレート化
        player.sendTitle(title, subtitle, 3, 20, 3)
    }
    fun staging(title: String, subtitle: String, player: Player, block: Block, set_block: Material, sound: Sound?) {
        // 演出
        block.setType(Material.BEDROCK)
        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20, 255))

        object : BukkitRunnable() {
            var count = 0
            var switching = true
            val blockBelow = block.location.subtract(0.0, 1.0, 0.0).block

            override fun run() {
                if (count <= 10) {
                    this.switching = if (switching) {
                        title(player, title, ChatColor.GREEN.toString() + subtitle)
                        blockBelow.setType(set_block)
                        false
                    } else {
                        title(player, ChatColor.YELLOW.toString() + title, subtitle)
                        blockBelow.setType(Material.BEDROCK)
                        true
                    }
                    sound?.let {
                        player.playSound(player, sound, 1.0f, 1.0f)
                    }
                    count++
                } else {
                    block.setType(Material.EMERALD_ORE)
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0, 8)
    }

    fun durabilityitem(mainhand: ItemStack, damageable_setting: Int, player: Player) {
        // 耐久値を減らす
        if (player.gameMode == GameMode.CREATIVE) { return }
        val itemMeta = mainhand.itemMeta
        if (itemMeta is Damageable) {
            val damageable = itemMeta
            var durability = damageable.damage.toShort()
            durability = (durability + damageable_setting).toShort()
            if (durability >= 250) { // 耐久値が0になったらアイテムを消す
                mainhand.setAmount(0)
                player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
                message(player, ChatColor.RED.toString() + "ピッケルが壊れた")
                return
            }
            damageable.damage = durability.toInt()
            mainhand.setItemMeta(itemMeta)
        }
    }
}
