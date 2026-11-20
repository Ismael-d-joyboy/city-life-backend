package ci.abidjan.citylife.economy;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/economy")
@RequiredArgsConstructor
public class EconomyController {

    private final EconomyService economyService;

    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> deposit(
            Authentication authentication,
            @Valid @RequestBody BankOperationRequest request) {
        return ResponseEntity.ok(economyService.deposit(authentication.getName(), request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdraw(
            Authentication authentication,
            @Valid @RequestBody BankOperationRequest request) {
        return ResponseEntity.ok(economyService.withdraw(authentication.getName(), request));
    }

    @GetMapping("/transactions/{characterId}")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            Authentication authentication,
            @PathVariable UUID characterId) {
        return ResponseEntity.ok(economyService.getHistory(authentication.getName(), characterId));
    }
}