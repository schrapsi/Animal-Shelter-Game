package base.gameobjects;

import base.Game;
import base.constants.MapConstants;
import base.gameobjects.animals.Butterfly;
import base.graphicsservice.ImageLoader;
import base.graphicsservice.Rectangle;
import base.graphicsservice.RenderHandler;
import base.graphicsservice.Sprite;
import base.map.GameMap;
import base.map.MapTile;
import base.navigationservice.Direction;
import base.navigationservice.NavigationService;
import base.navigationservice.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

import static base.constants.ColorConstant.GREEN;
import static base.constants.Constants.*;
import static base.constants.FilePath.IMAGES_PATH;
import static base.gameobjects.AgeStage.ADULT;
import static base.gameobjects.AgeStage.BABY;
import static base.navigationservice.Direction.*;

public abstract class Animal implements GameObject {

    private Sprite previewSprite;
    private Sprite sprite;
    private AnimatedSprite animatedSprite = null;
    private final Rectangle animalRectangle;
    private final Random random;
    private final int tileSize;
    private String fileName;

    private Direction direction;
    private int movingTicks = 0;
    private transient Route route;
    private String currentMap;
    private String homeMap;
    private int speed;
    private String color;
    private String animalName;

    private AgeStage age;
    public static final int GROWING_UP_TIME = 200_000;
    private int currentAge;

    protected static final int MAX_HUNGER = 30_000;
    protected static final int MIN_HUNGER = 1;
    private int currentHunger;

    protected static final int MAX_THIRST = 25_000;
    protected static final int MIN_THIRST = 1;
    private int currentThirst;

    protected static final int MAX_ENERGY = 40_000;
    protected static final int MIN_ENERGY = 1;
    protected static final int SLEEPING_SPEED = 15;
    private int currentEnergy;

    protected static final Logger logger = LoggerFactory.getLogger(Animal.class);

    protected Animal(String animalName, int startX, int startY, int speed, int tileSize, int currentHunger, int currentThirst, int currentEnergy, AgeStage age) {
        this(animalName, startX, startY, speed, MapConstants.MAIN_MAP, tileSize, currentHunger, currentThirst, currentEnergy, age);
    }

    protected Animal(String animalName, int startX, int startY, int speed, String currentMap, int tileSize, int currentHunger, int currentThirst, int currentEnergy, AgeStage age) {
        this.animalName = animalName;
        this.tileSize = tileSize;
        this.currentMap = currentMap;
        this.speed = speed;
        this.currentHunger = currentHunger;
        this.currentThirst = currentThirst;
        this.currentEnergy = currentEnergy;
        this.age = age;
        homeMap = MapConstants.TOP_CENTER_MAP;

        setSprite();
        setPreviewSprite();

        direction = DOWN;
        updateDirection();
        animalRectangle = new Rectangle(startX, startY, 32, 32);
        animalRectangle.generateGraphics(1, GREEN);

        if (BABY.equals(age)) {
            this.speed--;
        } else {
            setCurrentAge(GROWING_UP_TIME);
        }

        route = new Route();
        random = new Random();
    }

    protected void setSprite() {
        sprite = ImageLoader.getAnimatedSprite(IMAGES_PATH + animalName + ".png", tileSize);
        if (sprite != null) {
            animatedSprite = (AnimatedSprite) sprite;
        }
    }

    protected void setPreviewSprite() {
        previewSprite = ImageLoader.getPreviewSprite(IMAGES_PATH + animalName + "-preview.png");
    }

    private void updateDirection() {
        int startSprite = getStartingSprite(direction);
        int endSprite = getEndSprite(direction);
        animatedSprite.setAnimationRange(startSprite, endSprite);
    }

