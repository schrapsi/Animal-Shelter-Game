package base.gameobjects;

import base.Game;
import base.graphicsservice.Rectangle;
import base.graphicsservice.RenderHandler;
import base.graphicsservice.Sprite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static base.constants.Constants.TILE_SIZE;
import static base.constants.Constants.ZOOM;
import static base.constants.MapConstants.FOREST_MAP;

// TODO: extends Item? And refactor item
public class Feather implements GameObject {

    protected static final Logger logger = LoggerFactory.getLogger(Feather.class);

    public static final String ITEM_NAME = "feather";
    public static final int TILE_ID = 76;

    private transient Sprite sprite;
    private final int xPosition;
    private final int yPosition;
    private final Rectangle rectangle;
    private String mapName;

    public Feather(int xPosition, int yPosition, Sprite sprite) {
        this(xPosition, yPosition);
        this.sprite = sprite;
    }

    public Feather(int xPosition, int yPosition) {
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.rectangle = new Rectangle(xPosition, yPosition, TILE_SIZE, TILE_SIZE);
        mapName = FOREST_MAP;
    }

    @Override
    public void render(RenderHandler renderer, int zoom) {
        renderer.renderSprite(sprite, xPosition, yPosition, ZOOM, false);
    }

    @Override
    public void update(Game game) {

    }

    @Override
    public int getLayer() {
        return 1;
    }

    @Override
    public boolean handleMouseClick(Rectangle mouseRectangle, Rectangle camera, int zoom, Game game) {
        if (mouseRectangle.intersects(rectangle)) {
            logger.info("Feather is clicked");
            game.pickUpItem(ITEM_NAME, sprite, rectangle);
            return true;
        }
        return false;
    }

    @Override
    public Rectangle getRectangle() {
        return rectangle;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public int getPosition() {
        return xPosition;
    }

    public int getyPosition() {
        return yPosition;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }
}
