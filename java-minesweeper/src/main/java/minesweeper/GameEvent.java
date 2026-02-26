package minesweeper;

public class GameEvent {
    public enum EventType {
        MOUSE_DOWN, MOUSE_UP, MOUSE_MOVE, KEY_PRESS, KEY_RELEASE, QUIT
    }

    public final EventType type;
    public final int x;
    public final int y;
    public final int button;   // mouse button (1=left, 2=middle, 3=right)
    public final int keyCode;  // java.awt.event.KeyEvent key code

    public GameEvent(EventType type, int x, int y, int button, int keyCode) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.button = button;
        this.keyCode = keyCode;
    }

    // Convenience constructors
    public static GameEvent mouseDown(int x, int y, int button) {
        return new GameEvent(EventType.MOUSE_DOWN, x, y, button, 0);
    }

    public static GameEvent mouseUp(int x, int y, int button) {
        return new GameEvent(EventType.MOUSE_UP, x, y, button, 0);
    }

    public static GameEvent mouseMove(int x, int y) {
        return new GameEvent(EventType.MOUSE_MOVE, x, y, 0, 0);
    }

    public static GameEvent keyPress(int keyCode) {
        return new GameEvent(EventType.KEY_PRESS, 0, 0, 0, keyCode);
    }

    public static GameEvent keyRelease(int keyCode) {
        return new GameEvent(EventType.KEY_RELEASE, 0, 0, 0, keyCode);
    }

    public static GameEvent quit() {
        return new GameEvent(EventType.QUIT, 0, 0, 0, 0);
    }
}
