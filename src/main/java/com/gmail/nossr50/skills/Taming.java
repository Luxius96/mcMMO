package com.gmail.nossr50.skills;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.gmail.nossr50.Users;
import com.gmail.nossr50.m;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcPermissions;
import com.gmail.nossr50.config.LoadProperties;
import com.gmail.nossr50.datatypes.PlayerProfile;
import com.gmail.nossr50.datatypes.SkillType;
import com.gmail.nossr50.locale.mcLocale;
import com.gmail.nossr50.runnables.mcBleedTimer;

public class Taming {

    /**
     * Apply the Fast Food Service ability.
     *
     * @param PPo The PlayerProfile of the wolf's owner
     * @param theWolf The wolf using the ability
     * @param event The event to modify
     */
    public static void fastFoodService (PlayerProfile PPo, Wolf theWolf, EntityDamageEvent event) {
        final int SKILL_ACTIVATION_LEVEL = 50;
        final int ACTIVATION_CHANCE = 50;

        int health = theWolf.getHealth();
        int maxHealth = theWolf.getMaxHealth();
        int damage = event.getDamage();

        if (PPo.getSkillLevel(SkillType.TAMING) >= SKILL_ACTIVATION_LEVEL) {
            if (health < maxHealth) {
                if (Math.random() * 100 < ACTIVATION_CHANCE) {
                    if (health + damage <= maxHealth) {
                        theWolf.setHealth(health + damage);
                    }
                    else {
                        theWolf.setHealth(maxHealth);
                    }
                }
            }
        }
    }

    /**
     * Apply the Sharpened Claws ability.
     *
     * @param PPo The PlayerProfile of the wolf's owner
     * @param event The event to modify
     */
    public static void sharpenedClaws(PlayerProfile PPo, EntityDamageEvent event) {
        final int SKILL_ACTIVATION_LEVEL = 750;
        final int SHARPENED_CLAWS_BONUS = 2;

        if (PPo.getSkillLevel(SkillType.TAMING) >= SKILL_ACTIVATION_LEVEL) {
            event.setDamage(event.getDamage() + SHARPENED_CLAWS_BONUS);
        }
    }

    /**
     * Apply the Gore ability.
     *
     * @param PPo The PlayerProfile of the wolf's owner
     * @param event The event to modify
     * @param master The wolf's master
     * @param plugin mcMMO plugin instance
     */
    public static void gore(PlayerProfile PPo, EntityDamageEvent event, Player master, mcMMO plugin) {
        final int GORE_MULTIPLIER = 2;

        if (Math.random() * 1000 <= PPo.getSkillLevel(SkillType.TAMING)) {
            Entity entity = event.getEntity();
            event.setDamage(event.getDamage() * GORE_MULTIPLIER);

            if (entity instanceof Player) {
                Player target = (Player) entity;

                target.sendMessage(mcLocale.getString("Combat.StruckByGore"));
                Users.getProfile(target).setBleedTicks(2);
            }
            else {
                mcBleedTimer.add((LivingEntity) entity);
            }

            master.sendMessage(mcLocale.getString("Combat.Gore"));
        }
    }

    /**
     * Get the name of a wolf's owner.
     *
     * @param theWolf The wolf whose owner's name to get
     * @return the name of the wolf's owner, or "Offline Master" if the owner is offline
     */
    private static String getOwnerName(Wolf theWolf) {
        AnimalTamer tamer = theWolf.getOwner();

        if (tamer instanceof Player) {
            Player owner = (Player) tamer;
            return owner.getName();
        }
        else {
            return "Offline Master";
        }
    }

