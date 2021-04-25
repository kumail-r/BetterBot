import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import org.json.*;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import util.mentions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static util.mentions.mentionUserString;

public class Bot {

    private static final Map<String, Command> commands = new HashMap<>();
    private static String prefix = "!";
    private static ArrayList<RedditPost> redditMemes = new ArrayList<>(); // ArrayList holding posts from the front page of /r/memes

    interface Command{
        void execute(MessageCreateEvent event);

    }
    static { // poll
        commands.put("poll", event -> {
            if (event.getMessage().getContent().startsWith(prefix + "poll ")){
                String[] emojis = {null,"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟"};
                ArrayList<String> elements = new ArrayList<>(Arrays.asList(event.getMessage().getContent().substring((prefix + "poll ").length()).split(" ")));
                if (elements.size()>= 3 && elements.size() <= 11){
                    Snowflake pollPost = event.getMessage().getChannel().block().createEmbed(embedCreateSpec -> {
                        embedCreateSpec.setColor(Color.CINNABAR);
                        embedCreateSpec.setTitle("Poll");
                        embedCreateSpec.setDescription(elements.get(0));
                        for (int i = 1; i < elements.size(); i++){
                            embedCreateSpec.addField(emojis[i], elements.get(i), true);
                        }
                    }).block().getId();

                    for (int i = 1; i < elements.size(); i++){
                        event.getMessage().getChannel().block().getMessageById(pollPost).block().addReaction(ReactionEmoji.unicode(emojis[i])).subscribe();
                    }
                    return;
                }
            }
            event.getMessage().getChannel().block().createMessage("Usage: `" + prefix + "poll [description] [option 1] [option 2] ... [option 10]`\nEach option and description may not contain anyn spaces." +
                    "\nFor more details, use `" + prefix + "help poll`.").block();
        });
    }
    static { // ping
        commands.put("ping", event -> event.getMessage().getChannel().block().createMessage("Pong! " + mentionUserString(event.getMessage().getUserData())).block());
    }
    static { // change prefix
        commands.put("changeprefix", event -> {
           String temp = event.getMessage().getContent().substring((prefix + "changeprefix").length());
           if (!temp.equals("")) {
               prefix = temp.replaceAll("\\s", "");
               event.getMessage().getChannel().block().createMessage("Prefix is now set to be `"+prefix+"`.").block();
           }
           else{
               event.getMessage().getChannel().block().createMessage("Invalid usage. `"+prefix+"changeprefix [new prefix]`").block();
           }
        });
    }
    static { // show help menu
        commands.put("help", event -> {
            if (event.getMessage().getContent().equals(prefix + "help meme")){ // help meme
                event.getMessage().getChannel().block().createEmbed( embedCreateSpec -> {
                    embedCreateSpec.setColor(Color.RUBY);
                    embedCreateSpec.setTitle("Meme Help");
                    embedCreateSpec.setDescription("This command posts a random image from /r/memes from the current top 25. List of posts refreshes every 10 minutes.");
                    embedCreateSpec.addField("Syntax", "`"+prefix+"meme`", false);
                }).block();
            }
            else if (event.getMessage().getContent().equals(prefix + "help changeprefix")){ // help changeprefix
                event.getMessage().getChannel().block().createEmbed( embedCreateSpec -> {
                    embedCreateSpec.setColor(Color.RUBY);
                    embedCreateSpec.setTitle("Prefix Help");
                    embedCreateSpec.setDescription("This command changes the prefix used by Better Bot. " +
                            "Note: any whitespace used in the prefix is ignored." +
                            "Note 2: Please note that if the prefix is set to be too long certain features of the bot might break.");
                    embedCreateSpec.addField("Syntax", "`"+prefix+"changeprefix [new prefix]`", false);
                }).block();
            }
            else if (event.getMessage().getContent().equals(prefix + "help ping")){ // help ping
                event.getMessage().getChannel().block().createEmbed( embedCreateSpec -> {
                    embedCreateSpec.setColor(Color.RUBY);
                    embedCreateSpec.setTitle("Ping Help");
                    embedCreateSpec.setDescription("Responds with pong.");
                    embedCreateSpec.addField("Syntax", "`"+prefix+"ping`", false);
                }).block();
            }
            else if (event.getMessage().getContent().equals(prefix + "help help")){ // help help

            }
            else{
                event.getMessage().getChannel().block().createEmbed( embedCreateSpec -> {
                    embedCreateSpec.setColor(Color.RUBY);
                        embedCreateSpec.setTitle("Help Menu");
                        embedCreateSpec.setDescription("The following is a complete list of commands:\n For more details, type `" + prefix + "help [command]`");
                        embedCreateSpec.addField("Help","`"+prefix+"help`", true);
                        embedCreateSpec.addField("Change Prefix", "`"+prefix+"changeprefix [new prefix]`", true);
                        embedCreateSpec.addField("Ping", "`"+prefix+"ping`", true);
                        embedCreateSpec.addField("Reddit Meme ", "`"+prefix+"meme`", true);
                        embedCreateSpec.setUrl("https://github.com/kumail-r/BetterBot");
                }).block();
            }
        event.getMessage().getChannel().block().createMessage(mentionUserString(event.getMessage().getUserData())).block();
        });
    }
    static { // post a meme from the front page of /r/memes
        commands.put("meme", event -> {
            if (redditMemes.isEmpty()){
                if (generateRedditMemes())
                    return;
            }
            RedditPost post = redditMemes.get((int)(Math.random() * redditMemes.size()));
            event.getMessage().getChannel().block().createEmbed(embedCreateSpec -> {
                embedCreateSpec.setColor(Color.LIGHT_SEA_GREEN);
                embedCreateSpec.setTitle(post.getTitle());
                embedCreateSpec.setAuthor(post.getAuthor(), "https://reddit.com/u/" + post.getAuthor(), null);
                embedCreateSpec.setImage(post.getUrl());
                embedCreateSpec.setFooter(post.getUpvotes() + " \uD83D\uDC4D\t" + post.getCommentCount() + " \uD83D\uDCAC\t" + (new Date(post.getCreatedUTC() * 1000)).toString() + "\uD83D\uDD53" , null);
            }).block();
        });
    }

