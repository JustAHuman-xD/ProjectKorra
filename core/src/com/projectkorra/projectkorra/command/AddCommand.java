package com.projectkorra.projectkorra.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.projectkorra.projectkorra.util.ChatUtil;
import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.event.PlayerChangeElementEvent;
import com.projectkorra.projectkorra.event.PlayerChangeElementEvent.Result;
import com.projectkorra.projectkorra.event.PlayerChangeSubElementEvent;

/**
 * Executor for /bending add. Extends {@link PKCommand}.
 */
public class AddCommand extends PKCommand {

	private final String playerNotFound;
	private final String invalidElement;
	private final String addedOtherCFW;
	private final String addedOtherAE;
	private final String addedCFW;
	private final String addedAE;
	private final String alreadyHasElementOther;
	private final String alreadyHasElement;
	private final String alreadyHasSubElementOther;
	private final String alreadyHasSubElement;
	private final String addedOtherAll;
	private final String addedAll;
	private final String alreadyHasAllElementsOther;
	private final String alreadyHasAllElements;

	public AddCommand() {
		super("add", "/bending add <Element/SubElement> [Player]", ConfigManager.languageConfig.get().getString("Commands.Add.Description"), new String[] { "add", "a" });

		this.playerNotFound = ConfigManager.languageConfig.get().getString("Commands.Add.PlayerNotFound");
		this.invalidElement = ConfigManager.languageConfig.get().getString("Commands.Add.InvalidElement");
		this.addedOtherCFW = ConfigManager.languageConfig.get().getString("Commands.Add.Other.SuccessfullyAddedCFW");
		this.addedOtherAE = ConfigManager.languageConfig.get().getString("Commands.Add.Other.SuccessfullyAddedAE");
		this.addedCFW = ConfigManager.languageConfig.get().getString("Commands.Add.SuccessfullyAddedCFW");
		this.addedAE = ConfigManager.languageConfig.get().getString("Commands.Add.SuccessfullyAddedAE");
		this.addedOtherAll = ConfigManager.languageConfig.get().getString("Commands.Add.Other.SuccessfullyAddedAll");
		this.addedAll = ConfigManager.languageConfig.get().getString("Commands.Add.SuccessfullyAddedAll");
		this.alreadyHasElementOther = ConfigManager.languageConfig.get().getString("Commands.Add.Other.AlreadyHasElement");
		this.alreadyHasElement = ConfigManager.languageConfig.get().getString("Commands.Add.AlreadyHasElement");
		this.alreadyHasSubElementOther = ConfigManager.languageConfig.get().getString("Commands.Add.Other.AlreadyHasSubElement");
		this.alreadyHasSubElement = ConfigManager.languageConfig.get().getString("Commands.Add.AlreadyHasSubElement");
		this.alreadyHasAllElementsOther = ConfigManager.languageConfig.get().getString("Commands.Add.Other.AlreadyHasAllElements");
		this.alreadyHasAllElements = ConfigManager.languageConfig.get().getString("Commands.Add.AlreadyHasAllElements");
	}

	@Override
	public void execute(final CommandSender sender, final List<String> args) {
		if (!this.correctLength(sender, args.size(), 1, 2)) {
			return;
		} else if (args.size() == 1) { // bending add element.
			if (!this.hasPermission(sender) || !this.isPlayer(sender)) {
				return;
			}
			this.add(sender, (Player) sender, args.get(0).toLowerCase());
		} else if (args.size() == 2) { // bending add element combo.
			if (!this.hasPermission(sender, "others")) {
				return;
			}

			this.getPlayer(args.get(1)).thenAccept(player -> {
				if (player == null || (!player.isOnline() && !player.hasPlayedBefore())) {
					ChatUtil.sendBrandingMessage(sender, ChatColor.RED + this.playerNotFound);
					return;
				}
				this.add(sender, player, args.get(0).toLowerCase());
			}).exceptionally(e -> {
				e.printStackTrace();
				return null;
			});
		}
	}

