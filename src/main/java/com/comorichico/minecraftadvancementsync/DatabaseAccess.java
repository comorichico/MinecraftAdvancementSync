package com.comorichico.minecraftadvancementsync;

import java.sql.*;

public class DatabaseAccess {

    private static final String DB_URL = "jdbc:sqlite:./database.db"; // データベースファイルのパスを指定
    // テーブル作成のSQL文
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, name TEXT, age INTEGER)";

    // データ挿入のSQL文
    private static final String INSERT_DATA_SQL = "INSERT INTO users (name, age) VALUES (?, ?)";

    // データ取得のSQL文
    private static final String QUERY_DATA_SQL = "SELECT * FROM users";

    private Connection connection;
    public DatabaseAccess() {
        try {
            connection = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createTable() {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(CREATE_TABLE_SQL);
            System.out.println("Table created successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // データを挿入するメソッド（Advancement用）
    public void insertAdvancementData(PlayerAdvancementData advancementData) {
        String playerName = advancementData.getPlayerName();
        String advancementName = advancementData.getAdvancementName();
        boolean achieved = advancementData.isAchieved();

        // 既存のデータが存在するか確認するクエリ
        String query = "SELECT * FROM player_advancements WHERE player_name=? AND advancement_name=?";
        try (PreparedStatement checkStmt = connection.prepareStatement(query)) {
            checkStmt.setString(1, playerName);
            checkStmt.setString(2, advancementName);
            ResultSet resultSet = checkStmt.executeQuery();

            // 既存のデータが存在しない場合のみ新しいデータを挿入する
            if (!resultSet.next()) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // データを挿入するメソッド（Criterion用）
    public void insertCriterionData(PlayerCriterionData criterionData) {
        String playerName = criterionData.getPlayerName();
        String advancementName = criterionData.getAdvancementName();
        String criterionName = criterionData.getCriterionName();
        boolean achieved = criterionData.isAchieved();

        // 既存のデータが存在するか確認するクエリ
        String query = "SELECT * FROM player_criteria WHERE player_name=? AND advancement_name=? AND criterion_name=?";
        try (PreparedStatement checkStmt = connection.prepareStatement(query)) {
            checkStmt.setString(1, playerName);
            checkStmt.setString(2, advancementName);
            checkStmt.setString(3, criterionName);
            ResultSet resultSet = checkStmt.executeQuery();

            // 既存のデータが存在しない場合のみ新しいデータを挿入する
            if (!resultSet.next()) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void queryData() {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(QUERY_DATA_SQL)) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                int age = resultSet.getInt("age");
                System.out.println("ID: " + id + ", Name: " + name + ", Age: " + age);
            }

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