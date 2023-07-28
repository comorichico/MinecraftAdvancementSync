package com.comorichico.minecraftadvancementsync;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FTPSUploader {

    public void uploadFile(String absolutePath) {
        // ローカルのindex.htmlファイルのパス
        String localFilePath = absolutePath + File.separator + "index.html";
        // アップロード先のファイルパス
        String remoteFilePath = "/mc/index.html";
        // FTPサーバーの接続情報
        String server = "domain";
        String username = "user";
        String password = "password";

        FTPSClient ftpsClient = new FTPSClient();

        try {
            // FTPサーバーに接続
            ftpsClient.connect(server);
            int reply = ftpsClient.getReplyCode();
            if (!ftpsClient.isConnected()) {
                System.err.println("FTPサーバーへの接続に失敗しました。");
                return;
            }

            // ログイン
            if (!ftpsClient.login(username, password)) {
                ftpsClient.logout();
                System.err.println("FTPサーバーへのログインに失敗しました。");
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
                System.out.println("ファイルのアップロードが成功しました。");
            } else {
                System.err.println("ファイルのアップロードに失敗しました。");
            }

            // ストリームとFTPクライアントを閉じる
            inputStream.close();
            ftpsClient.logout();
            ftpsClient.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("例外が発生しました。");
        }
    }
}