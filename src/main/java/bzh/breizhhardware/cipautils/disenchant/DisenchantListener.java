package bzh.breizhhardware.cipautils.disenchant;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class DisenchantListener implements Listener {

    private final JavaPlugin plugin;

    public DisenchantListener(JavaPlugin plugin){
        this.plugin = plugin;
        plugin.getLogger().info("Disenchant initialized");
    }

    @EventHandler
    public void onEnchant(PrepareAnvilEvent anvil){
        AnvilInventory anvilInv = anvil.getInventory();
        ItemStack firstItem = anvilInv.getFirstItem();
        ItemStack secondItem = anvilInv.getSecondItem();
        if(firstItem != null && secondItem != null){
            if(secondItem.getType().equals(Material.BOOK)){
                if(anvil.getResult() == null){
                    Map<Enchantment, Integer> enchantments =  firstItem.getEnchantments();
                    if(!enchantments.isEmpty()){
                        ItemStack newBook = new ItemStack(Material.ENCHANTED_BOOK);
                        newBook.addUnsafeEnchantments(enchantments);
                        anvil.setResult(newBook);
                        Repairable repairable = (Repairable) firstItem.getItemMeta();
                        anvilInv.setRepairCost(repairable.getRepairCost());
                    }
                }
            }

        }
    }

    @EventHandler
    public void onClickAnvil(InventoryClickEvent event){
        if(event.getInventory().getType() != InventoryType.ANVIL) return;
        if(event.getSlot() != 2)  return;

        if(event.getResult() == null)  return;

        Player p = (Player) event.getWhoClicked();

        AnvilInventory anvilInv = (AnvilInventory) event.getInventory();

        ItemStack result = anvilInv.getItem(2);

        ItemStack firstItem = anvilInv.getItem(0);
        ItemStack secondItem = anvilInv.getItem(1);

        if(firstItem == null) return;
        if(secondItem == null) return;
        if(secondItem.getType() != Material.BOOK) return;

        Map<Enchantment, Integer> enchantments = firstItem.getEnchantments();

        if(enchantments.isEmpty()) return;

        Repairable repairable = (Repairable) firstItem.getItemMeta();
        //int expCost = calculateDisenchantmentCost(repairable.getRepairCost(), enchantments);
        //this.plugin.getLogger().info("Repair: " + repairable.getRepairCost() + ", expCost: " + expCost);

        if(repairable.getRepairCost() > p.getLevel() && p.getGameMode() != GameMode.CREATIVE){
            event.setCancelled(true);
            return;
        }

        int exp = p.getLevel() - repairable.getRepairCost();

        firstItem.removeEnchantments();

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if(secondItem.getAmount() > 1){
                secondItem.setAmount(secondItem.getAmount()-1);
                anvilInv.setItem(1, secondItem);
            }else{
                anvilInv.setItem(1, null);
            }
        });

        p.setItemOnCursor(result);

        if(p.getGameMode() != GameMode.CREATIVE) p.setLevel(exp);

        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, (float) 1.0, (float)1.0);
    }
    /*
    public int calculateDisenchantmentCost(int repairCost, Map<Enchantment, Integer> enchantments){
        int baseCost = Math.max(1, repairCost);

        int enchantmentMultiplier = 0;

        for(Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()){
            this.plugin.getLogger().info(entry.getValue().toString());
            enchantmentMultiplier += entry.getValue();
        }
        int result = baseCost + enchantmentMultiplier;
        this.plugin.getLogger().info("Result: " + result + " , baseCost: " + baseCost + ", enchantmentMultiplier: " + enchantmentMultiplier);
        return baseCost + enchantmentMultiplier;
    }*/
}

