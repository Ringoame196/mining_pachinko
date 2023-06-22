package com.github.Ringoame196

import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*


class Main : JavaPlugin(), Listener {
    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
    }
    val plugin = this

    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        val block = e.block
        val player = e.player
        val blockBelow = block.location.subtract(0.0, 1.0, 0.0).block
        val random = Random()

        val set_blockBellow = Material.BEDROCK

        val mainhand = player.inventory.itemInMainHand

        fun nohavepixel() {
            // 専用ピッケルじゃないときの処理まとめ
            player.sendMessage(ChatColor.RED.toString() + "専用ピッケルを購入してください")

            if (player.isOp) {
                val clickableText = ComponentBuilder(ChatColor.AQUA.toString() + "[GET]" + ChatColor.RED.toString() + "※OPメニュー")
                    .event(
                        net.md_5.bungee.api.chat.ClickEvent(
                            net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                            "/give @s iron_pickaxe{display:{Name:\"{\\\"text\\\":\\\"採掘パチンコ\\\",\\\"color\\\":\\\"gold\\\"}\",Lore:[\"採掘パチンコ用で使うピッケル\"]}} 1"
                        )
                    )
                    .create()

                player.spigot().sendMessage(*clickableText)
            }
        }

        // 当たりのエメラルド
        val hit_emerald = ItemStack(Material.EMERALD)
        val meta = hit_emerald.itemMeta
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD.toString() + "ラッキーエメラルド")
        }
        hit_emerald.setItemMeta(meta)

        if (block.type == Material.EMERALD_ORE) {
            val block_type = blockBelow.type
            if (block_type != set_blockBellow && block_type != Material.EMERALD_BLOCK && block_type != Material.REDSTONE_BLOCK) {
                return
            }
            e.isCancelled = true

            val itemMeta = mainhand.itemMeta
            if (itemMeta == null) {
                nohavepixel()
                return
            }
            if (mainhand.type != Material.IRON_PICKAXE || itemMeta.displayName != ChatColor.GOLD.toString() + "採掘パチンコ") {
                nohavepixel()
                return
            }

            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f)

            if (block_type == set_blockBellow) { // 通常時
                // 抽選
                val emerald_lottery = random.nextInt(120)
                val redstone_lottery = random.nextInt(60)

                lateinit var title:String
                if (emerald_lottery == 1) { // エメラルドブロックが当たったとき
                    blockBelow.setType(Material.EMERALD_BLOCK)
                    title = "当たり濃厚！！"
                } else if (redstone_lottery == 1) { // レッドストーンブロックが当たったとき
                    blockBelow.setType(Material.REDSTONE_BLOCK)
                    title = "チャンス"
                    player.playSound(player, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f)
                } else {
                    return
                }

                block.setType(Material.BEDROCK)
                val timer = Timer()
                timer.scheduleAtFixedRate(
                    object : TimerTask() {
                        var count = 0
                        var switching = 0

                        override fun run() {
                            if (count <= 10) {
                                if (switching == 0) {
                                    Bukkit.getScheduler().runTask(
                                        plugin,
                                        Runnable {
                                            player.sendTitle(ChatColor.YELLOW.toString() + title, " ", 1, 10, 1)
                                            if (redstone_lottery == 1) {
                                                blockBelow.setType(Material.REDSTONE_BLOCK)
                                            }
                                        }
                                    )
                                    switching = 1
                                } else {
                                    Bukkit.getScheduler().runTask(
                                        plugin,
                                        Runnable {
                                            player.sendTitle(title, " ", 1, 10, 1)
                                            if (redstone_lottery == 1) {
                                                blockBelow.setType(Material.BEDROCK)
                                            }
                                        }
                                    )
                                    switching = 0
                                }

                                if (emerald_lottery == 1) {
                                    Bukkit.getScheduler().runTask(
                                        plugin,
                                        Runnable {
                                            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                                        }
                                    )
                                }

                                count++
                            } else {
                                // タイマーを停止する
                                timer.cancel()

                                // ブロックの変更操作を同期的に行う
                                Bukkit.getScheduler().runTask(
                                    plugin,
                                    Runnable {
                                        block.type = Material.EMERALD_ORE
                                    }
                                )
                            }
                        }
                    },
                    10, 300
                )
            } else {
                if (blockBelow.type == Material.EMERALD_BLOCK) { // エメラルドブロックの処理
                    player.inventory.addItem(hit_emerald)
                    player.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f)
                } else { // レッドストーンブロックの処理
                    val redstone_ranodm = random.nextInt(2)
                    if (redstone_ranodm == 1) {
                        player.inventory.addItem(hit_emerald)

                        val timer = Timer()
                        timer.scheduleAtFixedRate(
                            object : TimerTask() {
                                var count = 0
                                var switching = 0
                                var title = "当たり"

                                override fun run() {
                                    if (count <= 10) {
                                        if (switching == 0) {
                                            player.sendTitle(ChatColor.YELLOW.toString() + title, " ", 1, 10, 1)
                                            switching = 1
                                        } else {
                                            player.sendTitle(title, " ", 1, 10, 1)
                                            switching = 0
                                        }
                                        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)

                                        count++
                                    } else {
                                        // タイマーを停止する
                                        timer.cancel()
                                    }
                                }
                            },
                            10, 300
                        )
                    }
                }

                blockBelow.setType(set_blockBellow)
            }
        }
    }
}
