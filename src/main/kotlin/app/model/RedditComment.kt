package app.model

data class RedditComment(

    val id: String,

    val author: String,

    val publishedDate: String,

    val content: String,

    val url: String,

    val countOfLikes: Int,

    val isMajor: Boolean,

    val isPost: Boolean = false,

    val isComment: Boolean = true,

    val level: Int,

    val subreddit: String
)
