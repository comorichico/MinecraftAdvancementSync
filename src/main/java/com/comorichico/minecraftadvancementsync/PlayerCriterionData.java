package com.comorichico.minecraftadvancementsync;

public class PlayerCriterionData {
    private String playerName;
    private String advancementName;
    private String criterionName;
    private boolean achieved;

    public PlayerCriterionData(String playerName, String advancementName, String criterionName, boolean achieved) {
        this.playerName = playerName;
        this.advancementName = advancementName;
        this.criterionName = criterionName;
        this.achieved = achieved;
    }

    public String getPlayerName() {
        return playerName;
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