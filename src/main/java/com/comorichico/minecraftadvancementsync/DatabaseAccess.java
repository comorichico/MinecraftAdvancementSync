package com.comorichico.minecraftadvancementsync;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseAccess {

    private static final String pathName = "plugins/MinecraftAdvancementSync";
    private static final String DB_URL = "jdbc:sqlite:" + pathName + "/database.db"; // データベースファイルのパスを指定

    private final JavaPlugin plugin;
    // テーブル作成のSQL文（Advancement用）
    private static final String CREATE_ADVANCEMENTS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS player_advancements ("
                    + "id INTEGER PRIMARY KEY, "
                    + "player_name TEXT, "
                    + "achieved BOOLEAN,"
                    + "title TEXT, "
                    + "title_key TEXT UNIQUE, "
                    + "description TEXT, "
                    + "description_key TEXT"
                    + ")";

    // テーブル作成のSQL文（Criterion用）
    private static final String CREATE_CRITERIA_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS player_criteria ("
                    + "id INTEGER PRIMARY KEY, "
                    + "player_name TEXT, "
                    + "advancement_name TEXT, "
                    + "criterion_name TEXT UNIQUE, "
                    + "achieved BOOLEAN"
                    + ")";
    private static final String INSERT_ADVANCEMENT_SQL =
            "INSERT INTO player_advancements "
                    + "(player_name, achieved, title, title_key, description, description_key) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

    // コネクションを保持する変数
    private Connection connection;

    public DatabaseAccess(JavaPlugin plugin) {
        this.plugin = plugin;
        try {
            // データベースファイルのディレクトリを取得
            File dbDirectory = new File(pathName);

            // ディレクトリが存在しない場合に作成
            if (!dbDirectory.exists()) {
                boolean created = dbDirectory.mkdirs();
                if (created) {
                    plugin.getLogger().log(Level.INFO,"ディレクトリが作成されました。");
                } else {
                    plugin.getLogger().log(Level.INFO,"ディレクトリの作成に失敗しました。");
                }
            }

            // コネクションを初期化
            connection = DriverManager.getConnection(DB_URL);
            // テーブルを作成
            createTables();

            String fileName = "ja_jp.json";
            // コンストラクタでファイルパスを設定
            Gson gson = new Gson();

            // リソースが存在するか確認
            InputStream resourceStream = MinecraftAdvancementSync.class.getClassLoader().getResourceAsStream(fileName);
            if (resourceStream != null) {
                // リソースが存在する場合にのみ InputStreamReader を作成
                InputStreamReader reader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8);
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

                PreparedStatement statement = connection.prepareStatement(INSERT_ADVANCEMENT_SQL);


                // keyがadvancementsからはじまりtitleで終わるものとdescriptionで終わるものの対になるデータを取得する
                for (String key : jsonObject.keySet()) {
                    if (key.startsWith("advancements") && key.endsWith("title")) {

                        // "title_key" がテーブル内に既に存在するかを確認
                        PreparedStatement checkStatement = connection.prepareStatement("SELECT title_key FROM player_advancements WHERE title_key = ?");
                        checkStatement.setString(1, key);
                        ResultSet checkResultSet = checkStatement.executeQuery();

                        String playerName = "None";
                        boolean achieved = false;
                        String title = jsonObject.get(key).getAsString();
                        String descriptionKey = key.replace("title", "description");
                        String description = jsonObject.get(descriptionKey).getAsString();
                        if (!checkResultSet.next()) {
                            // "title_key" がテーブル内に存在しない場合のみ新しい行を挿入
                            statement.setString(1, playerName);
                            statement.setBoolean(2, achieved);
                            statement.setString(3, title);
                            statement.setString(4, key);
                            statement.setString(5, description);
                            statement.setString(6, descriptionKey);
                            statement.addBatch(); // バッチ処理に追加
                        }
                    }
                }
                statement.executeBatch(); // バッチ処理を実行
            }
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
                String title = resultSet.getString("title");
                String titleKey = resultSet.getString("title_key");
                String description = resultSet.getString("description");
                String descriptionKey = resultSet.getString("description_key");
                boolean achieved = resultSet.getBoolean("achieved");

                completedAdvancementsData.add(new PlayerAdvancementData(playerName, title, titleKey, description, descriptionKey, achieved));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return completedAdvancementsData;
    }

    // データベースから未達成の進捗データを取得するメソッド
    private List<PlayerAdvancementData> getUncompletedAdvancementsDataFromDatabase() {
        List<PlayerAdvancementData> completedAdvancementsData = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_advancements WHERE achieved = ?")) {
            stmt.setBoolean(1, false);
            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                String playerName = resultSet.getString("player_name");
                String title = resultSet.getString("title");
                String titleKey = resultSet.getString("title_key");
                String description = resultSet.getString("description");
                String descriptionKey = resultSet.getString("description_key");
                boolean achieved = resultSet.getBoolean("achieved");

                completedAdvancementsData.add(new PlayerAdvancementData(playerName, title, titleKey, description, descriptionKey, achieved));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return completedAdvancementsData;
    }

    // データをHTMLファイルに出力するメソッド
    public void outputCompletedAdvancementsToHTML() {
        // 絶対パスの生成
        File dataFolder = plugin.getDataFolder();
        String absolutePath = dataFolder.getAbsolutePath();

        // 達成済みのデータ
        List<PlayerAdvancementData> completedAdvancementDataList = getCompletedAdvancementsDataFromDatabase();
        File completedFile = new File(absolutePath + File.separator + "index.html");

        try (FileWriter writer = new FileWriter(completedFile, StandardCharsets.UTF_8)) {
            writer.write("<html lang='ja'>");
            writer.write("<head><meta charset='UTF-8'><title>Completed Advancements</title></head>");
            writer.write("<body>");

            // 達成済みのデータへのリンクをページの上部に追加
            writer.write("<p><a href='index2.html'>未達成のデータを見る</a></p>");

            writer.write("<h1>Completed Advancements</h1>");
            writer.write("<ul>");
            for (PlayerAdvancementData advancementData : completedAdvancementDataList) {
                String playerName = advancementData.getPlayerName();
                String title = advancementData.getTitle();
                String description = advancementData.getDescription();
                writer.write("<li>" + playerName + ": " + title + "（" + description + "）</li>");
            }
            writer.write("</ul>");
            writer.write("</body>");
            writer.write("</html>");

        } catch (IOException e) {
            e.printStackTrace();
        }

        // 未達成のデータ
        List<PlayerAdvancementData> unCompletedAdvancementDataList = getUncompletedAdvancementsDataFromDatabase();
        File unCompletedFile = new File(absolutePath + File.separator + "index2.html");

        try (FileWriter writer = new FileWriter(unCompletedFile, StandardCharsets.UTF_8)) {
            writer.write("<html lang='ja'>");
            writer.write("<head><meta charset='UTF-8'><title>UnCompleted Advancements</title></head>");
            writer.write("<body>");

            // 未達成のデータへのリンクをページの上部に追加
            writer.write("<p><a href='index.html'>達成済みのデータを見る</a></p>");

            writer.write("<h1>UnCompleted Advancements</h1>");
            writer.write("<ul>");
            for (PlayerAdvancementData advancementData : unCompletedAdvancementDataList) {
                String playerName = advancementData.getPlayerName();
                String title = advancementData.getTitle();
                String description = advancementData.getDescription();
                writer.write("<li>" + playerName + ": " + title + "（" + description + "）</li>");
            }
            writer.write("</ul>");
            writer.write("</body>");
            writer.write("</html>");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // title_keyが指定された値のレコードのachievedをtrueに更新するメソッド
    private void updateAchievedByKey(String titleKey, String playerName) throws SQLException {
        String updateSql = "UPDATE player_advancements SET achieved = ?, player_name = ? WHERE title_key = ?";
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            // 指定されたtitle_keyを持つレコードがあればachievedをtrueに更新
            boolean achieved = true;
            statement.setBoolean(1, achieved);
            statement.setString(2, playerName);
            statement.setString(3, titleKey);

            int updatedRows = statement.executeUpdate();

            if (updatedRows > 0) {
                plugin.getLogger().log(Level.INFO,"Achievement updated successfully.");
            } else {
                plugin.getLogger().log(Level.INFO,"No matching achievement found for the given title_key.");
            }
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
            plugin.getLogger().log(Level.INFO,"Tables created successfully.");
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

        String insertSql = "INSERT OR IGNORE INTO player_criteria (player_name, advancement_name, criterion_name, achieved) VALUES (?, ?, ?, ?)";
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


    // 誰かが進捗の条件を達成した場合にワールド内にいる全員に同じ進捗の条件を達成したことにします
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

    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) throws SQLException {
        String advancementName = event.getAdvancement().getKey().getKey();
        String playerName = event.getPlayer().getName();

        // advancementNameが"recipes"から始まる場合はそのままreturnする
        if (advancementName.startsWith("recipes")) {
            return;
        }

        String titleKey = convertAdvancementKey(advancementName);
        updateAchievedByKey(titleKey, playerName);
    }

    public void onPlayerAdvancementCriterionGrant(PlayerAdvancementCriterionGrantEvent event) {
        String playerName = event.getPlayer().getName();
        String advancementName = event.getAdvancement().getKey().getKey();
        String advancementKey = event.getAdvancement().getKey().getKey();
        String criterionName = event.getCriterion();

        // advancementNameが"recipes"から始まる場合はそのままreturnする
        if (advancementName.startsWith("recipes")) {
            return;
        }

        advancementName = convertAdvancementKey(advancementName);
        // データベースから該当するAdvancementとCriterionのデータを取得
        PlayerCriterionData criterionData = getCriterionData(playerName, advancementName, criterionName);

        if (criterionData == null) {
            criterionData = new PlayerCriterionData(playerName, advancementName, criterionName, true);
            insertCriterionData(criterionData);
        }

        setAdvancementForAllPlayers(advancementKey, criterionName);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Component.text(event.getPlayer().getName() + "が進捗[" + advancementName
                    + "の[" + criterionName + "]条件を達成しました。", NamedTextColor.GREEN));
        }
    }
}