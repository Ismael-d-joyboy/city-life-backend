package ci.abidjan.citylife.player;

public record HospitalCareResponse(
        Integer health,
        Integer stamina,
        Integer hunger,
        Integer thirst,
        Long feeCharged,
        Long cashBalance,
        Long bankBalance,
        Integer hospitalVisits,
        String message
) {}
