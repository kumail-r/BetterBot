import org.json.JSONObject;

public class RedditPost {
    private String author;
    private String title;
    private long upvotes;
    private double ratio;
    private String thumbnail;
    private String url;
    private boolean over18;
    private JSONObject data;
    private String permalink;
    private long commentCount;
    private long createdUTC;

    public RedditPost(JSONObject data){
        this.data = data;
        author          = data.has("author")        ? data.getString("author") : "N/A" ;
        title           = data.has("title")         ? data.getString("title") : "N/A";
        upvotes         = data.has("ups")           ? data.getLong("ups") : 0;
        ratio           = data.has("upvote_ratio")  ? data.getDouble("upvote_ratio") : 0.0;
        thumbnail       = data.has("thumbnail")     ? data.getString("thumbnail") : "N/A";
        url             = data.has("url")           ? data.getString("url") : "N/A";
        over18          = data.has("over18")        ? data.getBoolean("over18") : false;
        permalink       = data.has("permalink")     ? data.getString("permalink") : "N/A";
        commentCount    = data.has("num_comments")  ? data.getLong("num_comments") : 0;
        createdUTC      = data.has("created_utc")   ? data.getLong("created_utc") : 0;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public long getUpvotes() {
        return upvotes;
    }

    public double getRatio() {
        return ratio;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getUrl() {
        return url;
    }

    public boolean isOver18() {
        return over18;
    }

    public JSONObject getData() {
        return data;
    }

    public String getPermalink() {
        return permalink;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public long getCreatedUTC() {
        return createdUTC;
    }

    public String getCommentsLink(){
        return "https://reddit.com" + permalink;
    }
}
