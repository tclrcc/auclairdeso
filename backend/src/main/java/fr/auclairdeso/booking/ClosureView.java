package fr.auclairdeso.booking;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

record ClosureView(Long id, LocalDate firstDay, LocalDate lastDay, @Nullable String reason) {
}
