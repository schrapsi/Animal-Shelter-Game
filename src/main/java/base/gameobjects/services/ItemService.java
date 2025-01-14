package base.gameobjects.services;

import base.gameobjects.*;
import base.gameobjects.plants.*;
import base.graphicsservice.SpriteService;

import java.util.Arrays;
import java.util.List;

public class ItemService {

    public static final List<String> STACKABLE_ITEMS = Arrays.asList(Carrot.NAME, Beet.NAME, Tomato.NAME, Strawberry.NAME, Bellpepper.NAME, Corn.NAME, Potato.NAME,
            "seed" + Carrot.NAME, "seed" + Beet.NAME, "seed" + Tomato.NAME, "seed" + Strawberry.NAME, "seed" + Bellpepper.NAME, "seed" + Corn.NAME, "seed" + Potato.NAME,
            Wood.ITEM_NAME, Feather.ITEM_NAME, Mushroom.ITEM_NAME, PetFood.SIMPLE_MEAL, PetFood.TASTY_MEAL, PetFood.PERFECT_MEAL);

    public Item createNewItem(SpriteService spriteService, String itemType, int x, int y) {
        Item item = createNewItem(itemType, x, y);
        if (item instanceof PetFood) {
            item.setSprite(spriteService.getMealSprite(itemType));
        } else {
            item.setSprite(spriteService.getPlantPreviewSprite(itemType));
        }
        return item;
    }

    public Item createNewItem(String itemType, int x, int y) {
        if (itemType.startsWith("seed")) {
            itemType = itemType.substring(4);
            return new Seed(itemType, x, y);
        } else if (PlantService.plantTypes.contains(itemType)) {
            return new Item(x, y, itemType);
        } else if (itemType.equalsIgnoreCase(PetFood.SIMPLE_MEAL) || itemType.equalsIgnoreCase(PetFood.TASTY_MEAL) || itemType.equalsIgnoreCase(PetFood.PERFECT_MEAL)) {
            return new PetFood(x, y, itemType);
        }
        return null;
    }
}