    protected int getStartingSprite(Direction direction) {
        if (animatedSprite != null && direction != STAY) {
            if (direction.name().startsWith("EAT")) {
                if (animatedSprite.getSpritesSize() >= 28) {
                    return (direction.directionNumber - 5) * animatedSprite.getSpritesSize() / 4 + 3;
                } else {
                    return (direction.directionNumber - 5) * animatedSprite.getSpritesSize() / 4;
                }
            }
            if (direction.name().startsWith("SLEEP") || direction.name().startsWith("WAKEUP")) {
                return (direction.directionNumber - 9) * animatedSprite.getSpritesSize() / 4 + 7;
            }
            return direction.directionNumber * animatedSprite.getSpritesSize() / 4;
        }
        return 0;
    }

    protected int getEndSprite(Direction direction) {
        if (animatedSprite != null && direction != STAY) {
            if (direction.name().startsWith("EAT")) {
                if (animatedSprite.getSpritesSize() >= 28) {
                    return (direction.directionNumber - 5) * animatedSprite.getSpritesSize() / 4 + 6;
                } else {
                    return (direction.directionNumber - 5) * animatedSprite.getSpritesSize() / 4 + 2;
                }
            }
            if (direction.name().startsWith("SLEEP") || direction.name().startsWith("WAKEUP")) {
                return (direction.directionNumber - 9) * animatedSprite.getSpritesSize() / 4 + 10;
            }
            return direction.directionNumber * animatedSprite.getSpritesSize() / 4 + 2;
        }
        return 0;
    }

    @Override
    public void render(RenderHandler renderer, int xZoom, int yZoom) {
        int xForSprite = animalRectangle.getX() - (tileSize - 32);
        int yForSprite = animalRectangle.getY() - ((tileSize - 32) + ((tileSize - 32) / 2));
        if (BABY.equals(age)) {
            xZoom = 1;
            yZoom = 1;
        }
        if (animatedSprite != null) {
            renderer.renderSprite(animatedSprite, xForSprite, yForSprite, xZoom, yZoom, false);
        } else if (sprite != null) {
            renderer.renderSprite(sprite, animalRectangle.getX(), animalRectangle.getY(), xZoom, yZoom, false);
        } else {
            renderer.renderRectangle(animalRectangle, xZoom, yZoom, false);
        }
    }

    @Override
    public void update(Game game) {
        boolean isMoving = false;
        Direction nextDirection = direction;

        checkIfNeedToGoToDifferentLocation(game);

        if (movingTicks < 1) {
            if (!route.isEmpty()) {
                nextDirection = route.getNextStep();
                movingTicks = getMovingTickToAdjustPosition(nextDirection);
                logger.debug(String.format("Direction: %s, moving ticks: %d", direction.name(), movingTicks));
            } else {
                nextDirection = getRandomDirection();
                movingTicks = getRandomMovingTicks();
            }
            if (isOutSideOfMap(game.getGameMap(getCurrentMap()))) {
                moveAnimalToCenter(game.getGameMap(getCurrentMap()));
            }
        }

        handleMoving(game.getGameMap(currentMap), nextDirection);
        if (nextDirection != STAY || this instanceof Butterfly) {
            isMoving = true;
        }

        if (nextDirection != direction) {
            direction = nextDirection;
            updateDirection();
        }
        if (animatedSprite != null) {
            if (isMoving && (isFallingAsleep() || isWakingUp() || notRelatedToSleeping())) {
                animatedSprite.update(game);
            } else if (!isSleeping()) {
                animatedSprite.reset();
            }
        }

        checkPortals(game);

        movingTicks--;
        if (!isSleeping()) {
            eatAndDrinkIfNeeded(game);
            sleepIfNeeded(game);
        } else {
            handleSleeping();
        }

        if (BABY.equals(age)) {
            updateAge();
        }
    }

    protected void updateAge() {
        incrementAge();
        if (isTimeToGrowUp()) {
            setAge(ADULT);
            speed++;
        }
    }

    protected boolean isTimeToGrowUp() {
        return currentAge >= GROWING_UP_TIME;
    }

    protected void incrementAge() {
        currentAge++;
    }

    private void handleSleeping() {
        currentEnergy += SLEEPING_SPEED;
        if (currentEnergy >= MAX_ENERGY) {
            wakeUp();
        }
    }

