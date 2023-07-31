package com.comorichico.minecraftadvancementsync;

public class PlayerCriteriaData {
    private final String playerName;
    private final String title;
    private final String advancementName;
    private final String criterionName;
    private final boolean achieved;

    public PlayerCriteriaData(String playerName, String title, String advancementName, String criterionName, boolean achieved) {
        this.playerName = playerName;
        this.title = title;
        this.advancementName = advancementName;
        this.criterionName = criterionName;
        this.achieved = achieved;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getTitle() {
        return title;
    }

    public String getAdvancementName() {
        return advancementName;
    }

    public String getCriterionName() {
        return criterionName;
    }

    public boolean isAchieved() {
        return achieved;
    }
}