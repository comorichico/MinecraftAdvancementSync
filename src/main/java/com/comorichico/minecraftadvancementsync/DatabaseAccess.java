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
import java.util.Objects;
import java.util.logging.Level;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseAccess {

    private static final String pathName = "plugins/MinecraftAdvancementSync";
    private static final String DB_URL = "jdbc:sqlite:" + pathName + "/database.db"; // データベースファイルのパスを指定

    private final JavaPlugin plugin;

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

                String INSERT_ADVANCEMENT_SQL =
                        "INSERT INTO player_advancements "
                                + "(player_name, achieved, title, title_key, description, description_key) "
                                + "VALUES (?, ?, ?, ?, ?, ?)";
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

                PreparedStatement stmt = connection.prepareStatement("SELECT title_key, title FROM player_advancements");
                ResultSet resultSet = stmt.executeQuery();

                String INSERT_CRITERIA_SQL =
                        "INSERT INTO player_criteria "
                                + "(player_name, title, advancement_name, criterion_name, achieved) "
                                + "VALUES (?, ?, ?, ?, ?)";
                PreparedStatement stmt2 = connection.prepareStatement(INSERT_CRITERIA_SQL);
                while (resultSet.next()) {

                    String titleKey = resultSet.getString("title_key");
                    titleKey = titleKey.replace("advancements.", "");
                    titleKey = titleKey.replace(".title", "");
                    titleKey = titleKey.replace(".", "/");
                    String title = resultSet.getString("title");

                    Advancement advancement = Bukkit.getAdvancement(NamespacedKey.minecraft(titleKey));

                    for(String criteria: Objects.requireNonNull(advancement).getCriteria()){
                        stmt2.setString(1,"None");
                        stmt2.setString(2,title);
                        stmt2.setString(3,advancement.getKey().getKey());
                        stmt2.setString(4,criteria);
                        stmt2.setBoolean(5,false);
                        stmt2.addBatch(); // バッチ処理に追加
                    }
                }
                stmt2.executeBatch(); // バッチ処理を実行
            }
        } catch (SQLException ignored) {

        }
    }

    // データベースから達成済み進捗データを取得するメソッド
    private List<PlayerAdvancementData> getCompletedAdvancementsDataFromDatabase() {
        List<PlayerAdvancementData> completedAdvancementsData = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DB_URL)){
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_advancements WHERE achieved = ?");
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
        } catch (SQLException ignored) {
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
        } catch (SQLException ignored) {
        }

        return completedAdvancementsData;
    }

    // データベースから達成済み条件データを取得するメソッド
    private List<PlayerCriteriaData> getCompletedCriteriaDataFromDatabase() {
        List<PlayerCriteriaData> completedACriterionData = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DB_URL)){
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_criteria WHERE achieved = ?");
            stmt.setBoolean(1, true);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                String playerName = resultSet.getString("player_name");
                String title = resultSet.getString("title");
                String advancementName = resultSet.getString("advancement_name");
                String criterionName = resultSet.getString("criterion_name");
                boolean achieved = resultSet.getBoolean("achieved");

                completedACriterionData.add(new PlayerCriteriaData(playerName, title, advancementName, criterionName, achieved));
            }
        } catch (SQLException ignored) {
        }

        return completedACriterionData;
    }

    // データベースから未達成の条件データを取得するメソッド
    private List<PlayerCriteriaData> getUnCompletedCriteriaDataFromDatabase() {
        List<PlayerCriteriaData> completedACriterionData = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DB_URL)){
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_criteria WHERE achieved = ?");
            stmt.setBoolean(1, false);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                String playerName = resultSet.getString("player_name");
                String title = resultSet.getString("title");
                String advancementName = resultSet.getString("advancement_name");
                String criterionName = resultSet.getString("criterion_name");
                boolean achieved = resultSet.getBoolean("achieved");

                completedACriterionData.add(new PlayerCriteriaData(playerName, title, advancementName, criterionName, achieved));
            }
        } catch (SQLException ignored) {
        }

        return completedACriterionData;
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

            // 各データへのリンクをページの上部に追加
            writer.write("<p><a href='index.html'>達成済み進捗のデータを見る</a></p>");
            writer.write("<p><a href='index2.html'>未達成進捗のデータを見る</a></p>");
            writer.write("<p><a href='index3.html'>達成済み条件のデータを見る</a></p>");
            writer.write("<p><a href='index4.html'>未達成条件のデータを見る</a></p>");

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

        } catch (IOException ignored) {
        }

        // 未達成のデータ
        List<PlayerAdvancementData> unCompletedAdvancementDataList = getUncompletedAdvancementsDataFromDatabase();
        File unCompletedFile = new File(absolutePath + File.separator + "index2.html");

        try (FileWriter writer = new FileWriter(unCompletedFile, StandardCharsets.UTF_8)) {
            writer.write("<html lang='ja'>");
            writer.write("<head><meta charset='UTF-8'><title>UnCompleted Advancements</title></head>");
            writer.write("<body>");

            // 各データへのリンクをページの上部に追加
            writer.write("<p><a href='index.html'>達成済み進捗のデータを見る</a></p>");
            writer.write("<p><a href='index2.html'>未達成進捗のデータを見る</a></p>");
            writer.write("<p><a href='index3.html'>達成済み条件のデータを見る</a></p>");
            writer.write("<p><a href='index4.html'>未達成条件のデータを見る</a></p>");

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

        } catch (IOException ignored) {
        }

        // 達成済み条件のデータ
        List<PlayerCriteriaData> completedCriteriaDataList = getCompletedCriteriaDataFromDatabase();
        File completedCriteriaFile = new File(absolutePath + File.separator + "index3.html");

        try (FileWriter writer = new FileWriter(completedCriteriaFile, StandardCharsets.UTF_8)) {
            writer.write("<html lang='ja'>");
            writer.write("<head><meta charset='UTF-8'><title>Completed Criteria</title></head>");
            writer.write("<body>");

            // 各データへのリンクをページの上部に追加
            writer.write("<p><a href='index.html'>達成済み進捗のデータを見る</a></p>");
            writer.write("<p><a href='index2.html'>未達成進捗のデータを見る</a></p>");
            writer.write("<p><a href='index3.html'>達成済み条件のデータを見る</a></p>");
            writer.write("<p><a href='index4.html'>未達成条件のデータを見る</a></p>");

            writer.write("<h1>Completed Criteria</h1>");
            writer.write("<ul>");
            for (PlayerCriteriaData criteriaData : completedCriteriaDataList) {
                String playerName = criteriaData.getPlayerName();
                String title = criteriaData.getTitle();
                String criterionName = criteriaData.getCriterionName();
                writer.write("<li>" + playerName + ": " + title + "（" + criterionName + "）</li>");
            }
            writer.write("</ul>");
            writer.write("</body>");
            writer.write("</html>");

        } catch (IOException ignored) {
        }

        // 未達成条件のデータ
        List<PlayerCriteriaData> unCompletedCriteriaDataList = getUnCompletedCriteriaDataFromDatabase();
        File unCompletedCriteriaFile = new File(absolutePath + File.separator + "index4.html");

        try (FileWriter writer = new FileWriter(unCompletedCriteriaFile, StandardCharsets.UTF_8)) {
            writer.write("<html lang='ja'>");
            writer.write("<head><meta charset='UTF-8'><title>UnCompleted Criteria</title></head>");
            writer.write("<body>");

            // 各データへのリンクをページの上部に追加
            writer.write("<p><a href='index.html'>達成済み進捗のデータを見る</a></p>");
            writer.write("<p><a href='index2.html'>未達成進捗のデータを見る</a></p>");
            writer.write("<p><a href='index3.html'>達成済み条件のデータを見る</a></p>");
            writer.write("<p><a href='index4.html'>未達成条件のデータを見る</a></p>");

            writer.write("<h1>UnCompleted Criteria</h1>");
            writer.write("<ul>");
            for (PlayerCriteriaData criteriaData : unCompletedCriteriaDataList) {
                String playerName = criteriaData.getPlayerName();
                String title = criteriaData.getTitle();
                String criterionName = criteriaData.getCriterionName();
                writer.write("<li>" + playerName + ": " + title + "（" + criterionName + "）</li>");
            }
            writer.write("</ul>");
            writer.write("</body>");
            writer.write("</html>");

        } catch (IOException ignored) {
        }
    }

    // title_keyが指定された値のレコードのachievedをtrueに更新するメソッド
    private void updatePlayerAdvancements(String titleKey, String playerName) throws SQLException {
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

    private void updatePlayerCriteria(String titleKey, String criterionName, String playerName) throws SQLException {
        String updateSql = "UPDATE player_criteria SET achieved = ?, player_name = ? WHERE advancement_name = ? and criterion_name = ? and player_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            boolean achieved = true;
            statement.setBoolean(1, achieved);
            statement.setString(2, playerName);
            statement.setString(3, titleKey);
            statement.setString(4, criterionName);
            statement.setString(5, "None");

            int updatedRows = statement.executeUpdate();

            if (updatedRows > 0) {
                plugin.getLogger().log(Level.INFO,"Achievement updated successfully.");
            } else {
                plugin.getLogger().log(Level.INFO,"No matching achievement found for the given advancement_name.");
            }
        }
    }

    // テーブルを作成するメソッド
    private void createTables() {
        String CREATE_ADVANCEMENTS_TABLE_SQL =
                "CREATE TABLE IF NOT EXISTS player_advancements ("
                        + "id INTEGER PRIMARY KEY, "
                        + "player_name TEXT, "
                        + "achieved BOOLEAN,"
                        + "title TEXT, "
                        + "title_key TEXT UNIQUE, "
                        + "description TEXT, "
                        + "description_key TEXT"
                        + ")";
        String CREATE_CRITERIA_TABLE_SQL =
                "CREATE TABLE IF NOT EXISTS player_criteria ("
                        + "id INTEGER PRIMARY KEY, "
                        + "player_name TEXT, "
                        + "title TEXT, "
                        + "advancement_name TEXT, "
                        + "criterion_name TEXT, "
                        + "achieved BOOLEAN,"
                        + "UNIQUE(advancement_name, criterion_name)"
                        + ")";
        try {
            Statement statement = connection.createStatement();
            // Advancement用のテーブルを作成
            statement.executeUpdate(CREATE_ADVANCEMENTS_TABLE_SQL);
            // Criterion用のテーブルを作成
            statement.executeUpdate(CREATE_CRITERIA_TABLE_SQL);
            plugin.getLogger().log(Level.INFO,"Tables created successfully.");
        } catch (SQLException ignored) {
        }
    }

    // データベースへの接続をクローズするメソッド
    public void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
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
                    // 未達成のCriteriaだった場合だけ達成する
                    for (String criteria: player.getAdvancementProgress(advancement).getRemainingCriteria()){
                        if (criteria.equals(criterion_name)){
                            player.getAdvancementProgress(advancement).awardCriteria(criterion_name);
                        }
                    }
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
        Player player = event.getPlayer();

        // advancementNameが"recipes"から始まる場合はそのままreturnする
        if (advancementName.startsWith("recipes")) {
            return;
        }

        String titleKey = convertAdvancementKey(advancementName);
        updatePlayerAdvancements(titleKey, playerName);

        // 進捗を達成した場合、残りのCriteriaも全部達成したことにする
        Advancement advancement = Bukkit.getAdvancement(NamespacedKey.minecraft(advancementName));
        if (advancement != null) {
            // 未達成のCriteriaだった場合だけ達成する
            for (String criteria: player.getAdvancementProgress(advancement).getRemainingCriteria()){
                player.getAdvancementProgress(advancement).awardCriteria(criteria);
            }
        }
    }

    public void onPlayerAdvancementCriterionGrant(PlayerAdvancementCriterionGrantEvent event) throws SQLException {
        String playerName = event.getPlayer().getName();
        String advancementName = convertAdvancementKey(event.getAdvancement().getKey().getKey());
        String advancementKey = event.getAdvancement().getKey().getKey();
        String criterionName = event.getCriterion();

        // advancementKeyが"recipes"から始まる場合はそのままreturnする
        if (advancementKey.startsWith("recipes")) {
            return;
        }

        updatePlayerCriteria(advancementKey, criterionName, playerName);

        setAdvancementForAllPlayers(advancementKey, criterionName);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Component.text(event.getPlayer().getName() + "が進捗[" + advancementName
                    + "の[" + criterionName + "]条件を達成しました。", NamedTextColor.GREEN));
        }
    }

    // 達成済みデータをデータベースと同期する
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<PlayerCriteriaData> completedCriteriaDataList = getCompletedCriteriaDataFromDatabase();
        for(PlayerCriteriaData playerCriteriaData: completedCriteriaDataList) {
            String advancementName = playerCriteriaData.getAdvancementName();
            String criterionName = playerCriteriaData.getCriterionName();
            // Advancementのキーを指定してAdvancementオブジェクトを取得します
            Advancement advancement = Bukkit.getAdvancement(NamespacedKey.minecraft(advancementName)); // advancement_keyは対象のAdvancementのキーに置き換えてください
            // Advancementがnullでない場合は、そのAdvancementをプレイヤーに設定します
            if (advancement != null) {
                for (String criteria: player.getAdvancementProgress(advancement).getRemainingCriteria()){
                    if (criteria.equals(criterionName)){
                        player.getAdvancementProgress(advancement).awardCriteria(criterionName);
                    }
                }
            }
        }
    }
}