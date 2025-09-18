package bzh.breizhhardware.cipautils.customRecipe;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class RecipeManager {
    private final JavaPlugin plugin;
    private final List<Recipe> customRecipes = new ArrayList<>();

    private static final int LIGHT_BLOCK_QUANTITY = 1;
    private static final String LIGHT_BLOCK_RECIPE_KEY = "light_block";


    public RecipeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRecipe(Recipe recipe) {
        Bukkit.addRecipe(recipe);
        customRecipes.add(recipe);
    }

    public List<Recipe> getCustomRecipes() {
        return new ArrayList<>(customRecipes);
    }

    public void registerLightBlockRecipe() {
        // Add the shapeless recipe for the light block
        ItemStack lightBlock = new ItemStack(org.bukkit.Material.LIGHT, LIGHT_BLOCK_QUANTITY);
        NamespacedKey key = new NamespacedKey(plugin, LIGHT_BLOCK_RECIPE_KEY);
        ShapelessRecipe recipe = new ShapelessRecipe(key, lightBlock);
        recipe.addIngredient(org.bukkit.Material.TORCH);
        registerRecipe(recipe);
    }
}
