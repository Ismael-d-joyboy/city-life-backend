package ci.abidjan.citylife.economy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record JobPayoutRequest(
        @NotNull(message = "L'identifiant du personnage est obligatoire.")
        UUID characterId,

        @NotNull(message = "Le type de tâche métier est obligatoire.")
        JobActionType actionType,

        @NotBlank(message = "La clé d'idempotence est obligatoire.")
        @Size(max = 100)
        String idempotencyKey
) {
    public enum JobActionType {
        TAXI_RIDE(2500L, 25L, "Course Taxi Compteur terminée"),
        MANAWA_BAG(1500L, 15L, "Livraison sac de ciment (Manawa)"),
        MANAWA_BONUS_5(2500L, 30L, "Bonus chef de chantier (5 sacs Manawa)"),
        DELIVERY_DROP(3000L, 30L, "Livraison Express Abidjan terminée"),
        GBAKA_STOP(2000L, 20L, "Arrêt ligne Gbaka / Woro-woro desservi"),
        GBAKA_TOUR_BONUS(3500L, 30L, "Pourboire tournée Gbaka sans accident"),
        CONTRABAND_DELIVERY(6500L, 50L, "Livraison colis non déclaré (Port de Vridi)");

        public final long rewardCash;
        public final long rewardXp;
        public final String label;

        JobActionType(long rewardCash, long rewardXp, String label) {
            this.rewardCash = rewardCash;
            this.rewardXp = rewardXp;
            this.label = label;
        }
    }
}