	/**
	 * Adds the ability to bend an element to a player.
	 *
	 * @param sender The CommandSender who issued the add command
	 * @param target The player to add the element to
	 * @param element The element to add
	 */
	private void add(final CommandSender sender, final OfflinePlayer target, final String element) {

		// if they aren't a BendingPlayer, create them.
		BendingPlayer.getOrLoadOfflineAsync(target).thenAccept(bPlayer -> {
			boolean online = bPlayer instanceof BendingPlayer;

			if (bPlayer.isPermaRemoved()) { // ignore permabanned users.
				ChatUtil.sendBrandingMessage(sender, ChatColor.RED + ConfigManager.languageConfig.get().getString("Commands.Preset.Other.BendingPermanentlyRemoved"));
				return;
			}

			if (element.equalsIgnoreCase("all")) {
				final StringBuilder elements = new StringBuilder("");
				boolean elementFound = false;
				for (final Element e : Element.getAllElements()) {
					if (!bPlayer.hasElement(e) && e != Element.AVATAR) {
						if (!this.hasPermission(sender, e.getName().toLowerCase())) {
							continue;
						}

						PlayerChangeElementEvent event = new PlayerChangeElementEvent(sender, target, e, Result.ADD);
						Bukkit.getServer().getPluginManager().callEvent(event);
						if (event.isCancelled()) continue; // if the event is cancelled, don't add the element.

						elementFound = true;
						bPlayer.addElement(e);

						if (elements.length() > 1) {
							elements.append(ChatColor.YELLOW + ", ");
						}
						elements.append(e.toString());

						bPlayer.getSubElements().clear();
						if (online) {
							for (final SubElement sub : Element.getAllSubElements()) {
								if (bPlayer.hasElement(sub.getParentElement()) && ((BendingPlayer)bPlayer).hasSubElementPermission(sub)) {
									PlayerChangeSubElementEvent subEvent = new PlayerChangeSubElementEvent(sender, target, sub, com.projectkorra.projectkorra.event.PlayerChangeSubElementEvent.Result.ADD);
									Bukkit.getServer().getPluginManager().callEvent(subEvent);
									if (subEvent.isCancelled()) continue; // if the event is cancelled, don't add the subelement.

									bPlayer.addSubElement(sub);
								}
							}
							bPlayer.saveSubElements();
						}

						bPlayer.saveElements();
					}
				}
				if (elementFound) {
					if (!(sender instanceof Player) || !((Player) sender).equals(target)) {
						ChatUtil.sendBrandingMessage(sender, ChatColor.YELLOW + this.addedOtherAll.replace("{target}", ChatColor.DARK_AQUA + target.getName() + ChatColor.YELLOW) + elements);
						if (online) ChatUtil.sendBrandingMessage((Player)target, ChatColor.YELLOW + this.addedAll + elements);
					} else {
						if (online) ChatUtil.sendBrandingMessage((Player)target, ChatColor.YELLOW + this.addedAll + elements);
					}
				} else {
					if (!(sender instanceof Player) || !((Player) sender).equals(target)) {
						ChatUtil.sendBrandingMessage(sender, ChatColor.RED + this.alreadyHasAllElementsOther.replace("{target}", ChatColor.DARK_AQUA + target.getName() + ChatColor.RED));
					} else {
						ChatUtil.sendBrandingMessage(sender, ChatColor.RED + this.alreadyHasAllElements);
					}
				}
				return;
			} else {

				// get the [sub]element.
				Element e = Element.fromString(element);
				if (e == null) {
					e = Element.fromString(element);
				}

				if (e == Element.AVATAR) {
					this.add(sender, target, Element.AIR.getName());
					this.add(sender, target, Element.EARTH.getName());
					this.add(sender, target, Element.FIRE.getName());
					this.add(sender, target, Element.WATER.getName());
					return;
				}

				// if it's an element:
				if (Arrays.asList(Element.getAllElements()).contains(e)) {
					boolean hasPermission = sender.hasPermission("bending.command.add." + Element.AVATAR.getName().toLowerCase()) || this.hasPermission(sender, e.getName().toLowerCase());

					if (!hasPermission) {
						return;
					}

					if (bPlayer.hasElement(e)) { // if already had, determine who to send the error message to.
						if (!(sender instanceof Player) || !((Player) sender).equals(target)) {
							ChatUtil.sendBrandingMessage(sender, ChatColor.RED + this.alreadyHasElementOther.replace("{target}", ChatColor.DARK_AQUA + target.getName() + ChatColor.RED));
						} else {
							ChatUtil.sendBrandingMessage(sender, ChatColor.RED + this.alreadyHasElement);
						}
						return;
					}

					PlayerChangeElementEvent event = new PlayerChangeElementEvent(sender, target, e, Result.ADD);
					Bukkit.getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) return; // if the event is cancelled, don't add the element.

					bPlayer.addElement(e);
					bPlayer.getSubElements().clear();
					if (online) { //Add all subs they have permission for
						for (final SubElement sub : Element.getAllSubElements()) {
							if (bPlayer.hasElement(sub.getParentElement()) && ((BendingPlayer)bPlayer).hasSubElementPermission(sub)) {
								PlayerChangeSubElementEvent subEvent = new PlayerChangeSubElementEvent(sender, target, sub, com.projectkorra.projectkorra.event.PlayerChangeSubElementEvent.Result.ADD);
								Bukkit.getServer().getPluginManager().callEvent(subEvent);
								if (subEvent.isCancelled()) continue; // if the event is cancelled, don't add the subelement.

								bPlayer.addSubElement(sub);
							}
						}
					}

					// send the message.
					final ChatColor color = e.getColor();
					if (!(sender instanceof Player) || !((Player) sender).equals(target)) {
						if (e != Element.AIR && e != Element.EARTH && e != Element.BLUE_FIRE) {
							ChatUtil.sendBrandingMessage(sender, color + this.addedOtherCFW.replace("{target}", ChatColor.DARK_AQUA + target.getName() + color).replace("{element}", e.toString() + e.getType().getBender()));
							if (online) ChatUtil.sendBrandingMessage((Player)target, color + this.addedCFW.replace("{element}", e.toString() + e.getType().getBender()));
						} else {
							ChatUtil.sendBrandingMessage(sender, color + this.addedOtherAE.replace("{target}", ChatColor.DARK_AQUA + target.getName() + color).replace("{element}", e.toString() + e.getType().getBender()));
							if (online) ChatUtil.sendBrandingMessage((Player)target, color + this.addedAE.replace("{element}", e.toString() + e.getType().getBender()));
						}
					} else {
						if (e != Element.AIR && e != Element.EARTH) {
							if (online) ChatUtil.sendBrandingMessage((Player)target, color + this.addedCFW.replace("{element}", e.toString() + e.getType().getBender()));
						} else {
							if (online) ChatUtil.sendBrandingMessage((Player)target, color + this.addedAE.replace("{element}", e.toString() + e.getType().getBender()));
						}

					}
					bPlayer.saveElements();
					bPlayer.saveSubElements();
					return;

					// if it's a sub element:
				} else if (Arrays.asList(Element.getAllSubElements()).contains(e)) {
					final SubElement sub = (SubElement) e;

					if (!this.hasPermission(sender, sub.getName().toLowerCase())) {
						return;
					}

					if (bPlayer.hasSubElement(sub)) { // if already had, determine  who to send the error message to.
						if (!(sender instanceof Player) || !((Player) sender).equals(target)) {
							ChatUtil.sendBrandingMessage(sender, ChatColor.RED + this.alreadyHasSubElementOther.replace("{target}", ChatColor.DARK_AQUA + target.getName() + ChatColor.RED));
						} else {
							ChatUtil.sendBrandingMessage(sender, ChatColor.RED + this.alreadyHasSubElement);
						}
						return;
					}

					PlayerChangeSubElementEvent event = new PlayerChangeSubElementEvent(sender, target, sub, com.projectkorra.projectkorra.event.PlayerChangeSubElementEvent.Result.ADD);
					Bukkit.getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) return; // if the event is cancelled, don't add the subelement.

					bPlayer.addSubElement(sub);
					final ChatColor color = e.getColor();

					if (!(sender instanceof Player) || !((Player) sender).equals(target)) {
						if (e != Element.AIR && e != Element.EARTH) {
							ChatUtil.sendBrandingMessage(sender, color + this.addedOtherCFW.replace("{target}", ChatColor.DARK_AQUA + target.getName() + color).replace("{element}", sub.toString() + sub.getType().getBender()));
						} else {
							ChatUtil.sendBrandingMessage(sender, color + this.addedOtherAE.replace("{target}", ChatColor.DARK_AQUA + target.getName() + color).replace("{element}", sub.toString() + sub.getType().getBender()));
						}

					} else {
						if (e != Element.AIR && e != Element.EARTH) {
							if (online) ChatUtil.sendBrandingMessage((Player)target, color + this.addedCFW.replace("{element}", sub.toString() + sub.getType().getBender()));
						} else {
							if (online) ChatUtil.sendBrandingMessage((Player)target, color + this.addedAE.replace("{element}", sub.toString() + sub.getType().getBender()));
						}
					}
					bPlayer.saveSubElements();
					return;

				} else { // bad element.
					sender.sendMessage(ChatColor.RED + this.invalidElement);
				}
			}
		}).exceptionally(e -> {
			e.printStackTrace();
			return null;
		});

	}

	public static boolean isVowel(final char c) {
		return "AEIOUaeiou".indexOf(c) != -1;
	}

	@Override
	protected List<String> getTabCompletion(final CommandSender sender, final List<String> args) {
		if (args.size() >= 2 || !sender.hasPermission("bending.command.add")) {
			return new ArrayList<String>();
		}
		final List<String> l = new ArrayList<String>();
		if (args.size() == 0) {
			// add tab completion for avatar
			if (sender.hasPermission("bending.command.add." + Element.AVATAR.getName().toLowerCase())) {
				l.add(Element.AVATAR.getName());
			}

			// add tab completion for elements the player has permission to add
			List<String> elementNames = Arrays.asList("Air", "Earth", "Fire", "Water", "Chi");

			for (final String elementName : elementNames) {
				final String commandPermission = "bending.command.add." + elementName.toLowerCase();
				if (!sender.hasPermission(commandPermission)) continue;

				l.add(elementName);
			}

			// add tab completion for addon elements the player has permission to add
			for (final Element e : Element.getAddonElements()) {
				final String commandPermission = "bending.command.add." + e.getName().toLowerCase();
				if (!sender.hasPermission(commandPermission)) continue;

				l.add(e.getName());
			}

			// add tab completion for sub-elements the player has permission to add
			List<String> subelementNames = Arrays.asList("Blood", "Combustion", "Flight", "Healing", "Ice", "Lava", "Lightning", "Metal", "Plant", "Sand", "Spiritual", "BlueFire");

			for (final String subelementName : subelementNames) {
				final String commandPermission = "bending.command.add." + subelementName.toLowerCase();
				if (!sender.hasPermission(commandPermission)) continue;

				l.add(subelementName);
			}

			// add tab completion for addon sub-elements the player has permission to add
			for (final SubElement e : Element.getAddonSubElements()) {
				final String commandPermission = "bending.command.add." + e.getName().toLowerCase();
				if (!sender.hasPermission(commandPermission)) continue;

				l.add(e.getName());
			}
		} else {
			for (final Player p : Bukkit.getOnlinePlayers()) {
				l.add(p.getName());
			}
		}
		return l;
	}
}
