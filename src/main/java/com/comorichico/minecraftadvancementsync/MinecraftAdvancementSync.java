package com.comorichico.minecraftadvancementsync;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public final class MinecraftAdvancementSync extends JavaPlugin implements Listener {

    private DatabaseAccess databaseAccess;
    @Override
    public void onEnable() {
        // データベース接続などの初期化は非同期で行う
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            databaseAccess = new DatabaseAccess(this);
        });
        // イベントリスナーを登録
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // データベース接続などの後処理
        databaseAccess.closeConnection();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        // コマンドの処理
        if (command.getName().equalsIgnoreCase("mas")) {

            // 絶対パスの生成
            File dataFolder = getDataFolder();
            String absolutePath = dataFolder.getAbsolutePath();

            // index.htmlに出力
            databaseAccess.outputCompletedAdvancementsToHTML(absolutePath);

            // index.htmlをサーバーにアップロード
            FTPSUploader ftpsUploader = new FTPSUploader(this);

            // 達成済みのhtml
            String localFilePath = absolutePath + File.separator + "index.html";
            String remoteFilePath = "/mc/index.html";
            ftpsUploader.uploadFile(localFilePath, remoteFilePath);

            // 未達成のhtml
            String localFilePath2 = absolutePath + File.separator + "index2.html";
            String remoteFilePath2 = "/mc/index2.html";
            ftpsUploader.uploadFile(localFilePath2, remoteFilePath2);

            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) throws SQLException {
        databaseAccess.onPlayerAdvancementDone(event);
    }

    @EventHandler
    public void onPlayerAdvancementCriterionGrant(PlayerAdvancementCriterionGrantEvent event) throws IOException {
        databaseAccess.onPlayerAdvancementCriterionGrant(event);
    }
}