    private void checkIfNeedToGoToDifferentLocation(Game game) {
        if (route.isEmpty() && getCurrentMap().startsWith("Bottom")) {
            route = game.calculateRouteToMap(this, NavigationService.getNextPortalToGetToCenter(getCurrentMap()));
        }
        if (route.isEmpty() && this instanceof Butterfly && MapConstants.HOME_MAPS.contains(getCurrentMap())) {
            route = game.calculateRouteToMap(this, NavigationService.getNextPortalToOutside(getCurrentMap()));
        }
    }

    private boolean notRelatedToSleeping() {
        return direction.directionNumber < 9;
    }

    private boolean isFallingAsleep() {
        return isSleeping() && animatedSprite.getCurrentSprite() < animatedSprite.getEndSprite();
    }

    private boolean isWakingUp() {
        return direction.name().startsWith("WAKEUP") && animatedSprite.getCurrentSprite() < animatedSprite.getEndSprite();
    }

    private void sleepIfNeeded(Game game) {
        decreaseEnergyLevel();

        if (!(this instanceof Butterfly) && isSleepy() && !isSleeping()) {
            if (route.isEmpty() && isTherePillow(game)) {
                sleep();
                return;
            }
            if (route.isEmpty()) {
                route = game.calculateRouteToPillow(this);
            }
            if (route.isEmpty()) {
                sleep();
            }
        }
    }

