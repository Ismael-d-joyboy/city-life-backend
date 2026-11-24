package ci.abidjan.citylife.vehicles;

public record RefuelResponse(
        int fuelLevel,
        long chargedFcfa,
        long cashBalance,
        long bankBalance,
        String message
) {
}
