package liric.casino.games.coinflip

import liric.casino.CasinoPlugin
import liric.casino.core.AbstractMatchmakingCommand
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CoinFlipCommand(plugin: CasinoPlugin) : AbstractMatchmakingCommand(plugin, "coinflip", "ОРЁЛ ИЛИ РЕШКА") {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return true }
        
        val sub = args.getOrNull(0)?.lowercase()
        if (sub == "menu" || sub == "list") {
            if (plugin.isGameEnabled("coinflip")) CoinFlipGUI(plugin, sender).open()
            return true
        }
        if (sub == "reload") {
            if (!sender.hasPermission("casino.admin")) {
                sender.sendMessage(msg("general.no-permission")); return true
            }
            plugin.reloadAllConfigurations()
            sender.sendMessage(plugin.format("<#00FF7F>Настройки игры «Орёл или решка» перезагружены."))
            return true
        }
        
        return super.onCommand(sender, command, label, args)
    }

    override fun onCreate(player: Player, amount: Double) {
        plugin.coinFlipManager.createGame(player, amount)
    }

    override fun onJoin(player: Player, targetName: String) {
        plugin.coinFlipManager.joinGame(player, targetName)
    }

    override fun onCancel(player: Player) {
        plugin.coinFlipManager.cancelGame(player)
    }

    override fun getOpenGameCreators(): List<String> {
        return plugin.coinFlipManager.getOpenGames().map { it.creatorName }
    }

    override fun getOpenGameLines(): List<String> {
        return plugin.coinFlipManager.getOpenGames().map { s ->
            "  <white>${s.creatorName} <dark_gray>• <#FFD700>${CoinFlipMenu.formatAmount(s.betAmount)}"
        }
    }

    override fun sendHelp(player: Player) {
        val isAdmin = player.hasPermission("casino.admin")
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        player.sendMessage(plugin.format(" <#FFD700>🪙 <gold><bold>ОРЁЛ ИЛИ РЕШКА — Команды</bold>"))
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        player.sendMessage(plugin.format(" <#FFB400>/cf menu <dark_gray>— <gray>Посмотреть открытые игры"))
        player.sendMessage(plugin.format(" <#FFB400>/cf create <сумма> <dark_gray>— <gray>Создать игру"))
        player.sendMessage(plugin.format(" <#FFB400>/cf join <игрок> <dark_gray>— <gray>Присоединиться к игре"))
        player.sendMessage(plugin.format(" <#FFB400>/cf cancel <dark_gray>— <gray>Отменить свою игру"))
        if (isAdmin) {
            player.sendMessage(plugin.format("<dark_gray>─────────────────────────────────────────────"))
            player.sendMessage(plugin.format(" <red>/cf reload <dark_gray>— <gray>Перезагрузить настройки"))
        }
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val opts = mutableListOf("menu", "create", "join", "cancel", "help")
            if (sender.hasPermission("casino.admin")) opts += "reload"
            return opts.filter { it.startsWith(args[0], true) }
        }
        if (args.size == 2 && args[0].lowercase() == "create") {
            return listOf("100", "1k", "5k", "10k", "50k", "100k", "1m", "5m")
                .filter { it.startsWith(args[1]) }
        }
        return super.onTabComplete(sender, cmd, alias, args)
    }
}
