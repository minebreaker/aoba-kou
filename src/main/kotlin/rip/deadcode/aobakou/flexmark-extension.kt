package rip.deadcode.aobakou

import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererFactory
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.DataHolder
import com.vladsch.flexmark.util.options.MutableDataHolder
import com.vladsch.flexmark.util.options.MutableDataSet


class MyAnchorLinkExtension : HtmlRenderer.HtmlRendererExtension {

    override fun rendererOptions(options: MutableDataHolder) {}

    override fun extend(rendererBuilder: HtmlRenderer.Builder, rendererType: String) {
        rendererBuilder.nodeRendererFactory(MyAnchorLinkRendererFactory())
    }
}

class MyAnchorLinkRenderer : NodeRenderer {
    override fun getNodeRenderingHandlers(): MutableSet<NodeRenderingHandler<*>> {
        return mutableSetOf(
                NodeRenderingHandler(Heading::class.java) { node, context, writer ->

                    writer.withAttr()
                            .attr("id", node.anchorRefId)
                            .attr("class", "anchor")
                            .tag("h" + node.level)
                    writer.withAttr()
                            .attr("class", "anchor-link")
                            .attr("href", "#" + node.anchorRefId)
                            .tag("a")

                    context.renderChildren(node)

                    writer.tag("/a")
                    writer.tag("/h" + node.level)
                }
        )
    }
}

class MyAnchorLinkRendererFactory : NodeRendererFactory {
    override fun create(options: DataHolder): NodeRenderer {
        return MyAnchorLinkRenderer()
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

