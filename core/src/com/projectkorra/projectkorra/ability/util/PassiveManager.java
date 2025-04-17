package com.projectkorra.projectkorra.ability.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.projectkorra.projectkorra.configuration.ConfigManager;
import org.bukkit.entity.Player;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.PassiveAbility;

public class PassiveManager {

	private static final Map<String, CoreAbility> PASSIVES = new HashMap<>();
	private static final Map<PassiveAbility, Class<? extends CoreAbility>> PASSIVE_CLASSES = new HashMap<>();

	public static void registerPassives(final Player player) {
		final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null) {
			return;
		}

		for (final CoreAbility ability : PASSIVES.values()) {
            if (!(ability instanceof PassiveAbility passive)) {
            	continue;
			} else if (!hasPassive(player, ability) || CoreAbility.hasAbility(player, ability.getClass())) {
				continue;
			} else if (!passive.isInstantiable()) {
				/*
				 * Passive's such as not taking fall damage are managed in
				 * PKListener, so we do not want to create instances of them
				 * here. This just enables the passive to be displayed in /b
				 * d [element]passive
				 */
				continue;
			}

			try {
				final Class<? extends CoreAbility> clazz = PASSIVE_CLASSES.get(ability);
				final Constructor<?> constructor = clazz.getConstructor(Player.class);
				final Object object = constructor.newInstance(player);
				((CoreAbility) object).start();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
        }
	}

	public static boolean hasPassive(final Player player, final CoreAbility passive) {
		if (player == null || passive == null) {
			return false;
		}

		final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		if (bPlayer == null || (!bPlayer.isToggled() && ConfigManager.defaultConfig.get().getBoolean("Properties.TogglePassivesWithAllBending"))) {
			return false;
		}

		Element element = passive.getElement();
		if (passive.getElement() instanceof SubElement subElement) {
			element = subElement.getParentElement();
		}
		return passive instanceof PassiveAbility && passive.isEnabled() && bPlayer.canBendPassive(passive)
				&& bPlayer.isElementToggled(element) && bPlayer.isPassiveToggled(element) && bPlayer.isToggledPassives();
	}

	public static Set<String> getPassivesForElement(final Element element) {
		final Set<String> passives = new HashSet<>();
		for (final CoreAbility passive : PASSIVES.values()) {
			if (passive.getElement() == element) {
				passives.add(passive.getName());
			} else if (passive.getElement() instanceof SubElement subElement) {
				final Element check = subElement.getParentElement();
				if (check == element) {
					passives.add(passive.getName());
				}
			}
		}
		return passives;
	}

	public static Map<String, CoreAbility> getPassives() {
		return PASSIVES;
	}

	public static Map<PassiveAbility, Class<? extends CoreAbility>> getPassiveClasses() {
		return PASSIVE_CLASSES;
	}

}
