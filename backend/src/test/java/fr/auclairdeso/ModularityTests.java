package fr.auclairdeso;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(AuClairDeSoApplication.class);

    @Test
    void verifierModularStructure() {
        modules.verify();
    }
}
