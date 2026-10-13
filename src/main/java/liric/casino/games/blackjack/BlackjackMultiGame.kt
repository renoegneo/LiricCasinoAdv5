package liric.casino.games.blackjack

import liric.casino.CasinoPlugin
import liric.casino.util.SchedulerUtil
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID

enum class MultiGameState { WAITING, STARTING, PLAYING }

class BlackjackMultiGame(private val plugin: CasinoPlugin) {
    val maxPlayers = 4
    var state = MultiGameState.WAITING
        private set
    var countdownSeconds = 15
        private set


    val activePlayers = mutableMapOf<UUID, Double>()
    private var countdownTask: Runnable? = null

    fun addPlayer(player: Player, betAmount: Double): Boolean {
        if (state == MultiGameState.PLAYING) {
            player.sendMessage(plugin.format("<#FF0000>КАЗИНО</#FF0000> <gray>» <red>Игра уже началась. Дождитесь её окончания."))
            return false
        }

        if (activePlayers.size >= maxPlayers) {
            player.sendMessage(plugin.format("<#FF0000>КАЗИНО</#FF0000> <gray>» <red>Стол заполнен."))
            return false
        }

        if (activePlayers.containsKey(player.uniqueId)) {
            player.sendMessage(plugin.format("<#FF0000>КАЗИНО</#FF0000> <gray>» <red>Вы уже за этим столом."))
            return false
        }

        if (plugin.economyManager.withdrawPlayer(player, betAmount)?.transactionSuccess() != true) {
            player.sendMessage(plugin.messages.get("blackjack.no-funds"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return false
        }

        activePlayers[player.uniqueId] = betAmount
        broadcast("<#00FF7F>${player.name}</#00FF7F> <gray>занял место за столом со ставкой <#FFD700>$${betAmount}</#FFD700>. (${activePlayers.size}/$maxPlayers)")

        plugin.blackjackManager.updateHolograms()

        if (state == MultiGameState.WAITING) {
            startCountdown()
        } else if (activePlayers.size == maxPlayers) {

            countdownSeconds = 2
        }
        return true
    }


    fun removePlayer(player: Player) {
        if (!activePlayers.containsKey(player.uniqueId)) {
            player.sendMessage(plugin.format("<#FF0000>КАЗИНО</#FF0000> <gray>» <red>Вы не за игровым столом."))
            return
        }

        if (state == MultiGameState.PLAYING) {
            player.sendMessage(plugin.format("<#FF0000>КАЗИНО</#FF0000> <gray>» <red>Игра уже началась, сейчас нельзя уйти!"))
            return
        }


        val betAmount = activePlayers.remove(player.uniqueId) ?: 0.0


        plugin.economyManager.vault?.depositPlayer(player, betAmount)

        player.sendMessage(plugin.format("<#FF0000>КАЗИНО</#FF0000> <gray>» <#00FF7F>Вы покинули стол. Ваша ставка $${betAmount} возвращена.</#00FF7F>"))
        broadcast("<#FF5555>${player.name}</#FF5555> <gray>покинул стол. (${activePlayers.size}/$maxPlayers)")

        plugin.blackjackManager.updateHolograms()


        if (activePlayers.isEmpty()) {
            state = MultiGameState.WAITING
            countdownTask?.run()
            countdownTask = null
        }
    }


    private fun startCountdown() {
        state = MultiGameState.STARTING
        countdownSeconds = 15

        countdownTask = SchedulerUtil.runGlobalTimer(plugin, 0L, 20L) {
            if (activePlayers.isEmpty()) {
                state = MultiGameState.WAITING
                countdownTask?.run()
                return@runGlobalTimer
            }

            plugin.blackjackManager.updateHolograms()

            if (countdownSeconds <= 0) {
                startGame()
                countdownTask?.run()
                return@runGlobalTimer
            }

            if (countdownSeconds == 15 || countdownSeconds <= 5) {
                broadcast("<gray>Игра начнётся через <yellow>$countdownSeconds</yellow> с...")
            }
            countdownSeconds--
        }
    }

    private fun startGame() {
        state = MultiGameState.PLAYING
        plugin.blackjackManager.updateHolograms()
        broadcast("<#FFB400>СТАВКИ БОЛЬШЕ НЕ ПРИНИМАЮТСЯ! Дилер раздаёт карты...</#FFB400>")


        val session = BlackjackMultiSession(plugin, activePlayers.toMap(), this)
        session.start()
    }

    fun resetGame() {
        activePlayers.clear()
        state = MultiGameState.WAITING
        plugin.blackjackManager.updateHolograms()
    }

    fun updateBet(uuid: UUID, amount: Double) {
        if (uuid in activePlayers) activePlayers[uuid] = amount
    }

    fun cleanupAll() {
        countdownTask?.run()
        countdownTask = null
        activePlayers.forEach { (uuid, amount) ->
            plugin.economyManager.depositPlayer(org.bukkit.Bukkit.getOfflinePlayer(uuid), amount)
        }
        activePlayers.clear()
        state = MultiGameState.WAITING
    }

    private fun broadcast(msg: String) {
        val formatted = plugin.format("<#FF0000><bold>♠ СТОЛ БЛЭКДЖЕКА</bold></#FF0000> <gray>»</gray> $msg")
        activePlayers.keys.forEach { uuid ->
            plugin.server.getPlayer(uuid)?.sendMessage(formatted)
            plugin.server.getPlayer(uuid)?.playSound(plugin.server.getPlayer(uuid)!!.location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f)
        }
    }
}
