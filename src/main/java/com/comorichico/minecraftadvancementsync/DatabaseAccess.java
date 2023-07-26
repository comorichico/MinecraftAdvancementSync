package com.comorichico.minecraftadvancementsync;

import java.io.File;
import java.sql.*;

public class DatabaseAccess {

    private static final String pathName = "plugins/MinecraftAdvancementSync";
    private static final String DB_URL = "jdbc:sqlite:" + pathName + "/database.db"; // データベースファイルのパスを指定
    // テーブル作成のSQL文（Advancement用）
    private static final String CREATE_ADVANCEMENTS_TABLE_SQL = "CREATE TABLE IF NOT EXISTS player_advancements (id INTEGER PRIMARY KEY, player_name TEXT, advancement_name TEXT, achieved BOOLEAN)";

    // テーブル作成のSQL文（Criterion用）
    private static final String CREATE_CRITERIA_TABLE_SQL = "CREATE TABLE IF NOT EXISTS player_criteria (id INTEGER PRIMARY KEY, player_name TEXT, advancement_name TEXT, criterion_name TEXT, achieved BOOLEAN)";

    // データ取得のSQL文（Advancement用）
    private static final String QUERY_ADVANCEMENTS_SQL = "SELECT * FROM player_advancements";

    // データ取得のSQL文（Criterion用）
    private static final String QUERY_CRITERIA_SQL = "SELECT * FROM player_criteria";

    // コネクションを保持する変数
    private Connection connection;

    public DatabaseAccess() {
        try {
            // データベースファイルのディレクトリを取得
            File dbDirectory = new File(this.pathName);

            // ディレクトリが存在しない場合に作成
            if (!dbDirectory.exists()) {
                dbDirectory.mkdirs();
            }

            // コネクションを初期化
            connection = DriverManager.getConnection(DB_URL);
            // テーブルを作成
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // テーブルを作成するメソッド
    private void createTables() {
        try {
            Statement statement = connection.createStatement();
            // Advancement用のテーブルを作成
            statement.executeUpdate(CREATE_ADVANCEMENTS_TABLE_SQL);
            // Criterion用のテーブルを作成
            statement.executeUpdate(CREATE_CRITERIA_TABLE_SQL);
            System.out.println("Tables created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Advancementデータを挿入するメソッド
    public void insertAdvancementData(PlayerAdvancementData advancementData) {
        String playerName = advancementData.getPlayerName();
        String advancementName = advancementData.getAdvancementName();
        boolean achieved = advancementData.isAchieved();

        String insertSql = "INSERT INTO player_advancements (player_name, advancement_name, achieved) VALUES (?, ?, ?)";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setString(1, playerName);
            insertStmt.setString(2, advancementName);
            insertStmt.setBoolean(3, achieved);
            insertStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Criterionデータを挿入するメソッド
    public void insertCriterionData(PlayerCriterionData criterionData) {
        String playerName = criterionData.getPlayerName();
        String advancementName = criterionData.getAdvancementName();
        String criterionName = criterionData.getCriterionName();
        boolean achieved = criterionData.isAchieved();

        String insertSql = "INSERT INTO player_criteria (player_name, advancement_name, criterion_name, achieved) VALUES (?, ?, ?, ?)";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            insertStmt.setString(1, playerName);
            insertStmt.setString(2, advancementName);
            insertStmt.setString(3, criterionName);
            insertStmt.setBoolean(4, achieved);
            insertStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // プレイヤーのAdvancementデータを取得するメソッド
    public PlayerAdvancementData getAdvancementData(String playerName, String advancementName) {
        String query = "SELECT * FROM player_advancements WHERE player_name=? AND advancement_name=?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerName);
            stmt.setString(2, advancementName);
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                boolean achieved = resultSet.getBoolean("achieved");
                return new PlayerAdvancementData(playerName, advancementName, achieved);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // プレイヤーのCriterionデータを取得するメソッド
    public PlayerCriterionData getCriterionData(String playerName, String advancementName, String criterionName) {
        String query = "SELECT * FROM player_criteria WHERE player_name=? AND advancement_name=? AND criterion_name=?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerName);
            stmt.setString(2, advancementName);
            stmt.setString(3, criterionName);
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                boolean achieved = resultSet.getBoolean("achieved");
                return new PlayerCriterionData(playerName, advancementName, criterionName, achieved);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // データベースへの接続をクローズするメソッド
    public void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}