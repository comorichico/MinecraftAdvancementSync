package com.comorichico.minecraftadvancementsync;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.Properties;

public class FTPSUploader {

    private final JavaPlugin plugin;
    FTPSUploader(JavaPlugin plugin){
        this.plugin = plugin;
    }
    public void uploadFile(String localFilePath, String remoteFilePath) {

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

            // ファイルをアップロード
            if (ftpsClient.storeFile(remoteFilePath, inputStream)) {
                plugin.getLogger().log(Level.INFO,"ファイルのアップロードが成功しました。");
            } else {
                plugin.getLogger().log(Level.INFO,"ファイルのアップロードに失敗しました。");
            }

            // ストリームとFTPクライアントを閉じる
            inputStream.close();
            ftpsClient.logout();
            ftpsClient.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
            plugin.getLogger().log(Level.INFO,"例外が発生しました。");
        }
    }
}