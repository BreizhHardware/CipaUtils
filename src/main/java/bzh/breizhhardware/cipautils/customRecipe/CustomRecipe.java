package bzh.breizhhardware.cipautils.customRecipe;

import bzh.breizhhardware.cipautils.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

public class CustomRecipe {
    private final Main plugin;

    public CustomRecipe(Main plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        // Add the shapeless recipe for the light block
        ItemStack lightBlock = new ItemStack(Material.LIGHT, 1);

        NamespacedKey key = new NamespacedKey(plugin, "light_block");
        ShapelessRecipe recipe = new ShapelessRecipe(key, lightBlock);
        recipe.addIngredient(Material.TORCH);
        plugin.getServer().addRecipe(recipe);
    }

}
