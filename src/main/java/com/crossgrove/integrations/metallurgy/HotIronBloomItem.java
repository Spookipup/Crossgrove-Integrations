package com.crossgrove.integrations.metallurgy;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.common.data.GTDamageTypes;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.crossgrove.integrations.overgeared.OvergearedTongsProtection;

public final class HotIronBloomItem extends Item {
	public HotIronBloomItem(Properties properties) {
		super(properties.fireResistant());
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		super.inventoryTick(stack, level, entity, slotId, isSelected);
		if (level.isClientSide || !(entity instanceof LivingEntity living) || living.tickCount % 20 != 0) {
			return;
		}
		if (living instanceof Player player && OvergearedTongsProtection.tryProtect(player)) {
			return;
		}

		float damage = (GTMaterials.Iron.getBlastTemperature() - 1750) / 1000.0F + 2.0F;
		ItemStack chestStack = living.getItemBySlot(EquipmentSlot.CHEST);
		if (!chestStack.isEmpty() && chestStack.getItem() instanceof ArmorComponentItem armor) {
			damage *= armor.getArmorLogic().getHeatResistance();
		}

		if (damage > 0.0F) {
			living.hurt(GTDamageTypes.HEAT.source(level), damage);
		} else if (damage < 0.0F) {
			living.hurt(living.damageSources().freeze(), -damage);
		}
	}
}
