package ci.abidjan.citylife.player;

import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateAppearanceRequest(
        @Size(max = 30) String skinTone,
        @Size(max = 50) String hairStyle,
        @Size(max = 30) String hairColor,
        Map<String, Object> outfitData
) {}
