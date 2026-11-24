package ci.abidjan.citylife.combat;

public record PoliceFineResponse(
        long finePaidFcfa,
        long cashBalance,
        long bankBalance,
        long totalFinesPaid,
        String message
) {
}