    private boolean isTherePillow(Game game) {
        for (MapTile pillow : game.getGameMap(currentMap).getPillows()) {
            if (animalRectangle.intersects(pillow)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSleepy() {
        return currentEnergy < MAX_ENERGY / 100 * 25;
    }

    private void sleep() {
        logger.info(String.format("%s will sleep now", animalName));
        if (direction == UP || direction == LEFT) {
            direction = SLEEP_LEFT;
        } else {
            direction = SLEEP_RIGHT;
        }
        updateDirection();
        movingTicks = MAX_ENERGY;
    }

    private void wakeUp() {
        currentEnergy = MAX_ENERGY;
        if (direction == SLEEP_LEFT) {
            direction = WAKEUP_LEFT;
        }
        if (direction == SLEEP_RIGHT) {
            direction = WAKEUP_RIGHT;
        }
        updateDirection();
        movingTicks = 3 * animatedSprite.getSpeed();
    }

    private void decreaseEnergyLevel() {
        if (!isSleeping() && currentEnergy > MIN_ENERGY) {
            currentEnergy--;
        }
        if (currentEnergy != 0 && currentEnergy % (MAX_ENERGY / 10) == 0) {
            logger.debug(String.format("Energy level for %s is %d percent", animalName, currentEnergy / (MAX_ENERGY / 100)));
        }
    }

    private boolean isSleeping() {
        return direction.name().startsWith("SLEEP");
    }

    private void eatAndDrinkIfNeeded(Game game) {
        decreaseHungerLevel();
        if (!(this instanceof Butterfly) && currentHunger < MAX_HUNGER / 100 * 25 && route.isEmpty()) {
            route = game.calculateRouteToFood(this);
        }

        decreaseThirstLevel();
        if (!(this instanceof Butterfly) && currentThirst < MAX_THIRST / 100 * 25 && route.isEmpty()) {
            route = game.calculateRouteToWater(this);
        }

        if (!(this instanceof Butterfly) && (isHungerLow() && checkForFood(game)) || (isThirstLow() && checkForWater(game))) {
            updateEatingDirection();
            movingTicks = getRandomMovingTicks();
        }
    }

    private void updateEatingDirection() {
        if (direction == UP) {
            direction = EAT_UP;
        }
        if (direction == DOWN) {
            direction = EAT_DOWN;
        }
        if (direction == LEFT) {
            direction = EAT_LEFT;
        }
        if (direction == RIGHT) {
            direction = EAT_RIGHT;
        }
        if (animatedSprite.getSpritesSize() < 28) {
            direction = STAY;
        }
        updateDirection();
    }

    private boolean isHungerLow() {
        return currentHunger < MAX_HUNGER / 100 * 70;
    }

    private boolean isThirstLow() {
        return currentThirst < MAX_THIRST / 100 * 70;
    }

    private void decreaseHungerLevel() {
        if (currentHunger > MIN_HUNGER) {
            currentHunger--;
        }
        if (currentHunger != 0 && currentHunger % (MAX_HUNGER / 10) == 0) {
            logger.debug(String.format("Hunger level for %s is %d percent", animalName, currentHunger / (MAX_HUNGER / 100)));
        }
        if (currentHunger < MAX_HUNGER / 100 * 10) {
            speed = 1;
        }
    }

    private void decreaseThirstLevel() {
        if (currentThirst > MIN_THIRST) {
            currentThirst--;
        }
        if (currentThirst != 0 && currentThirst % (MAX_THIRST / 10) == 0) {
            logger.debug(String.format("Thirst level for %s is %d percent", animalName, currentThirst / (MAX_THIRST / 100)));
        }
        if (currentThirst < MAX_THIRST / 100 * 10) {
            speed = 1;
        }
    }

    private boolean checkForFood(Game game) {
        for (Item item : game.getGameMap(currentMap).getItems()) {
            if (animalRectangle.intersects(item.getRectangle())) {
                logger.info(String.format("%s ate food", animalName));
                currentHunger = MAX_HUNGER;
                resetSpeedToDefault();
                game.getGameMap(currentMap).removeItem(item.getItemName(), item.getRectangle());
                logger.debug(String.format("Hunger level for %s is 100 percent", animalName));
                return true;
            }
        }
        for (FoodBowl bowl : game.getGameMap(currentMap).getFoodBowls()) {
            if (bowl.isFull() && animalRectangle.intersects(bowl.getRectangle())) {
                logger.info(String.format("%s ate food", animalName));
                currentHunger = MAX_HUNGER;
                resetSpeedToDefault();
                bowl.emptyBowl();
                logger.debug(String.format("Hunger level for %s is 100 percent", animalName));
                return true;
            }
        }
        return false;
    }

    private boolean checkForWater(Game game) {
        for (WaterBowl bowl : game.getGameMap(currentMap).getWaterBowls()) {
            if (bowl.isFull() && animalRectangle.intersects(bowl.getRectangle())) {
                logger.info(String.format("%s drank water", animalName));
                currentThirst = MAX_THIRST;
                resetSpeedToDefault();
                bowl.emptyBowl();
                logger.debug(String.format("Thirst level for %s is 100 percent", animalName));
                return true;
            }
        }
        return false;
    }

    protected void resetSpeedToDefault() {
        if (BABY.equals(age)) {
            setSpeed(2);
        } else {
            setSpeed(3);
        }
    }

    private void handleMoving(GameMap gameMap, Direction direction) {
        if (unwalkableInThisDirection(gameMap, direction)) {
            route = new Route();
            handleUnwalkable(direction);
            return;
        }

        switch (direction) {
            case LEFT:
                if (animalRectangle.getX() > 0 || nearPortal(gameMap.getPortals())) {
                    animalRectangle.setX(animalRectangle.getX() - speed);
                }
                break;
            case RIGHT:
                if (animalRectangle.getX() < (gameMap.getMapWidth() * TILE_SIZE - animalRectangle.getWidth()) * ZOOM || nearPortal(gameMap.getPortals())) {
                    animalRectangle.setX(animalRectangle.getX() + speed);
                }
                break;
            case UP:
                if (animalRectangle.getY() > 0 || nearPortal(gameMap.getPortals())) {
                    animalRectangle.setY(animalRectangle.getY() - speed);
                }
                break;
            case DOWN:
                if (animalRectangle.getY() < (gameMap.getMapHeight() * TILE_SIZE - animalRectangle.getHeight()) * ZOOM || nearPortal(gameMap.getPortals())) {
                    animalRectangle.setY(animalRectangle.getY() + speed);
                }
                break;
        }
    }

    private void handleUnwalkable(Direction direction) {
        movingTicks = 0;
        switch (direction) {
            case LEFT:
                animalRectangle.setX(animalRectangle.getX() + speed);
                break;
            case RIGHT:
                animalRectangle.setX(animalRectangle.getX() - speed);
                break;
            case UP:
                animalRectangle.setY(animalRectangle.getY() + speed);
                break;
            case DOWN:
                animalRectangle.setY(animalRectangle.getY() - speed);
                break;
        }
    }

    Direction getRandomDirection() {
        int result = random.nextInt(5);
        switch (result) {
            case 0:
                return DOWN;
            case 1:
                return LEFT;
            case 2:
                return UP;
            case 3:
                return RIGHT;
            default:
                return STAY;
        }
    }

    private int getRandomMovingTicks() {
        return random.nextInt(20) + 64;
    }

    protected int getMovingTickToAdjustPosition(Direction direction) {
        return Math.abs(NavigationService.getPixelsToAdjustPosition(direction, getCurrentX(), getCurrentY())) / speed;
    }

    private boolean unwalkableInThisDirection(GameMap gameMap, Direction direction) {
        int xPosition = animalRectangle.getX();
        int yPosition = animalRectangle.getY();

        List<MapTile> tilesOnLayer = gameMap.getTilesOnLayer(getLayer());

        switch (direction) {
            case LEFT:
                xPosition = xPosition - (speed + 1);
                break;
            case RIGHT:
                xPosition = xPosition + (speed + 1);
                break;
            case UP:
                yPosition = yPosition - (speed + 1);
                break;
            case DOWN:
                yPosition = yPosition + (speed + 1);
                break;
        }
        if (tilesOnLayer != null) {
            for (MapTile tile : tilesOnLayer) {
                if (animalRectangle.potentialIntersects(tile, xPosition, yPosition)) {
                    return true;
                }
            }
        }

        List<MapTile> portals = gameMap.getPortals();
        if (portals != null) {
            for (MapTile portal : portals) {
                if (this instanceof Butterfly && MapConstants.HOME_MAPS.contains(portal.getPortalDirection()) && animalRectangle.potentialIntersects(portal, xPosition, yPosition)) {
                    return true;
                }
                if (MapConstants.MAIN_MAP.equals(getCurrentMap()) && portal.getPortalDirection().startsWith("Bottom") && animalRectangle.potentialIntersects(portal, xPosition, yPosition)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean nearPortal(List<MapTile> portals) {
        for (MapTile portal : portals) {
            logger.debug(String.format("Portal X: %d player X: %d", portal.getX() * (TILE_SIZE * ZOOM), animalRectangle.getX()));
            int diffX = portal.getX() * (TILE_SIZE * ZOOM) - animalRectangle.getX();
            logger.debug(String.format("diff x: %d", diffX));
            int diffY = portal.getY() * (TILE_SIZE * ZOOM) - animalRectangle.getY();
            logger.debug(String.format("diff y: %d", diffY));
            if (Math.abs(diffX) <= TILE_SIZE * ZOOM && Math.abs(diffY) <= TILE_SIZE * ZOOM) {
                return true;
            }
        }
        return false;
    }

    private void checkPortals(Game game) {
        if (game.getGameMap(currentMap).getPortals() != null) {
            for (MapTile tile : game.getGameMap(currentMap).getPortals()) {
                if (animalRectangle.intersects(tile)) {
                    game.moveAnimalToAnotherMap(this, tile);
                }
            }
        }
    }

    public boolean isAnimalStuck(GameMap gameMap) {
        return unwalkableInThisDirection(gameMap, LEFT)
                && unwalkableInThisDirection(gameMap, RIGHT)
                && unwalkableInThisDirection(gameMap, UP)
                && unwalkableInThisDirection(gameMap, DOWN);
    }

    public void tryToMove(GameMap gameMap) {
        logger.info(String.format("Animal %s is stuck, will try to move to nearest directions", this));
        route = new Route();
        for (Direction potentialDirection : Direction.values()) {
            if (!unwalkableInThisDirection(gameMap, potentialDirection)) {
                moveAnimalTo(potentialDirection);
            }
        }
        if (isAnimalStuck(gameMap)) {
            logger.info("Animal still stuck, will try to move to center");
            moveAnimalToCenter(gameMap);
        }
        if (isAnimalStuck(gameMap)) {
            logger.error("Animal is stuck completely");
        }
    }

    public void moveAnimalTo(Direction direction) {
        switch (direction) {
            case LEFT:
                animalRectangle.setX(animalRectangle.getX() - (TILE_SIZE * ZOOM));
                break;
            case RIGHT:
                animalRectangle.setX(animalRectangle.getX() + (TILE_SIZE * ZOOM));
                break;
            case UP:
                animalRectangle.setY(animalRectangle.getY() - (TILE_SIZE * ZOOM));
                break;
            case DOWN:
                animalRectangle.setY(animalRectangle.getY() + (TILE_SIZE * ZOOM));
                break;
        }
    }

    @Override
    public int getLayer() {
        return 2;
    }

    @Override
    public boolean handleMouseClick(Rectangle mouseRectangle, Rectangle camera, int xZoom, int yZoom, Game game) {
        if (mouseRectangle.intersects(animalRectangle)) {
            logger.info("Click on Animal: ");
            moveAnimalToCenter(game.getGameMap());
            return true;
        }
        return false;
    }

    private boolean isOutSideOfMap(GameMap gameMap) {
        return animalRectangle.getX() < 0
                || animalRectangle.getY() < 0
                || animalRectangle.getX() > gameMap.getMapWidth() * (TILE_SIZE * ZOOM)
                || animalRectangle.getY() > gameMap.getMapHeight() * (TILE_SIZE * ZOOM);
    }

    private void moveAnimalToCenter(GameMap gameMap) {
        animalRectangle.setX(gameMap.getMapWidth() * TILE_SIZE * ZOOM / 2);
        animalRectangle.setY(gameMap.getMapHeight() * TILE_SIZE * ZOOM / 2);

        if (isAnimalStuck(gameMap)) {
            tryToMove(gameMap);
        }
    }

    public String getCurrentMap() {
        return currentMap;
    }

    public void setCurrentMap(String currentMap) {
        this.currentMap = currentMap;
    }

    public Sprite getPreviewSprite() {
        if (previewSprite != null) {
            return previewSprite;
        }
        return animatedSprite.getStartSprite();
    }

    public void teleportAnimalTo(int x, int y) {
        animalRectangle.setX(x);
        animalRectangle.setY(y);
    }

    public int getSpeed() {
        return speed;
    }

    public int getCurrentX() {
        return animalRectangle.getX();
    }

    public int getCurrentY() {
        return animalRectangle.getY();
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getCurrentHunger() {
        return currentHunger;
    }

    public int getCurrentThirst() {
        return currentThirst;
    }

    public int getCurrentEnergy() {
        return currentEnergy;
    }

    public int getCurrentHungerInPercent() {
        return currentHunger / (MAX_HUNGER / 100);
    }

    public int getCurrentThirstInPercent() {
        return currentThirst / (MAX_THIRST / 100);
    }

    public int getCurrentEnergyInPercent() {
        return currentEnergy / (MAX_ENERGY / 100);
    }

    public String getAnimalName() {
        return animalName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getHomeMap() {
        return homeMap;
    }

    public void setHomeMap(String homeMap) {
        this.homeMap = homeMap;
    }

    public AgeStage getAge() {
        return age;
    }

    public void setAge(AgeStage age) {
        this.age = age;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setAnimalName(String animalName) {
        this.animalName = animalName;
    }

    public int getCurrentAge() {
        return currentAge;
    }

    public void setCurrentAge(int currentAge) {
        this.currentAge = currentAge;
    }
}
