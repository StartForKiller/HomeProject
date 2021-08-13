import io.github.pieter12345.javaloader.bukkit.JavaLoaderBukkitProject;
import io.github.pieter12345.javaloader.bukkit.BukkitCommand;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class HomeProject extends JavaLoaderBukkitProject {

	YamlConfiguration dataStore = null;
	
	@Override
	public void onLoad() {
		int playersLoaded = LoadDataFile(false);

		Bukkit.getConsoleSender().sendMessage(
				ChatColor.DARK_GREEN + "[DEBUG] HomeProject project loaded. Loaded " + playersLoaded + " players." + ChatColor.RESET);
	}
	
	@Override
	public void onUnload() {
		HandlerList.unregisterAll(this.getPlugin());

		Bukkit.getConsoleSender().sendMessage(
				ChatColor.DARK_RED + "[DEBUG] HomeProject project unloaded." + ChatColor.RESET);
	}
	
	@Override
	public String getVersion() {
		return "0.0.1";
	}
	
	@Override
	public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
		if(command.getName().equalsIgnoreCase("sethome")) {
			if(sender instanceof Player) {
				Player player = (Player)sender;
				SetPlayerHome(player);
			} else {
				sender.sendMessage("[DEBUG] [HomeProject] This command can only be executed by a player");
			}
			return true;
		} else if(command.getName().equalsIgnoreCase("home")) {
			if(sender instanceof Player) {
				Player player = (Player)sender;
				TeleportPlayer(player);
			} else {
				sender.sendMessage("[DEBUG] [HomeProject] This command can only be executed by a player");
			}
			return true;
		} else if(command.getName().equalsIgnoreCase("reloadhome")) {
			int playersLoaded = LoadDataFile(true);
			sender.sendMessage(
					ChatColor.DARK_GREEN + "[DEBUG] Reloaded " + playersLoaded + " players." + ChatColor.RESET);
			return true;
		}
		
		return false;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return null;
	}
	
	@Override
	public BukkitCommand[] getCommands() {
		return new BukkitCommand[] {
				new BukkitCommand("sethome")
						.setUsageMessage("Set the user home location. Once a week.")
						.setPermission("javaloader.homeproject.sethome")
						.setPermissionMessage("You do not have permission to use this command.")
						.setExecutor(this)
						.setTabCompleter(this),
				new BukkitCommand("home")
						.setUsageMessage("Teleport to home. Once every 30 minutes.")
						.setPermission("javaloader.homeproject.home")
						.setPermissionMessage("You do not have permission to use this command.")
						.setExecutor(this)
						.setTabCompleter(this),
				new BukkitCommand("reloadhome")
						.setUsageMessage("Reload the data file.")
						.setPermission("javaloader.homeproject.reloadhome")
						.setPermissionMessage("You do not have permission to use this command.")
						.setExecutor(this)
						.setTabCompleter(this)
			};
	}

	boolean PlayerHasHome(Player player) {
		return Objects.requireNonNull(dataStore.getConfigurationSection("Players")).contains(player.getUniqueId().toString(), false);
	}

	void TeleportPlayer(Player player) {
		ConfigurationSection section;
		if(PlayerHasHome(player)) {
			section = Objects.requireNonNull(dataStore.getConfigurationSection("Players")).getConfigurationSection(player.getUniqueId().toString());
		} else {
			player.sendMessage(ChatColor.DARK_RED + "You need to set a home first" + ChatColor.RESET);
			return;
		}
		assert section != null;

		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		if(section.contains("LastExecution")) {
			try {
				Date lastExecution = formatter.parse(section.getString("LastExecution"));
				long lastMinutes = lastExecution.getTime() / (1000 * 60);
				long currentMinutes = (new Date()).getTime() / (1000 * 60);
				if((lastMinutes + 30) > currentMinutes) {
					player.sendMessage(ChatColor.DARK_RED + "You need to wait 30 minutes to execute this command again" + ChatColor.RESET);
					return;
				}
			} catch (ParseException e) { //If this happens you edited the file manually or the file is corrupted
				e.printStackTrace();
				player.sendMessage(ChatColor.DARK_RED + "[ERROR] An error occurred, please contact an administrator" + ChatColor.RESET);
				return;
			}
		}

		double startX = section.getDouble("X");
		double startY = section.getDouble("Y");
		double startZ = section.getDouble("Z");

		double x = section.getDouble("X");
		double y = section.getDouble("Y");
		double z = section.getDouble("Z");
		float yaw = (float)section.getDouble("Yaw");
		float pitch = (float)section.getDouble("Pitch");

		new BukkitRunnable() {
			int counter = 5;
			@Override
			public void run() {
				if(player.getLocation().getX() != startX || player.getLocation().getY() != startY || player.getLocation().getZ() != startZ) {
					player.sendMessage(ChatColor.DARK_RED + "Teleport canceled, don't move while waiting for teleportation." + ChatColor.RESET);
					this.cancel();
				}else if(counter == 0) {
					player.sendMessage(ChatColor.DARK_GREEN + "Teleporting now" + ChatColor.RESET);
					if(player.teleport(new Location(Bukkit.getWorld("world"), x, y, z, yaw, pitch))) {
						section.set("LastExecution", formatter.format(new Date()));
						SaveConfig();
					}
					this.cancel();
				} else {
					player.sendMessage(ChatColor.DARK_GREEN + "Teleporting in " + counter + " seconds." + ChatColor.RESET);
					counter--;
				}
			}
		}.runTaskTimer(this.getPlugin(), 0, 20);
	}

	void SetPlayerHome(Player player) {
		ConfigurationSection section;
		if(PlayerHasHome(player)) {
			section = Objects.requireNonNull(dataStore.getConfigurationSection("Players")).getConfigurationSection(player.getUniqueId().toString());
		} else {
			section = Objects.requireNonNull(dataStore.getConfigurationSection("Players")).createSection(player.getUniqueId().toString());
		}
		assert section != null;

		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		if(section.contains("LastSet")) {
			try {
				Date lastSet = formatter.parse(section.getString("LastSet"));
				long lastDays = lastSet.getTime() / (1000 * 60 * 60 * 24);
				long currentDays = (new Date()).getTime() / (1000 * 60 * 60 * 24);
				if((lastDays + 7) > currentDays) {
					player.sendMessage(ChatColor.DARK_RED + "You need to wait one week to execute this command again" + ChatColor.RESET);
					return;
				}
			} catch (ParseException e) { //If this happens you edited the file manually or the file is corrupted
				e.printStackTrace();
				player.sendMessage(ChatColor.DARK_RED + "[ERROR] An error occurred, please contact an administrator" + ChatColor.RESET);
				return;
			}
		}

		section.set("X", player.getLocation().getX());
		section.set("Y", player.getLocation().getY());
		section.set("Z", player.getLocation().getZ());
		section.set("Yaw", (double)player.getLocation().getYaw());
		section.set("Pitch", (double)player.getLocation().getPitch());

		Date date = new Date();

		section.set("LastSet", formatter.format(date));
		if(!section.contains("LastExecution")) {
			section.set("LastExecution", formatter.format(new Date(0)));
		}

		player.sendMessage(ChatColor.DARK_GREEN + "Home set successfully." + ChatColor.RESET);

		SaveConfig();
	}

	void SaveConfig() {
		File file = new File(getPlugin().getDataFolder() + File.separator + "data.yml");

		try {
			dataStore.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	int LoadDataFile(boolean reload) {
		File file = new File(getPlugin().getDataFolder() + File.separator + "data.yml");

		if(dataStore == null || reload) {
			dataStore = new YamlConfiguration();

			if(!file.exists()) {
				try {
					Files.createDirectories(file.toPath().getParent());
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				dataStore.createSection("Players");
			} else {
				try {
					dataStore.load(file);
				} catch (IOException | InvalidConfigurationException e) {
					e.printStackTrace();
				}
			}
		}
		int numberOfPlayers = Objects.requireNonNull(dataStore.getConfigurationSection("Players")).getKeys(false).size();

		try {
			dataStore.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return numberOfPlayers;
	}
}
