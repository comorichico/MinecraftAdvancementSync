package com.comorichico.minecraftadvancementsync;

public class PlayerAdvancementData {
    private final String playerName;
    private final String title;
    private final String titleKey;
    private final String description;
    private final String descriptionKey;
    private final boolean achieved;

    public PlayerAdvancementData(String playerName, String title, String titleKey, String description, String descriptionKey, boolean achieved) {
        this.playerName = playerName;
        this.title = title;
        this.titleKey = titleKey;
        this.description = description;
        this.descriptionKey = descriptionKey;
        this.achieved = achieved;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

}