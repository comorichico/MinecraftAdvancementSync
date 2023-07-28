package com.comorichico.minecraftadvancementsync;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AdvancementData {
    private JsonObject jsonObject;
    private String fileName;

    public AdvancementData(String fileName) {
        // コンストラクタでファイルパスを設定
        this.fileName = fileName;
        this.readJson();
    }

    // Setter メソッド
    public void setJsonObject(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    // keyを指定して日本語の値を取得するメソッド
    public String getJapaneseValue(String key) {
        if (jsonObject == null || key == null) {
            return null;
        }

        JsonElement value = jsonObject.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return key; // 元の key を返す
        }

        return value.getAsString();
    }

    // jsonファイルを読み込むメソッド
    public void readJson() {
        Gson gson = new Gson();

        InputStreamReader reader = new InputStreamReader(
                MinecraftAdvancementSync.class.getClassLoader().getResourceAsStream(fileName),
                StandardCharsets.UTF_8
        );
        JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
        setJsonObject(jsonObject);
    }
}