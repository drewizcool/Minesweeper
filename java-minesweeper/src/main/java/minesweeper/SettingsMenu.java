package minesweeper;

public class SettingsMenu {
    public final Button button1;
    public final Button button2;
    public final Button button3;
    public final Button button4;
    public final Button button5;
    public final Button button6;
    public final Button button7;
    public final Button[] buttons;

    public SettingsMenu(Grid grid, ImageAssets images) {
        int baseX = grid.W - 152;
        int originY = grid.originY;

        button1 = new Button(new int[]{baseX, originY + 8},  163, 25, images.b1,      null, 1);
        button2 = new Button(new int[]{baseX, originY + 37}, 163, 25, images.b2,      null, 2);
        button3 = new Button(new int[]{baseX, originY + 66}, 163, 25, images.b3,      null, 3);
        button4 = new Button(new int[]{baseX, originY + 95}, 163, 25, images.i1,      null, 4);
        button5 = new Button(new int[]{baseX, originY + 124},163, 25, images.i2,      null, 5);
        button6 = new Button(new int[]{baseX, originY + 153},163, 25, images.expert,  null, 6);
        button7 = new Button(new int[]{baseX, originY + 182},163, 25, images.dungeon, null, 9);

        buttons = new Button[]{button1, button2, button3, button4, button5, button6, button7};
    }
}
