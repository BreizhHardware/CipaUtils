package bzh.breizhhardware.cipautils.waystone;

import bzh.breizhhardware.cipautils.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.util.Arrays;

public class WaystoneRecipe {
    private final Main plugin;

    public WaystoneRecipe(Main plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        // Craft for a waystone
        ItemStack waystoneItem = createWaystoneItem();
        NamespacedKey key = new NamespacedKey(plugin, "waystone");

        ShapedRecipe recipe = new ShapedRecipe(key, waystoneItem);
        recipe.shape(
            "EDE",
            "DLD",
            "OOO"
        );

        recipe.setIngredient('E', Material.ENDER_PEARL);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('L', Material.LODESTONE);
        recipe.setIngredient('O', Material.OBSIDIAN);

        plugin.getServer().addRecipe(recipe);
        plugin.getLogger().info("Recette de waystone enregistrée !");
    }

    private ItemStack createWaystoneItem() {
        ItemStack item = new ItemStack(Material.LODESTONE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "Waystone Vierge");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Placez ce bloc pour créer",
            ChatColor.GRAY + "un point de téléportation",
            ChatColor.GOLD + "Craft: Ender Pearl + Diamants + Lodestone + Obsidienne",
            ChatColor.BLUE + "Clic droit après placement pour nommer"
        ));

        item.setItemMeta(meta);
        return item;
    }
}
