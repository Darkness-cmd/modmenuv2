package com.desafio.menu.data;

/** Lógica compartida cliente/servidor. */
public final class QuestLogic {
    public enum State { LOCKED, ACTIVE, READY, CLAIMED }

    private QuestLogic() {}

    public static State state(PlayerData p, Content.Quest q) {
        if (p.claimed.contains(q.id)) return State.CLAIMED;
        for (String r : q.requires) if (!p.claimed.contains(r)) return State.LOCKED;
        return p.progress.getOrDefault(q.id, 0) >= q.goal ? State.READY : State.ACTIVE;
    }

    public static boolean loreUnlocked(PlayerData p, Content.Lore l) {
        return l.unlockedBy == null || l.unlockedBy.isBlank() || p.claimed.contains(l.unlockedBy);
    }

    public static boolean discovered(PlayerData p, Content.Mob m) {
        return "always".equalsIgnoreCase(m.discover) || p.discovered.contains(m.id);
    }
}
