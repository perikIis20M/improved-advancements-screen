package betteradvancements.common.gui;

import java.text.Normalizer;
import java.util.*;

/** Local search over only the entries the current advancement view has revealed. */
public final class AdvancementSearchIndex {
    public record Entry(String id, String title, String description, String iconName,
                        boolean revealed, boolean detailsVisible, boolean completed, float progress) { }
    private AdvancementSearchIndex() { }

    public static List<Entry> search(Collection<Entry> entries, String query) {
        String term = normalize(query);
        List<String> words = term.isEmpty() ? List.of() : List.of(term.split(" "));
        return entries.stream().filter(Entry::revealed).filter(entry -> {
            String haystack = normalize(entry.title() + " " + entry.iconName()
                + (entry.detailsVisible() ? " " + entry.description() : ""));
            return words.stream().allMatch(haystack::contains);
        }).sorted(Comparator.comparing(Entry::completed)
            .thenComparingInt(entry -> titleRank(entry.title(), term))
            .thenComparing(Comparator.comparingDouble((Entry entry) -> safeProgress(entry.progress())).reversed())
            .thenComparing(entry -> normalize(entry.title())).thenComparing(Entry::id)).toList();
    }

    private static int titleRank(String title, String query) {
        String normalized = normalize(title);
        return query.isEmpty() || normalized.equals(query) ? 0 : normalized.startsWith(query) ? 1 : normalized.contains(query) ? 2 : 3;
    }

    private static double safeProgress(float value) { return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0; }

    static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKD).replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
