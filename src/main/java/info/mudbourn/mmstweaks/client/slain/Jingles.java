package info.mudbourn.mmstweaks.client.slain;

import info.mudbourn.mmstweaks.MmsTweaks;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

/**
 * The game over jingles, each with the second its tail falls silent and four headlines tied to it.
 */
public final class Jingles {

    public static final int MESSAGES = 4;

    private static final int JINGLE_COOLDOWN = 5;

    private static final int QUIP_COOLDOWN = 3;

    private static final List<Jingle> ALL = List.of(
        new Jingle("bf", 8.44F),
        new Jingle("cod", 12.77F),
        new Jingle("cvx", 8.64F),
        new Jingle("dmc3", 8.98F),
        new Jingle("ds3", 7.66F),
        new Jingle("fail", 6.88F),
        new Jingle("ff8", 9.50F),
        new Jingle("fnaf", 9.66F),
        new Jingle("gtav", 4.52F),
        new Jingle("mario", 5.35F),
        new Jingle("p5r", 11.92F),
        new Jingle("pico", 8.85F),
        new Jingle("re4", 8.90F),
        new Jingle("sonic", 12.61F),
        new Jingle("sotb2", 13.46F),
        new Jingle("undertale", 11.65F),
        new Jingle("zelda", 9.95F)
    );

    private static final Deque<Jingle> RECENT = new ArrayDeque<>();

    private static final Map<String, Deque<Integer>> RECENT_QUIPS = new HashMap<>();

    private static Jingle forced;

    private static int forcedQuip = -1;

    private Jingles() {}

    public static List<String> ids() {
        return ALL.stream().map(Jingle::id).toList();
    }

    // Makes the next death play this jingle with this quip.
    public static boolean force(String id, int quip) {
        forced = ALL.stream().filter(jingle -> jingle.id().equals(id)).findFirst().orElse(null);
        forcedQuip = quip;
        return forced != null;
    }

    // Picks a jingle that did not play in the last five deaths.
    public static Jingle pick(RandomSource random) {
        List<Jingle> open = ALL.stream().filter(jingle -> !RECENT.contains(jingle)).toList();
        Jingle picked = forced != null ? forced : open.get(random.nextInt(open.size()));
        forced = null;
        remember(RECENT, picked, JINGLE_COOLDOWN);
        return picked;
    }

    private static <T> void remember(Deque<T> recent, T value, int cooldown) {
        recent.addLast(value);
        if (recent.size() > cooldown) {
            recent.removeFirst();
        }
    }

    public record Jingle(String id, float tailSeconds) {

        public Identifier sound() {
            return MmsTweaks.id("gameover." + id);
        }

        // Picks a headline that did not show in this jingle's last three plays.
        public Component headline(RandomSource random, int nights) {
            Deque<Integer> recent = RECENT_QUIPS.computeIfAbsent(id, key -> new ArrayDeque<>());
            List<Integer> open = IntStream.range(0, MESSAGES).boxed().filter(index -> !recent.contains(index)).toList();
            int picked = forcedQuip >= 0 ? forcedQuip : open.get(random.nextInt(open.size()));
            forcedQuip = -1;
            remember(recent, picked, QUIP_COOLDOWN);
            return Component.translatable("mms_tweaks.slain." + id + "." + picked, nights);
        }
    }
}
