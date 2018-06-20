package rip.deadcode.aobakou

data class Setting(
        val site: String,
        val notification: String,
        val breadcrumb: Breadcrumb
)

data class Breadcrumb(
        val splitter: String
)

data class PageSetting(
        val title: String
)
