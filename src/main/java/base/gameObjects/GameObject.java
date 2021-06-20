package base.gameObjects;

import base.Game;
import base.graphicsService.Rectangle;
import base.graphicsService.RenderHandler;

public interface GameObject {

    //should be called as often as possible
    void render(RenderHandler renderer, int xZoom, int yZoom);

    //called 60fps
    void update(Game game);

    int getLayer();

    boolean handleMouseClick(Rectangle mouseRectangle, Rectangle camera, int xZoom, int yZoom);
}
