package rip.deadcode.aobakou

import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.util.*

private val markdownOption = MutableDataSet()
        .set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
private val markdownParser = Parser.builder(markdownOption).build()
private val htmlRenderer = HtmlRenderer.builder(markdownOption).build()

private val thResolver = ClassLoaderTemplateResolver().apply {
    suffix = ".html"
}
private val thEngine = TemplateEngine().apply {
    setTemplateResolver(thResolver)
}

private fun parseMarkdown(markdown: String): String = htmlRenderer.render(markdownParser.parse(markdown))

fun generateArticle(contentMd: String,
        headerMd: String,
        footerMd: String,
        metaMd: String,
        breadcrumb: String,
        pageSetting: PageSetting,
        setting: Setting): String {

    val content = parseMarkdown(contentMd)
    val escapedContent = Jsoup.clean(content, Whitelist.none())
    val description = if (escapedContent.length >= 96) escapedContent.substring(0, 90) + "(...)" else escapedContent
    val header = parseMarkdown(headerMd)
    val footer = parseMarkdown(footerMd)
    val meta = parseMarkdown(metaMd)

    val context = Context(Locale.getDefault(), mapOf(
            "title" to pageSetting.title,
            "site" to setting.site,
            "content" to content,
            "description" to description,
            "header" to header,
            "footer" to footer,
            "meta" to meta,
            "breadcrumb" to breadcrumb,
            "production" to false
    ))
    return thEngine.process("content", context)
}
