import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;

import java.util.HashMap;
import java.util.Map;

public class Bot {

    private static final Map<String, Command> commands = new HashMap<>();
    private static String prefix = "!";

    interface Command{
        void execute(MessageCreateEvent event);
    }

    static { // ping
        commands.put("ping", event -> event.getMessage().getChannel().block().createMessage("Pong!").block());
    }
    static { // change prefix
        commands.put("changeprefix", event -> {
           String temp = event.getMessage().getContent().substring((prefix + "changeprefix").length());
           if (!temp.equals("")) {
               System.out.println(prefix);
               prefix = temp.replaceAll("\\s", "");
               event.getMessage().getChannel().block().createMessage("Prefix is now set to be `"+prefix+"`.").block();
               System.out.println(prefix);
           }
           else{
               event.getMessage().getChannel().block().createMessage("Invalid usage. `"+prefix+"changeprefix [new prefix]`").block();
           }
        });
    }
    static { // show help menu
        commands.put("help", event -> {
            event.getMessage().getChannel().block().createEmbed( embedCreateSpec -> {
                embedCreateSpec.setColor(Color.RUBY);
                embedCreateSpec.setTitle("Help Menu");
                embedCreateSpec.setDescription("The following is a complete list of commands: ");
                embedCreateSpec.addField("Help","`"+prefix+"help`", true);
                embedCreateSpec.addField("Change Prefix", "`"+prefix+"changeprefix [new prefix]`", true);
                embedCreateSpec.addField("Ping", "`"+prefix+"ping`", true);
            }).block();
        });
    }
    public static void main(String[] args) {
        GatewayDiscordClient client = DiscordClientBuilder.create(args[0])
                .build()
                .login()
                .block();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                    final String content = event.getMessage().getContent();
                    for (final Map.Entry<String, Command> entry : commands.entrySet()) {
                        if (content.startsWith(prefix + entry.getKey())) {
                            entry.getValue().execute(event);
                            break;
                        }
                    }
                });
        client.onDisconnect().block();
    }
}