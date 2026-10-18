import betteradvancements.common.gui.OldScreenVisibility;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.tools.ToolProvider;

/** Runs the original StatFileWriter methods, verbatim, against the port's discovery rules. */
public final class LegacyReferenceCheck {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        String reference = Files.readString(root.resolve("build/classic-reference/StatFileWriter.java.reference"));
        String source = """
            public class LegacyStatOracle {
                public static class Achievement {
                    public Achievement parentAchievement;
                    public int value;
                    public Achievement(Achievement parent) { parentAchievement = parent; }
                }
                public int readStat(Achievement a) { return a.value; }
            """ + method(reference, "public boolean hasAchievementUnlocked(")
                 + method(reference, "public boolean canUnlockAchievement(")
                 + method(reference, "public int func_150874_c(") + "}";
        Path output = root.resolve("build/legacy-layout-check/reference");
        Files.createDirectories(output);
        Path file = output.resolve("LegacyStatOracle.java");
        Files.writeString(file, source);
        if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", output.toString(), file.toString()) != 0)
            throw new AssertionError("Could not compile the original source methods");
        int checks = 0;
        try (var loader = new URLClassLoader(new java.net.URL[]{output.toUri().toURL()})) {
            Class<?> oracleClass = loader.loadClass("LegacyStatOracle");
            Class<?> nodeClass = loader.loadClass("LegacyStatOracle$Achievement");
            var construct = nodeClass.getConstructor(nodeClass);
            var value = nodeClass.getField("value");
            var unlock = oracleClass.getMethod("canUnlockAchievement", nodeClass);
            var depth = oracleClass.getMethod("func_150874_c", nodeClass);
            var oracle = oracleClass.getConstructor().newInstance();
            for (int graphSeed = 0; graphSeed < 12; graphSeed++) {
                var random = new Random(graphSeed);
                Map<String, String> parents = new LinkedHashMap<>();
                Object[] nodes = new Object[10];
                for (int i = 0; i < nodes.length; i++) {
                    int parent = i == 0 ? -1 : graphSeed == 0 ? i - 1 : random.nextInt(i + 1) - 1;
                    parents.put("node" + i, parent < 0 ? null : "node" + parent);
                    nodes[i] = construct.newInstance(parent < 0 ? null : nodes[parent]);
                }
                for (int mask = 0; mask < 1 << nodes.length; mask++) {
                    Set<String> completed = new HashSet<>();
                    for (int i = 0; i < nodes.length; i++) {
                        boolean done = (mask & (1 << i)) != 0;
                        value.setInt(nodes[i], done ? 1 : 0);
                        if (done) completed.add("node" + i);
                    }
                    for (int i = 0; i < nodes.length; i++) {
                        boolean expected = (boolean) unlock.invoke(oracle, nodes[i]);
                        int expectedDepth = Math.min(5, (int) depth.invoke(oracle, nodes[i]));
                        if (expected != OldScreenVisibility.canUnlock("node" + i, parents, completed)
                            || expectedDepth != OldScreenVisibility.unlockDepth("node" + i, parents, completed))
                            throw new AssertionError("Original source differs: graph=" + graphSeed + " state=" + mask + " node=" + i);
                        checks++;
                    }
                }
            }
        }
        System.out.println("PASS: " + checks + " node/progress cases match the original StatFileWriter methods, including out-of-order completion and revoked ancestors.");
    }

    private static String method(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) throw new AssertionError("Missing original method: " + signature);
        int brace = source.indexOf('{', start), depth = 1, end = brace + 1;
        while (depth > 0 && end < source.length()) {
            char c = source.charAt(end++);
            if (c == '{') depth++;
            else if (c == '}') depth--;
        }
        if (depth != 0) throw new AssertionError("Unterminated original method");
        return source.substring(start, end) + "\n";
    }
}
