package com.comorichico.minecraftadvancementsync;

public class PlayerAdvancementData {
    private String playerName;
    private String advancementName;
    private boolean achieved;

    public PlayerAdvancementData(String playerName, String advancementName, boolean achieved) {
        this.playerName = playerName;
        this.advancementName = advancementName;
        this.achieved = achieved;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getAdvancementName() {
        return advancementName;
    }

    public boolean isAchieved() {
        return achieved;
    }
}