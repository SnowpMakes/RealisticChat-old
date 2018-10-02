package olafher.RealisticChat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.AbstractMutableMessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import com.google.inject.Inject;

@Plugin(id="realistic-chat", name="Realistic Chat", description="Chat like in real life, not everyone can hear you (clearly)!")
public class RealisticChat {
	
	@Inject
	Logger logger;
	
	@Listener
	public void onServerStarting(GamePreInitializationEvent event) {
		logger.info("Starting up and injecting changes...");
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		logger.info("/------------------------\\");
		logger.info("|     Realisitc Chat     |");
		logger.info("|       By olafher       |");
		logger.info("\\------------------------/");
		logger.info("Started!");
	}
	
	@Listener
	public void onPlayerJoin(ClientConnectionEvent.Join event) {
		Player player = event.getTargetEntity();
		
		if(!player.hasPlayedBefore()) {
			AbstractMutableMessageChannel playerMessageChannel = new AbstractMutableMessageChannel(){};
			
			playerMessageChannel.clearMembers();
			playerMessageChannel.addMember(player);
			
			playerMessageChannel.send(Text.of(""));
			playerMessageChannel.send(Text.builder("Welcome " + player.getName() + "!").color(TextColors.GOLD).build());
			playerMessageChannel.send(Text.builder("-----------------------").color(TextColors.GOLD).build());
			playerMessageChannel.send(Text.builder("This server runs Realistic Chat, other players won't be able to hear you from far.").color(TextColors.GOLD).build());
			playerMessageChannel.send(Text.builder("Add exclamation marks (!) at the cost of the hunger effect to shout!").color(TextColors.GOLD).build());
			playerMessageChannel.send(Text.builder("-----------------------").color(TextColors.GOLD).build());
			playerMessageChannel.send(Text.builder("No exclamation marks: Everyone in a 10 block radius can hear you, for player further than 5 blocks away the message will get garbled.").color(TextColors.GOLD).build());
			playerMessageChannel.send(Text.builder("Add 1 exclamation mark: 5 blocks and 5 seconds of hunger will be added.").color(TextColors.GOLD).build());
		}
	}
	
	@Listener
	public void onChat(MessageChannelEvent.Chat event) {
		Optional<Player> optplayer = event.getCause().first(Player.class);
		if(optplayer.isPresent()) {
			final Player player = optplayer.get();
			
			AbstractMutableMessageChannel clearChannel = new AbstractMutableMessageChannel(){};
			AbstractMutableMessageChannel garbledChannel = new AbstractMutableMessageChannel(){};
			clearChannel.clearMembers();
			garbledChannel.clearMembers();
			clearChannel.addMember(player);
			
			Text message = event.getMessage();
			String messageString = message.toPlain();
			
			// Function to find all occurrences of "!"
			/*
			ArrayList<Integer> occurrences = new ArrayList<Integer>();
			int index = message.indexOf("!");
			while(index >= 0) {
			   occurences.add(index);
			   index = message.indexOf("!", index+1);
			}
			*/
			
			int occurrences = 0;
			
			while((messageString.substring(messageString.length() - 1 - occurrences).equals("!") ||
				messageString.substring(messageString.length() - 1 - occurrences, messageString.length() - occurrences).equals("!")) &&
				messageString.length() - 1 - occurrences > 0
				) {
				occurrences = occurrences + 1;
			}
			
			final double clearRadius = (occurrences * 5) + 7;
			final double garbledRadius = (occurrences * 5) + 12;
			
			Collection<Entity> entities = player.getNearbyEntities(clearRadius);
			Iterator<Entity> iterator = entities.iterator();
			Entity entity;
			Player targetPlayer;
			while(iterator.hasNext()) {
				entity = iterator.next();
				if (entity.getType().equals(EntityTypes.PLAYER)) {
					targetPlayer = (Player) entity;
					clearChannel.addMember(targetPlayer);
				}
			}
			
			entities = player.getNearbyEntities(garbledRadius);
			iterator = entities.iterator();
			Collection<MessageReceiver> clearChannelMembers = clearChannel.getMembers();
			while(iterator.hasNext()) {
				entity = iterator.next();
				if (entity.getType().equals(EntityTypes.PLAYER)) {
					targetPlayer = (Player) entity;
					if(!clearChannelMembers.contains(targetPlayer)) {
						garbledChannel.addMember(targetPlayer);
					}
				}
			}
			
			if(occurrences > 0) {
				PotionEffect potion = PotionEffect.builder().potionType(PotionEffectTypes.HUNGER).duration(occurrences * 5 * 20).build();
				List<PotionEffect> list = new ArrayList<PotionEffect>();
				list.add(potion);
				player.offer(Keys.POTION_EFFECTS, list);
			}
			
			clearChannel.send(message);
			garbledChannel.send(message.toBuilder().style(TextStyles.OBFUSCATED).build());
			
			event.setCancelled(true);
		}
	}
	
	@Listener
	public void onServerStopped(GameStoppedEvent event) {
		logger.info("The server has stopped, goodbye!");
	}
	
	public void printError(String error) {
		logger.error("Something went terrably wrong!:");
		logger.error(error);
	}
	
}
