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
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

public final class MinecraftAdvancementSync extends JavaPlugin implements Listener {

    private DatabaseAccess databaseAccess;
    @Override
    public void onEnable() {
        // データベース接続などの初期化は非同期で行う
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            CreateEnvFile();
            databaseAccess = new DatabaseAccess(this);
        });

        // イベントリスナーを登録
        getServer().getPluginManager().registerEvents(this, this);
    }

    // .envファイルが存在しない場合に作成する
    public void CreateEnvFile(){

        File dataFolder = getDataFolder();
        String absolutePath = dataFolder.getAbsolutePath();

        // ディレクトリが存在しない場合に作成
        File directory = new File(absolutePath);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                getLogger().log(Level.INFO,"ディレクトリが作成されました。");
            } else {
                getLogger().log(Level.INFO,"ディレクトリの作成に失敗しました。");
            }
        }

        // .envファイルの絶対パス
        String envFilePath = absolutePath + File.separator + ".env";

        // .envファイルが存在する場合は終了
        File envFile = new File(envFilePath);
        if (envFile.exists()) {
            getLogger().log(Level.INFO,".envファイルは既に存在します。");
            return;
        }

        // .envファイルを作成し、内容を書き込む
        try {
            FileWriter writer = new FileWriter(envFile);
            writer.write("server=domain\n");
            writer.write("username=user\n");
            writer.write("password=password\n");
            writer.close();
            getLogger().log(Level.INFO,".envファイルが作成されました。");
        } catch (IOException e) {
            getLogger().log(Level.INFO,"エラーが発生しました: " + e.getMessage());
        }
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
    public void onPlayerAdvancementCriterionGrant(PlayerAdvancementCriterionGrantEvent event) {
        databaseAccess.onPlayerAdvancementCriterionGrant(event);
    }
}