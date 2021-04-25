import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import org.json.*;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Bot {

    private static final Map<String, Command> commands = new HashMap<>();
    private static String prefix = "!";
    public static ArrayList<RedditPost> redditMemes = new ArrayList<>(); // ArrayList holding posts from the front page of /r/memes

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
            event.getMessage().getChannel().block().createEmbed( embedCreateSpec -> {
                embedCreateSpec.setColor(Color.RUBY);
                embedCreateSpec.setTitle("Help Menu");
                embedCreateSpec.setDescription("The following is a complete list of commands: ");
                embedCreateSpec.addField("Help","`"+prefix+"help`", true);
                embedCreateSpec.addField("Change Prefix", "`"+prefix+"changeprefix [new prefix]`", true);
                embedCreateSpec.addField("Ping", "`"+prefix+"ping`", true);
                embedCreateSpec.addField("Meme from /r/memes", "`"+prefix+"meme`", true);
                embedCreateSpec.setUrl("https://github.com/kumail-r/BetterBot");
            }).block();
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
        scheduler.scheduleJob(job, trigger);
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
            Bot.generateRedditMemes();
        }
    }
}