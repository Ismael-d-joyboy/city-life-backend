package ci.abidjan.citylife.tutorial;

public record TutorialStatusResponse(
        boolean tutorialCompleted,
        int tutorialStep,
        int totalSteps,
        String tutorialCompletedAt,
        boolean rewardGranted,
        long rewardCash,
        long rewardXp,
        long cashBalance,
        long bankBalance,
        long totalXp,
        String message
) {}
