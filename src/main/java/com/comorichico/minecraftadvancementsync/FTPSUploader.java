package com.comorichico.minecraftadvancementsync;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public class FTPSUploader {

    private final JavaPlugin plugin;
    FTPSUploader(JavaPlugin plugin){
        this.plugin = plugin;
    }

    // フォルダを作成するメソッド
    private boolean createRemoteFolder(FTPSClient ftpsClient, String remoteFolderPath) throws IOException {
        if (ftpsClient.makeDirectory(remoteFolderPath)) {
            plugin.getLogger().log(Level.INFO, "フォルダの作成に成功しました: " + remoteFolderPath);
            return true;
        } else {
            plugin.getLogger().log(Level.INFO, "フォルダの作成に失敗しました: " + remoteFolderPath);
            return false;
        }
    }

    public void uploadFile(@NotNull CommandSender sender) {

        // .envの読み込み
        File dataFolder = plugin.getDataFolder();
        String absolutePath = dataFolder.getAbsolutePath();
        String envPath = absolutePath + File.separator + ".env";
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream(envPath)) {
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        String server = prop.getProperty("server");
        String username = prop.getProperty("username");
        String password = prop.getProperty("password");
        String folder = prop.getProperty("folder");

        if(server.equals("domain")){
            sender.sendMessage(Component.text("masコマンドを打ちましたが.envの中身を編集していないのではないですか？「plugins/MinecraftAdvancementSync/.env」を編集してからmasコマンドをもう一度お試しください。", NamedTextColor.GREEN));
            return;
        }

        String localFilePath = absolutePath + File.separator + "index.html";
        String remoteFilePath = folder + "/index.html";

        String localFilePath2 = absolutePath + File.separator + "index2.html";
        String remoteFilePath2 = folder + "/index2.html";

        FTPSClient ftpsClient = new FTPSClient();

        try {
            // FTPサーバーに接続
            ftpsClient.connect(server);
            if (!ftpsClient.isConnected()) {
                plugin.getLogger().log(Level.INFO,"FTPサーバーへの接続に失敗しました。");
                return;
            }

            // ログイン
            if (!ftpsClient.login(username, password)) {
                ftpsClient.logout();
                plugin.getLogger().log(Level.INFO,"FTPサーバーへのログインに失敗しました。");
                return;
            }

            // ファイルをバイナリモードで転送
            ftpsClient.setFileType(FTPSClient.BINARY_FILE_TYPE);

            // PASVモードを有効化（必要に応じて）
            ftpsClient.enterLocalPassiveMode();

            // ローカルファイルをストリームで開く
            FileInputStream inputStream = new FileInputStream(localFilePath);

            // フォルダをアップロードする前に作成する
            if (ftpsClient.makeDirectory(folder)) {
                plugin.getLogger().log(Level.INFO, "フォルダの作成に成功しました: " + folder);
            } else {
                plugin.getLogger().log(Level.INFO, "フォルダの作成に失敗しました: " + folder);
            }

            plugin.getLogger().log(Level.INFO, "remoteFilePath: " + remoteFilePath);
            // ファイルをアップロード
            if (ftpsClient.storeFile(remoteFilePath, inputStream)) {
                plugin.getLogger().log(Level.INFO,"ファイルのアップロードが成功しました。");
            } else {
                plugin.getLogger().log(Level.INFO,"ファイルのアップロードに失敗しました。");
            }

            // ローカルファイルをストリームで開く
            FileInputStream inputStream2 = new FileInputStream(localFilePath2);

            plugin.getLogger().log(Level.INFO, "remoteFilePath2: " + remoteFilePath2);
            // ファイルをアップロード
            if (ftpsClient.storeFile(remoteFilePath2, inputStream2)) {
                plugin.getLogger().log(Level.INFO,"ファイルのアップロードが成功しました。");
            } else {
                plugin.getLogger().log(Level.INFO,"ファイルのアップロードに失敗しました。");
            }

            // ストリームとFTPクライアントを閉じる
            inputStream.close();
            ftpsClient.logout();
            ftpsClient.disconnect();

        } catch (IOException e) {
            plugin.getLogger().log(Level.INFO,"ファイルのアップロードエラーです。「plugins/MinecraftAdvancementSync/.env」ファイルを修正してください。");
        }
    }
}