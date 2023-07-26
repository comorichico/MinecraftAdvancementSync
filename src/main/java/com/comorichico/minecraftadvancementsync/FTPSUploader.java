package com.comorichico.minecraftadvancementsync;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FTPSUploader {
    public static void main(String[] args) {
        String server = "ftp.example.com";
        int port = 21; // FTPS port (default is usually 21)
        String username = "your_username";
        String password = "your_password";
        String localFilePath = "path_to_your_index.html";
        String remoteFilePath = "/path_on_server/index.html";

        FTPSClient ftpClient = new FTPSClient("TLS", false); // Connect using explicit TLS (FTPS)
        try {
            ftpClient.connect(server, port);
            ftpClient.login(username, password);

            File localFile = new File(localFilePath);
            try (FileInputStream inputStream = new FileInputStream(localFile)) {
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                ftpClient.enterLocalPassiveMode();
                ftpClient.storeFile(remoteFilePath, inputStream);
            }

            ftpClient.logout();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}