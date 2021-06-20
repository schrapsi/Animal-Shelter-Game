package base.map;

import base.Game;
import base.gameObjects.GameObject;
import base.graphicsService.Rectangle;
import base.graphicsService.RenderHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class GameMap {

    TileService tileService;

    private File mapFile;
    private Map<Integer, List<MapTile>> layeredTiles = new HashMap<>();
    private List<MapTile> portals = new ArrayList<>();

    int backGroundTileId = -1;      //background of walkable part of the map
    int alphaBackground = -1;       //outside of walkable part
    public int mapWidth = -1;
    public int mapHeight = -1;
    int maxLayer = -1;

    public GameMap(File mapFile, TileService tileService) {
        this.tileService = tileService;
        this.mapFile = mapFile;
        loadMapFromFile();
    }

    private void loadMapFromFile() {
        try {
            Scanner scanner = new Scanner(mapFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("Fill:")) {
                    String[] splitLine = line.split(":");
                    backGroundTileId = Integer.parseInt(splitLine[1]);
                    continue;
                }
                if (line.startsWith("AlphaFill:")) {
                    String[] splitLine = line.split(":");
                    alphaBackground = Integer.parseInt(splitLine[1]);
                    continue;
                }
                if (line.startsWith("Size:")) {
                    String[] splitLine = line.split(":");
                    mapWidth = Integer.parseInt(splitLine[1]);
                    if (mapWidth < 20) {
                        mapWidth = 20;
                    }
                    mapHeight = Integer.parseInt(splitLine[2]);
                    if (mapHeight < 20) {
                        mapHeight = 20;
                    }
                    System.out.println("Size of the map is " + mapWidth + " by " + mapHeight + " tiles");
                    continue;
                }
                if (line.startsWith("//")) {        //just a comment
                    continue;
                }
                String[] splitLine = line.split(",");
                if (splitLine.length >= 4) {
                    int layer = Integer.parseInt(splitLine[0]);
                    List<MapTile> tiles;
                    if (layeredTiles.containsKey(layer)) {
                        tiles = layeredTiles.get(layer);
                    } else {
                        tiles = new ArrayList<>();
                        layeredTiles.put(layer, tiles);
                    }
                    if (maxLayer < layer) {
                        maxLayer = layer;
                        System.out.println("max layer: " + maxLayer);
                    }
                    int tileId = Integer.parseInt(splitLine[1]);
                    int xPosition = Integer.parseInt(splitLine[2]);
                    int yPosition = Integer.parseInt(splitLine[3]);
                    MapTile tile = new MapTile(layer, tileId, xPosition, yPosition);
                    if (splitLine.length >= 5) {
                        System.out.println("Found portal");
                        tile.setPortal(true);
                        tile.setPortalDirection(splitLine[4]);
                        portals.add(tile);
                    }
                    tiles.add(tile);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void renderMap(RenderHandler renderer, List<GameObject> gameObjects, int xZoom, int yZoom) {
        int tileWidth = Game.TILE_SIZE * xZoom;
        int tileHeight = Game.TILE_SIZE * yZoom;
        renderFixedSizeMap(renderer, gameObjects, xZoom, yZoom, tileWidth, tileHeight);
    }

    private void renderInSightOfCamera(RenderHandler renderer, int xZoom, int yZoom, int tileWidth, int tileHeight, int tileId) {
        Rectangle camera = renderer.getCamera();

        for (int i = camera.getY() - tileHeight - (camera.getY() % tileHeight); i < camera.getY() + camera.getHeight(); i += tileHeight) {
            for (int j = camera.getX() - tileWidth - (camera.getX() % tileWidth); j < camera.getX() + camera.getWidth(); j += tileWidth) {
                tileService.renderTile(tileId, renderer, j, i, xZoom, yZoom);
            }
        }
    }

    private void renderFixedSizeMap(RenderHandler renderer, List<GameObject> gameObjects, int xZoom, int yZoom, int tileWidth, int tileHeight) {
        if (alphaBackground >= 0) {
            renderInSightOfCamera(renderer, xZoom, yZoom, tileWidth, tileHeight, alphaBackground);
        }
        if (backGroundTileId >= 0) {
            for (int i = 0; i < mapHeight * tileHeight; i += tileHeight) {
                for (int j = 0; j < mapWidth * tileWidth; j += tileWidth) {
                    tileService.renderTile(backGroundTileId, renderer, j, i, xZoom, yZoom);
                }
            }
        }
        for (GameObject gameObject : gameObjects) {
            if (maxLayer < gameObject.getLayer()) {
                maxLayer = gameObject.getLayer();
                System.out.println("max layer: " + maxLayer);
            }
        }
        for (int i = 0; i <= maxLayer; i++) {
            List<MapTile> tiles = layeredTiles.get(i);
            if (tiles != null) {
                // with for-each loop there is ConcurrentModificationException often, but with this loop everything works fine
                for (int j = 0; j < tiles.size(); j++) {
                    MapTile mappedTile = tiles.get(j);
                    if (mappedTile.getLayer() == i) {
                        int xPosition = mappedTile.getX() * tileWidth;
                        int yPosition = mappedTile.getY() * tileHeight;
                        if (xPosition <= mapWidth * tileWidth && yPosition <= mapHeight * tileHeight) {
                            tileService.renderTile(mappedTile.getId(), renderer, xPosition, yPosition, xZoom, yZoom);
                        }
                    }
                }
            }
            for (GameObject gameObject : gameObjects) {
                if (gameObject.getLayer() == i) {
                    gameObject.render(renderer, xZoom, yZoom);
                }
            }
        }
    }

    public List<MapTile> getPortals() {
        return portals;
    }


    public List<MapTile> getTilesOnLayer(int layer) {
        return layeredTiles.get(layer);
    }

    public void setTile(int tileX, int tileY, int tileId) {
//        boolean foundTile = false;
//        for (MappedTile tile : tileList) {
//            if (tile.getX() == tileX && tile.getY() == tileY) {
//                tile.setId(tileId);
//                foundTile = true;
//                break;
//            }
//        }
//        if (!foundTile) {
//            tileList.add(new MappedTile(tileId, tileX, tileY));
//        }
    }

    public void removeTile(int tileX, int tileY) {
//        tileList.removeIf(tile -> tile.getX() == tileX && tile.getY() == tileY);
    }

    public void saveMap() {
//        try {
//            int currentLine = 0;
//
//            if (mapFile.exists()) {
//                mapFile.delete();
//            }
//
//            mapFile.createNewFile();
//            PrintWriter printWriter = new PrintWriter(mapFile);
//
//            if (fillTileId >= 0) {
//                if (comments.containsKey(currentLine)) {
//                    printWriter.println(comments.get(currentLine));
//                    currentLine++;
//                }
//                printWriter.println("Fill:" + fillTileId);
//            }
//
//            for (MappedTile tile : tileList) {
//                if (comments.containsKey(currentLine)) {
//                    printWriter.println(comments.get(currentLine));
//                }
//                printWriter.println(tile.getId() + "," + tile.getX() + "," + tile.getY());
//                currentLine++;
//            }
//
//            printWriter.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
