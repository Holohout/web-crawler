package app.model

data class RedditPost(

    val id: String,

    val title: String,

    val author: String,

    val publishedDate: String,

    val content: String,

    val url: String,

    val subreddit: String,

    val countOfLikes: Int,

    val countOfComments: Int,

    val isPost: Boolean = true,

    val isComment: Boolean = false
)
