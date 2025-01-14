package base.gameobjects;

import base.Game;
import base.graphicsservice.Rectangle;
import base.graphicsservice.RenderHandler;
import base.graphicsservice.Sprite;

import static base.constants.Constants.ZOOM;

public class Flower implements GameObject{

    private final transient Sprite sprite;
    private final int xPosition;
    private final int yPosition;
    private final String mapName;

    private static final int GROWING_LIMIT = 3000;
    private int currentTick;

    public Flower(Sprite sprite, int xPosition, int yPosition, String mapName) {
        this.sprite = sprite;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.mapName = mapName;
    }

    @Override
    public void render(RenderHandler renderer, int zoom) {
        renderer.renderSprite(sprite, xPosition, yPosition, ZOOM, false);
    }

    @Override
    public void update(Game game) {
        currentTick++;
        if (currentTick >= GROWING_LIMIT) {
            game.getGameMap(mapName).removeObject(this);
        }
    }

    @Override
    public int getLayer() {
        return 1;
    }

    @Override
    public boolean handleMouseClick(Rectangle mouseRectangle, Rectangle camera, int zoom, Game game) {
        return false;
    }

    @Override
    public Rectangle getRectangle() {
        return null;
    }

    public int getxPosition() {
        return xPosition;
    }

    public int getyPosition() {
        return yPosition;
    }

    public String getMapName() {
        return mapName;
    }

    public int getCurrentTick() {
        return currentTick;
    }
}