    /**
     * Prevent damage to wolves based on various skills.
     *
     * @param event The event to modify
     */
    public static void preventDamage(EntityDamageEvent event) {
        final int ENVIRONMENTALLY_AWARE_LEVEL = 100;
        final int THICK_FUR_LEVEL = 250;
        final int SHOCK_PROOF_LEVEL = 500;

        final int THICK_FUR_MODIFIER = 2;
        final int SHOCK_PROOF_MODIFIER = 6;

        DamageCause cause = event.getCause();
        Wolf wolf = (Wolf) event.getEntity();
        Player master = (Player) wolf.getOwner();
        int skillLevel = Users.getProfile(master).getSkillLevel(SkillType.TAMING);

        switch (cause) {

        /* Environmentally Aware */
        case CONTACT:
        case LAVA:
        case FIRE:
            if (mcPermissions.getInstance().environmentallyAware(master)) {
                if (skillLevel >= ENVIRONMENTALLY_AWARE_LEVEL) {
                    if (event.getDamage() >= wolf.getHealth()) {
                        return;
                    }

                    wolf.teleport(master.getLocation());
                    master.sendMessage(mcLocale.getString("mcEntityListener.WolfComesBack"));
                }
            }
            break;

        case FALL:
            if (mcPermissions.getInstance().environmentallyAware(master)) {
                if (skillLevel >= ENVIRONMENTALLY_AWARE_LEVEL) {
                    event.setCancelled(true);
                }
            }
            break;

        /* Thick Fur */
        case FIRE_TICK:
            if (mcPermissions.getInstance().thickFur(master)) {
                if(skillLevel >= THICK_FUR_LEVEL) {
                    wolf.setFireTicks(0);
                }
            }
            break;

        case ENTITY_ATTACK:
        case PROJECTILE:
            if (mcPermissions.getInstance().thickFur(master)) {
                if (skillLevel >= THICK_FUR_LEVEL) {
                    event.setDamage(event.getDamage() / THICK_FUR_MODIFIER);
                }
            }
            break;

        /* Shock Proof */
        case ENTITY_EXPLOSION:
        case BLOCK_EXPLOSION:
            if (mcPermissions.getInstance().shockProof(master)) {
                if (skillLevel >= SHOCK_PROOF_LEVEL) {
                    event.setDamage(event.getDamage() / SHOCK_PROOF_MODIFIER);
                }
            }
            break;

        default:
            break;
        }
    }

    /**
     * Summon an animal.
     *
     * @param type Type of animal to summon
     * @param player Player summoning the animal
     */
    public static void animalSummon(EntityType type, Player player, mcMMO plugin) {
        ItemStack item = player.getItemInHand();
        Material summonItem = null;
        int summonAmount = 0;

        switch (type) {
        case WOLF:
            summonItem = Material.BONE;
            summonAmount = LoadProperties.bonesConsumedByCOTW;
            break;

        case OCELOT:
            summonItem = Material.RAW_FISH;
            summonAmount = LoadProperties.fishConsumedByCOTW;
            break;

        default:
            break;
        }

        if (item.getType().equals(summonItem)) {
            if (item.getAmount() >= summonAmount) {
                for (Entity x : player.getNearbyEntities(40, 40, 40)) {
                    if (x.getType().equals(type)) {
                        player.sendMessage(mcLocale.getString("m.TamingSummonFailed"));
                        return;
                    }
                }
                LivingEntity entity = player.getWorld().spawnCreature(player.getLocation(), type);
                entity.setMetadata("mcmmoSummoned", new FixedMetadataValue(plugin, true));
                ((Tameable) entity).setOwner(player);

                player.setItemInHand(new ItemStack(summonItem, item.getAmount() - summonAmount));
                player.sendMessage(mcLocale.getString("m.TamingSummon"));
            }
            else {
                player.sendMessage(mcLocale.getString("Skills.NeedMore")+ " " + ChatColor.GRAY + m.prettyItemString(summonItem.getId()));
            }
        }
    }

    /**
     * Inspect a tamed animal for details.
     *
     * @param event
     * @param target
     */
    public static void beastLore(EntityDamageByEntityEvent event, LivingEntity target, Player attacker) {

        //TODO: Make this work for Ocelots
        if (target.getType().equals(EntityType.WOLF)) {
            Wolf wolf = (Wolf) target;
            String message = mcLocale.getString("Combat.BeastLore") + " ";
            int health = wolf.getHealth();
            event.setCancelled(true);

            if (wolf.isTamed()) {
                message = message.concat(mcLocale.getString("Combat.BeastLoreOwner", new Object[] {getOwnerName(wolf)}) + " ");
                message = message.concat(mcLocale.getString("Combat.BeastLoreHealthWolfTamed", new Object[] {health}));
            }
            else {
                message = message.concat(mcLocale.getString("Combat.BeastLoreHealthWolf", new Object[] {health}));
            }

            attacker.sendMessage(message);
        }
    }
}
