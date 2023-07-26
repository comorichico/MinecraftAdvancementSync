package com.comorichico.minecraftadvancementsync;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class MinecraftAdvancementSync extends JavaPlugin implements Listener {

    private DatabaseAccess databaseAccess;

    @Override
    public void onEnable() {
        // データベース接続などの初期化
        databaseAccess = new DatabaseAccess();

        // イベントリスナーを登録
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // データベース接続などの後処理
        databaseAccess.closeConnection();
    }

    public void setAdvancementForAllPlayers(String advancement_key, String criterion_name) {
        // まずWorldを取得します
        World world = Bukkit.getWorld("world"); // worldは対象のワールド名に置き換えてください

        if (world != null) {
            // ワールド内の全てのプレイヤーを取得します
            for (Player player : world.getPlayers()) {
                // Advancementのキーを指定してAdvancementオブジェクトを取得します
                Advancement advancement = Bukkit.getAdvancement(NamespacedKey.minecraft(advancement_key)); // advancement_keyは対象のAdvancementのキーに置き換えてください

                // Advancementがnullでない場合は、そのAdvancementをプレイヤーに設定します
                if (advancement != null) {
                    player.getAdvancementProgress(advancement).awardCriteria(criterion_name); // criterion_nameは対象のCriterionの名前に置き換えてください
                }
            }
        }
    }

    private static String convertAdvancementKey(String input) {
        String[] parts = input.split("/");
        String joinedString = String.join(".", parts);
        return "advancements." + joinedString + ".title";
    }

    // onPlayerAdvancementDoneは使ってないけど一応残してます
    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        String playerName = event.getPlayer().getName();
        String advancementName = event.getAdvancement().getKey().getKey();
        boolean achieved = true; // Advancementが完了したので常にtrue

        PlayerAdvancementData advancementData = new PlayerAdvancementData(playerName, advancementName, achieved);
        databaseAccess.insertAdvancementData(advancementData);
    }

    @EventHandler
    public void onPlayerAdvancementCriterionGrant(PlayerAdvancementCriterionGrantEvent event) throws IOException {
        String playerName = event.getPlayer().getName();
        String advancementName = event.getAdvancement().getKey().getKey();
        String criterionName = event.getCriterion();

        // advancementNameが"recipes"から始まる場合はそのままreturnする
        if (advancementName.startsWith("recipes")) {
            return;
        }

        // データベースから該当するAdvancementとCriterionのデータを取得
        PlayerAdvancementData playerAdvancementData = databaseAccess.getAdvancementData(playerName, advancementName);
        PlayerCriterionData criterionData = databaseAccess.getCriterionData(playerName, advancementName, criterionName);

        // データが存在しない場合は新たに追加
        if (playerAdvancementData == null) {
            playerAdvancementData = new PlayerAdvancementData(playerName, advancementName, true);
            databaseAccess.insertAdvancementData(playerAdvancementData);
            event.getPlayer().sendMessage(Component.text("データベースにplayerAdvancementDataを追加しました", NamedTextColor.GREEN));
        }

        if (criterionData == null) {
            criterionData = new PlayerCriterionData(playerName, advancementName, criterionName, true);
            databaseAccess.insertCriterionData(criterionData);
            event.getPlayer().sendMessage(Component.text("データベースにcriterionDataを追加しました", NamedTextColor.GREEN));
        }

        setAdvancementForAllPlayers(advancementName, criterionName);

        AdvancementData advancementData = new AdvancementData("ja_jp.json");
        advancementData.readJson();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Component.text(advancementName + "「"
                    + advancementData.getJapaneseValue(convertAdvancementKey(advancementName))
                    + "」の進捗の「" + criterionName + "」条件を達成しました。", NamedTextColor.GREEN));
        }
    }
}