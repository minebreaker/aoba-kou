package rip.deadcode.aobakou

import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
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

fun generateArticle(markdown: String, pageSetting: PageSetting, setting: Setting): String {
    val nodes = markdownParser.parse(markdown)
    val content = htmlRenderer.render(nodes)
    val description = if (content.length >= 60) content.substring(0, 64) + "(...)" else content

    val context = Context(Locale.getDefault(), mapOf(
            "title" to pageSetting.title,
            "site" to setting.site,
            "content" to content,
            "description" to description,
            "production" to false
    ))
    return thEngine.process("content", context)
}
