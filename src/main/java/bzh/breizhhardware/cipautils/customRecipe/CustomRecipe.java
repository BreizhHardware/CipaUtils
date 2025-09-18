package bzh.breizhhardware.cipautils.customRecipe;

import bzh.breizhhardware.cipautils.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

public class CustomRecipe {
    private final Main plugin;

    private static final int LIGHT_BLOCK_QUANTITY = 1;
    private static final String LIGHT_BLOCK_RECIPE_KEY = "light_block";

    public CustomRecipe(Main plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        // Add the shapeless recipe for the light block
        ItemStack lightBlock = new ItemStack(Material.LIGHT, LIGHT_BLOCK_QUANTITY);

        NamespacedKey key = new NamespacedKey(plugin, LIGHT_BLOCK_RECIPE_KEY);
        ShapelessRecipe recipe = new ShapelessRecipe(key, lightBlock);
        recipe.addIngredient(Material.TORCH);
        plugin.getServer().addRecipe(recipe);
    }

}
