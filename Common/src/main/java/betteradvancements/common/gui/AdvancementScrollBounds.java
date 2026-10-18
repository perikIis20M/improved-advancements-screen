package betteradvancements.common.gui;

/** The same axis bounds apply to dragging, restored positions and search navigation. */
final class AdvancementScrollBounds {
    private AdvancementScrollBounds() { }

    static int center(int min, int max, int viewport) {
        return (viewport - (max + min)) / 2;
    }

    static int constrain(double offset, int min, int max, int viewport) {
        if (max - min <= viewport) return center(min, max, viewport);
        return (int) Math.round(Math.clamp(offset, viewport - max, -min));
    }

    static int reveal(int offset, int start, int end, int min, int max, int viewport) {
        offset = constrain(offset, min, max, viewport);
        // Leave an already visible result where it is; pan only enough to bring a clipped one in.
        if (start + offset < 0) offset = -start;
        else if (end + offset > viewport) offset = viewport - end;
        return constrain(offset, min, max, viewport);
    }
}
