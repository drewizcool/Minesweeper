package minesweeper;

/**
 * Direct port of the Python SettingsMenu class.
 */
public class SettingsMenu {
    public final Button button1;
    public final Button button2;
    public final Button button3;
    public final Button button4;
    public final Button button5;
    public final Button button6;
    public final Button[] buttons;

    public SettingsMenu(Grid grid) {
        int baseX = grid.W - 152;
        int originY = Minesweeper.ORIGIN_Y;

        button1 = new Button(new int[]{baseX, originY + 8},  163, 25, Minesweeper.B1_image,  null, 1);
        button2 = new Button(new int[]{baseX, originY + 37}, 163, 25, Minesweeper.B2_image,  null, 2);
        button3 = new Button(new int[]{baseX, originY + 66}, 163, 25, Minesweeper.B3_image,  null, 3);
        button4 = new Button(new int[]{baseX, originY + 95}, 163, 25, Minesweeper.I1_image,  null, 4);
        button5 = new Button(new int[]{baseX, originY + 124},163, 25, Minesweeper.I2_image,  null, 5);
        button6 = new Button(new int[]{baseX, originY + 153},163, 25, Minesweeper.E_image,   null, 6);

        buttons = new Button[]{button1, button2, button3, button4, button5, button6};
    }
}
