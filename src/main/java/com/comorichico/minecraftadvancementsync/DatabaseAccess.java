package com.comorichico.minecraftadvancementsync;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    // データベースから達成済み進捗データを取得するメソッド
    private List<PlayerAdvancementData> getCompletedAdvancementsDataFromDatabase() {
        List<PlayerAdvancementData> completedAdvancementsData = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_advancements WHERE achieved = ?")) {
            stmt.setBoolean(1, true);
            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                String playerName = resultSet.getString("player_name");
                String advancementName = resultSet.getString("advancement_name");
                boolean achieved = resultSet.getBoolean("achieved");
                completedAdvancementsData.add(new PlayerAdvancementData(playerName, advancementName, achieved));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return completedAdvancementsData;
    }

    // 達成済み進捗の一覧をHTMLファイルに出力するメソッド
    public void outputCompletedAdvancementsToHTML(String absolutePath) {
        // ここで適切なデータを取得してadvancementDataListに格納する処理を実装する
        List<PlayerAdvancementData> advancementDataList = getCompletedAdvancementsDataFromDatabase();

        File outputFile = new File(absolutePath + File.separator + "index.html");
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("<html>");
            writer.write("<head><title>Completed Advancements</title></head>");
            writer.write("<body>");
            writer.write("<h1>Completed Advancements</h1>");
            writer.write("<ul>");
            for (PlayerAdvancementData advancementData : advancementDataList) {
                String playerName = advancementData.getPlayerName();
                String advancementName = advancementData.getAdvancementName();
                writer.write("<li>" + playerName + ": " + advancementName + "</li>");
            }
            writer.write("</ul>");
            writer.write("</body>");
            writer.write("</html>");
        } catch (IOException e) {
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

    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        String playerName = event.getPlayer().getName();
        String advancementName = event.getAdvancement().getKey().getKey();
        boolean achieved = true; // Advancementが完了したので常にtrue

        // advancementNameが"recipes"から始まる場合はそのままreturnする
        if (advancementName.startsWith("recipes")) {
            return;
        }

        PlayerAdvancementData advancementData = new PlayerAdvancementData(playerName, advancementName, achieved);
        insertAdvancementData(advancementData);
    }
    public void onPlayerAdvancementCriterionGrant(PlayerAdvancementCriterionGrantEvent event) throws IOException {
        String playerName = event.getPlayer().getName();
        String advancementName = event.getAdvancement().getKey().getKey();
        String criterionName = event.getCriterion();

        // advancementNameが"recipes"から始まる場合はそのままreturnする
        if (advancementName.startsWith("recipes")) {
            return;
        }

        // データベースから該当するAdvancementとCriterionのデータを取得
        PlayerAdvancementData playerAdvancementData = getAdvancementData(playerName, advancementName);
        PlayerCriterionData criterionData = getCriterionData(playerName, advancementName, criterionName);

        // データが存在しない場合は新たに追加
        if (playerAdvancementData == null) {
            playerAdvancementData = new PlayerAdvancementData(playerName, advancementName, true);
            insertAdvancementData(playerAdvancementData);
            event.getPlayer().sendMessage(Component.text("データベースにplayerAdvancementDataを追加しました", NamedTextColor.GREEN));
        }

        if (criterionData == null) {
            criterionData = new PlayerCriterionData(playerName, advancementName, criterionName, true);
            insertCriterionData(criterionData);
            event.getPlayer().sendMessage(Component.text("データベースにcriterionDataを追加しました", NamedTextColor.GREEN));
        }

        setAdvancementForAllPlayers(advancementName, criterionName);

        AdvancementData advancementData = new AdvancementData("ja_jp.json");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Component.text(advancementName + "「"
                    + advancementData.getJapaneseValue(convertAdvancementKey(advancementName))
                    + "」の進捗の「" + criterionName + "」条件を達成しました。", NamedTextColor.GREEN));
        }
    }
}