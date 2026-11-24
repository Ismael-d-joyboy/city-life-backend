package ci.abidjan.citylife.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsumeItemRequest(
        @NotBlank @Size(max = 60) String itemCode,
        @Min(1) int quantity
) {}
