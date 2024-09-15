package org.gang.customShop

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.ticks
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.kotlindsl.*
import io.github.monun.invfx.InvFX.frame
import io.github.monun.invfx.openFrame
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.io.File
import java.util.concurrent.CompletableFuture


class CustomShop : org.bukkit.plugin.java.JavaPlugin() {

    companion object{
        lateinit var econ : Economy
    }
    private fun setupEconomy(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) {
            return false
        }
        val rsp = server.servicesManager.getRegistration(
            Economy::class.java
        )
        if (rsp == null) {
            return false
        }
        econ = rsp.provider
        return true
    }
    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(this).verboseOutput(true)) // Load with verbose output
    }
    override fun onEnable() {


        CommandAPI.onEnable()
        if (!setupEconomy() ) {
            logger.severe(String.format("[%s] - Disabled due to no Vault dependency found!", description.name));
        }
        commandTree("shop"){
            literalArgument("open"){
                stringArgument("file"){
                    playerExecutor { player, commandArguments ->
                        var page = 0
                        val file = commandArguments["file"] as String
                        val config = YamlConfiguration.loadConfiguration(File(dataFolder,"$file.yml"))
                        val list = config.itemList.toMutableList()
                        val frame = frame(6, Component.text(file)){
                            slot(0,4){
                                item = ItemStack(Material.ARROW)
                                onClick {
                                    page--
                                    if (page < 0) page = 0
                                }
                            }
                            slot(8,4){
                                item = ItemStack(Material.ARROW)
                                onClick {
                                    page++
                                    if (page > (list.size/36)) page--
                                }
                            }
                            onClick { x, y, event ->
                                if (x < 9 && y < 4 && event.isLeftClick){
                                    val item = event.currentItem
                                    item?.let {
                                        val price = item.itemMeta .persistentDataContainer.get(NamespacedKey.fromString("price")!!,
                                            PersistentDataType.INTEGER)
                                        val seller = item.itemMeta.persistentDataContainer.get(NamespacedKey.fromString("seller")!!,
                                            PersistentDataType.STRING)
                                        if (price != null && seller != null){
                                            val buyer = Bukkit.getOfflinePlayer(player.name)
                                            val sellerPlayer = Bukkit.getOfflinePlayer(seller)
                                            if (econ.getBalance(buyer) >= price){
                                                econ.withdrawPlayer(buyer,price.toDouble())
                                                econ.depositPlayer(sellerPlayer,price.toDouble())
                                                list.remove(item)
                                                config.itemList = list
                                                player.inventory.addItem(item)
                                                config.save(File(dataFolder,file))
                                                player.sendMessage("- $price")
                                                player.sendMessage("+ ${item.i18NDisplayName}")
                                            }
                                        }
                                    }
                                }
                            }
                            onOpen {
                                launch(minecraftDispatcher) {
                                    while (true){
                                        val lArrow = ItemStack(Material.ARROW).apply {
                                            editMeta { it.lore = listOf("${page+1}/${(list.size/36)+1}")
                                            }}
                                        val lMeta =  lArrow.itemMeta
                                        lMeta.setDisplayName("<")
                                        lArrow.setItemMeta(lMeta)
                                        val rArrow = ItemStack(Material.ARROW).apply {
                                            editMeta { it.lore = listOf("${page+1}/${(list.size/36)+1}")
                                            }}
                                        val rMeta =  lArrow.itemMeta
                                        rMeta.setDisplayName(">")
                                        rArrow.setItemMeta(rMeta)
                                        item(0,4,lArrow)
                                        item(8,4,lArrow)
                                        delay(5.ticks)
                                        for (y in 0..3){
                                            for (x in 0..8){
                                                val item = list.getOrNull((x + y * 9)+page * 36)
                                                item?.let {
                                                    item(x,y,item.apply {
                                                        editMeta { it.lore = listOf(
                                                            "가격 : ${
                                                                it.persistentDataContainer.getOrDefault(
                                                                    NamespacedKey.fromString("price")!!,
                                                                    PersistentDataType.INTEGER, 0
                                                                )
                                                            }",
                                                            "판매자 : ${
                                                                it.persistentDataContainer.getOrDefault(
                                                                    NamespacedKey.fromString("seller")!!,
                                                                    PersistentDataType.STRING, "null"
                                                                )
                                                            }",
                                                            "Left Click : 구매",
                                                        ) }
                                                    })
                                                }?:run {
                                                    item(x,y,null)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            onClose {
                                config.save(File(dataFolder,"$file.yml"))
                                player.inventory.contents.forEach { it ->
                                    it?.apply {
                                        editMeta { it.lore = null }
                                    }
                                }
                            }
                            onClickBottom { e->
                                val item = e.currentItem?:return@onClickBottom
                                val meta = item.itemMeta
                                when{
                                    e.isShiftClick -> {
                                        item.apply {
                                            editMeta{
                                                it.lore = null
                                            }
                                        }
                                        meta.persistentDataContainer.set(NamespacedKey.fromString("seller")!!,
                                            PersistentDataType.STRING,player.name)
                                        item.setItemMeta(meta)
                                        list.add(item)
                                        player.inventory.remove(item)
                                        config.itemList = list
                                        config.save(file)
                                    }
                                    e.isRightClick -> {

                                        meta.persistentDataContainer.set(NamespacedKey.fromString("price")!!,
                                            PersistentDataType.INTEGER,item.itemMeta.persistentDataContainer.getOrDefault(NamespacedKey.fromString("price")!!,
                                                PersistentDataType.INTEGER,0)+100)
                                    }
                                    e.isLeftClick -> {

                                        meta.persistentDataContainer.set(NamespacedKey.fromString("price")!!,
                                            PersistentDataType.INTEGER,item.itemMeta.persistentDataContainer.getOrDefault(NamespacedKey.fromString("price")!!,
                                                PersistentDataType.INTEGER,0)-100)
                                    }
                                    else->{}
                                }
                                item.setItemMeta(meta)
                                player.inventory.contents.forEach { it ->
                                    if (it == item){
                                        it.apply {
                                            editMeta { it.lore = listOf(
                                                "가격 : ${item.itemMeta.persistentDataContainer.getOrDefault(NamespacedKey.fromString("price")!!,
                                                    PersistentDataType.INTEGER,0)}",
                                                "Left Click : 가격 -100",
                                                "Right Click : 가격 +100",
                                                "Shift Click : 판매") }
                                        }
                                    }else{
                                        it?.apply {
                                            editMeta { it.lore = null }
                                        }
                                    }
                                }
                            }
                        }
                        player.openFrame(frame)
                    }
                }
            }
            withRequirement { sender: CommandSender -> sender.isOp }
            literalArgument("create"){
                stringArgument("config"){
                    playerExecutor { player, commandArguments ->
                        val config : String = commandArguments["config"] as String
                        val config2 = YamlConfiguration.loadConfiguration(File(dataFolder, "$config.yml"))
                        config2.itemList = listOf()
                        config2.save(File(dataFolder, "$config.yml"))
                        player.sendMessage("성공적으로 [${config}] 상점을 생성하였습니다.")
                    }
                }
            }
        }
    }

    override fun onDisable() {
        CommandAPI.onDisable()
    }
}

var YamlConfiguration.itemList : List<ItemStack>
    get() {
        @Suppress("UNCHECKED_CAST") val list: List<Map<String, Any>> = this.getMapList("items") as List<Map<String, Any>>
        return list.map { ItemStack.deserialize(it) }
    }
    set(value) {
        this.set("items", value.map { it.serialize() })
    }




