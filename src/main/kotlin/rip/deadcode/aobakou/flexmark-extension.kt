package rip.deadcode.aobakou

import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererFactory
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.DataHolder
import com.vladsch.flexmark.util.options.DataKey
import com.vladsch.flexmark.util.options.MutableDataHolder
import com.vladsch.flexmark.util.options.MutableDataSet


private const val defaultLinkIcon = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" viewBox=\"0 0 24 24\"><path d=\"M0 0h24v24H0z\" fill=\"none\"/><path d=\"M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z\"/></svg>"
val LINK_ICON_TAG_KEY = DataKey("rip.deadcode.aobakou.LinkIconTagKey", defaultLinkIcon)

class MyAnchorLinkExtension : HtmlRenderer.HtmlRendererExtension {

    override fun rendererOptions(options: MutableDataHolder) {}

    override fun extend(rendererBuilder: HtmlRenderer.Builder, rendererType: String) {
        rendererBuilder.nodeRendererFactory(MyAnchorLinkRendererFactory())
    }
}

class MyAnchorLinkRenderer(private val linkIconTag: String) : NodeRenderer {

    override fun getNodeRenderingHandlers(): MutableSet<NodeRenderingHandler<*>> {
        return mutableSetOf(
                NodeRenderingHandler(Heading::class.java) { node, context, writer ->

                    // Check for not nesting a tags
                    if (node.children.any { it is Link }) {
                        writer.withAttr()
                                .attr("id", node.anchorRefId)
                                .attr("class", "anchor")
                                .tag("h" + node.level)

                        context.renderChildren(node)

                        writer.tag("/h" + node.level)

                    } else {
                        writer.withAttr()
                                .attr("id", node.anchorRefId)
                                .attr("class", "anchor")
                                .tag("h" + node.level)
                        writer.withAttr()
                                .attr("class", "anchor-link")
                                .attr("href", "#" + node.anchorRefId)
                                .tag("a")

                        context.renderChildren(node)

                        writer.append(linkIconTag)
                        writer.tag("/a")
                        writer.tag("/h" + node.level)
                    }
                }
        )
    }
}

class MyAnchorLinkRendererFactory : NodeRendererFactory {
    override fun create(options: DataHolder): NodeRenderer {
        return MyAnchorLinkRenderer(options[LINK_ICON_TAG_KEY])
    }
}

fun main() {

    val options = MutableDataSet().set(
            Parser.EXTENSIONS, listOf(MyAnchorLinkExtension()))
//            Parser.EXTENSIONS, listOf(AnchorLinkExtension.create()))
//            .set(AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS, "hoge")
    val markdownParser = Parser.builder(options).build()
    val htmlRenderer = HtmlRenderer.builder(options).build()

    val node = markdownParser.parse("""
        ## hoge

        abc

        ## ぴよ

        def
    """.trimIndent())

    val result = htmlRenderer.render(node)
    println(result)
}

