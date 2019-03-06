package rip.deadcode.aobakou

import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ext.emoji.EmojiExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.options.MutableDataSet
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.util.*

private val markdownOption = MutableDataSet().set(Parser.EXTENSIONS, listOf(
        TablesExtension.create(),
        FootnoteExtension.create(),
        EmojiExtension.create(),
        MyAnchorLinkExtension()
))
private val markdownParser = Parser.builder(markdownOption).build()
private val htmlRenderer = HtmlRenderer.builder(markdownOption).build()

private val thResolver = ClassLoaderTemplateResolver().apply {
    suffix = ".html"
}
private val thEngine = TemplateEngine().apply {
    setTemplateResolver(thResolver)
}

private fun parseMarkdown(markdown: String): String = htmlRenderer.render(markdownParser.parse(markdown))

data class ArticleResult(val articleHtml: String, val headers: List<Heading>)

fun generateArticle(contentMd: String,
        headerMd: String,
        footerMd: String,
        metaMd: String,
        breadcrumb: String,
        pageSetting: PageSetting,
        setting: Setting): ArticleResult {

    val nodes = markdownParser.parse(contentMd)
    val content = htmlRenderer.render(nodes)
    val headers = flattenHeaders(nodes)
    val index = renderHeaders(headers)
    val escapedContent = Jsoup.clean(content, Whitelist.none())
    val description = if (escapedContent.length >= 96) escapedContent.substring(0, 90) + "(...)" else escapedContent
    val header = parseMarkdown(headerMd)
    val footer = parseMarkdown(footerMd)
    val meta = parseMarkdown(metaMd)

    val context = Context(Locale.getDefault(), mapOf(
            "lang" to setting.lang,
            "title" to pageSetting.title,
            "site" to setting.site,
            "content" to content,
            "index" to index,
            "description" to description,
            "header" to header,
            "footer" to footer,
            "meta" to meta,
            "breadcrumb" to breadcrumb,
            "production" to false
    ))
    return ArticleResult(thEngine.process("content", context), headers)
}

private fun flattenHeaders(node: Node): List<Heading> {

    val current = if (node is Heading) listOf(node) else listOf()
    val children = node.children.flatMap { flattenHeaders(it) }

    return current + children
}

private data class Result(val result: String, val index: Int)

private infix fun String.at(i: Int) = Result(this, i)

private fun renderHeaders(headers: List<Heading>): String {

    // Treat h1 and h2 as a same level. h4 or lesser are ignored.
    // First h3 should be treated as toplevel.
    /*
    <ul>
        <li>
            <a> H1 & H2
            <ul>
                <li><a>H3</a></li>
            </ul>
        </li>
    </ul>
     */

    val result = renderTopLevelHeader(headers, 0)

    return if (result.isNotEmpty()) {
        """<ul class="">
${result}</ul>"""
    } else {
        ""
    }
}

private fun renderTopLevelHeader(headers: List<Heading>, i: Int): String {

    if (i >= headers.size) {
        return ""
    }

    val (childElements, nextIndex) = renderChildren(headers, i + 1)
    val children = if (childElements.isNotEmpty()) {
        "\n    <ul>${childElements}\n    </ul>"
    } else {
        ""
    }

    val next = renderTopLevelHeader(headers, nextIndex)

    return """<li>
    ${headerToLinkTag(headers[i])}${children}
</li>
${next}"""
}

private fun renderChildren(headers: List<Heading>, i: Int): Result {

    if (i >= headers.size) {
        return "" at i + 1
    }

    val header = headers[i]
    return when {
        header.level == 3 -> {
            val next = renderChildren(headers, i + 1)
            "\n        " + """<li>${headerToLinkTag(header)}</li>${next.result}""" at next.index
        }
        header.level >= 4 -> renderChildren(headers, i + 1)
        else -> "" at i
    }
}

private fun headerToLinkTag(header: Heading): String {
    return """<a href="#${header.anchorRefId}">${header.anchorRefText}</a>"""
}
