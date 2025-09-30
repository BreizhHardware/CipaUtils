package bzh.breizhhardware.cipautils.customRecipe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.Arrays;
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

    public void registerCustomRecipes() {
        registerLightBlockRecipe();
        registerWaystoneRecipe();
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

    private void registerWaystoneRecipe() {
        // Craft for a waystone
        ItemStack waystoneItem = createWaystoneItem();
        NamespacedKey key = new NamespacedKey(plugin, "waystone");

        ShapedRecipe recipe = new ShapedRecipe(key, waystoneItem);
        recipe.shape(
            "EDE",
            "DLD",
            "OOO"
        );

        recipe.setIngredient('E', org.bukkit.Material.ENDER_PEARL);
        recipe.setIngredient('D', org.bukkit.Material.DIAMOND);
        recipe.setIngredient('L', org.bukkit.Material.LODESTONE);
        recipe.setIngredient('O', org.bukkit.Material.OBSIDIAN);

        registerRecipe(recipe);
        plugin.getLogger().info("Waystone recipe registered!");
    }

    private ItemStack createWaystoneItem() {
        ItemStack item = new ItemStack(org.bukkit.Material.LODESTONE);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "Empty Waystone");
        java.util.List<String> lore = java.util.Arrays.asList(
                ChatColor.GRAY + "Place this bloc to create",
                ChatColor.GRAY + "a point of teleportation.",
                ChatColor.GOLD + "Craft: Ender Pearl + Diamants + Lodestone + Obsidian",
                ChatColor.BLUE + "Right-click a placed waystone to use it"
        );
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
