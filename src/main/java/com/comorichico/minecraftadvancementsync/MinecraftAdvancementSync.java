package com.comorichico.minecraftadvancementsync;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // コマンドの処理
        if (command.getName().equalsIgnoreCase("mas")) {

            File dataFolder = getDataFolder();
            String absolutePath = dataFolder.getAbsolutePath();

            // index.htmlに出力
            databaseAccess.outputCompletedAdvancementsToHTML(absolutePath);

            // index.htmlをサーバーにアップロード
            //FTPSUploader ftpsUploader = new FTPSUploader();
            //ftpsUploader.uploadFile(absolutePath);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        databaseAccess.onPlayerAdvancementDone(event);
    }

    @EventHandler
    public void onPlayerAdvancementCriterionGrant(PlayerAdvancementCriterionGrantEvent event) throws IOException {
        databaseAccess.onPlayerAdvancementCriterionGrant(event);
    }
}