    public static boolean generateRedditMemes(){ // populate redditMemes ArrayList with memes from the trending page of /r/memes and returns true if failed to populate array
        HttpURLConnection connection;
        BufferedReader reader;
        String line;
        StringBuffer responseContent = new StringBuffer();
        boolean failFlag = false;
        try {
            URL url = new URL("https://www.reddit.com/r/memes.json");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("http.agent","Better Bot:kumail-r:v0.3 (by /u/raimimemer69)");

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int status = connection.getResponseCode();
            if (status > 299) {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                failFlag = true;
            }
            else{
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            }
            while((line = reader.readLine()) != null){
                responseContent.append(line);
            }
            reader.close();
            if (!failFlag){ // if successfully read from reddit
                redditMemes = new ArrayList<>(); // reset meme list
                JSONObject jsonObject = new JSONObject(responseContent.toString());
                JSONObject jsonTemp = jsonObject.getJSONObject("data");
                JSONArray posts = jsonTemp.getJSONArray("children");
                for (int i = 0; i < posts.length(); i++){
                    JSONObject post = posts.getJSONObject(i);
                    JSONObject data = post.getJSONObject("data");
                    redditMemes.add(new RedditPost(data));
                    System.out.println("object added..."); // for testing
                }
                System.out.println("Exited loop..."); // for testing
            }else{
                System.out.println("FAILURE: " +responseContent.toString()); // for testing
            }
            return failFlag;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void main(String[] args) throws SchedulerException {
        Scheduler scheduler = new StdSchedulerFactory().getDefaultScheduler();
        scheduler.start();
        JobDetail job = newJob(PopulateArrayJob.class).withIdentity("populate-array").build();
        SimpleTrigger trigger = newTrigger().withIdentity("trigger1").startNow().withSchedule(simpleSchedule().withIntervalInMinutes(10).repeatForever()).build();
        // TEMPORARILY commented out so that I don't spam reddit servers while testing other features of the bot
        //scheduler.scheduleJob(job, trigger);
        GatewayDiscordClient client = DiscordClientBuilder.create(args[0]).build().login().block();
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
        scheduler.shutdown();
    }

    public static class PopulateArrayJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                while(Bot.generateRedditMemes()) {
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}