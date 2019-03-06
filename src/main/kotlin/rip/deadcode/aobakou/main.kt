package rip.deadcode.aobakou;

import com.google.common.base.Preconditions.checkState
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {

    checkState(args.size == 2)
    val contentRootPathStr = checkNotNull(args[0])
    val outputPathStr = checkNotNull(args[1])

    val contentRoot = Paths.get(contentRootPathStr).toAbsolutePath()
    val outputPath = Paths.get(outputPathStr).toAbsolutePath()
    val configPath = contentRoot.resolve("setting.json")
    val setting = readJson(configPath, Setting::class.java)

    val meta = readAllIfExist(contentRoot.resolve("meta.md"))
    val header = readAllIfExist(contentRoot.resolve("header.md"))
    val footer = readAllIfExist(contentRoot.resolve("footer.md"))

    clean(outputPath)

    val indexList = walk(contentRoot, content = contentRoot, header = header, footer = footer, meta = meta, out = outputPath, setting = setting)

    /*
    [ {"title": "ページタイトル", "headers": [ {"path": "URL", "header": "ヘッダーテキスト"} ]} ]
     */
    Files.newBufferedWriter(outputPath.resolve("searchIndex.json"), StandardCharsets.UTF_8).use {
        gson.toJson(indexList, it)
    }
}

fun clean(out: Path) {
    if (Files.exists(out)) {
        print("Removing: '${out}'. Are you sure? (y/n) > ")
        if (System.`in`.read() != 'y'.toInt()) {
            System.exit(0)
        }
        Files.walk(out).use {
            it.sorted(Comparator.reverseOrder()).forEach {
                if (it != out) {
                    Files.delete(it)
                }
            }
        }
    }
}

private val preservedFileNames = listOf("header.md", "footer.md", "meta.md", "setting.md")
private val copyingExtensions = listOf(".html", ".css", ".js", ".json", ".png", ".jpg", ".gif")

fun walk(base: Path, content: Path, header: String, footer: String, meta: String, out: Path, setting: Setting): List<Map<String, Any>> {

    checkState(Files.isDirectory(content))

    val indexList = mutableListOf<Map<String, Any>>()

    Files.list(content).use {
        it.forEach { target ->
            val targetFileName = target.getFileName2()

            if (Files.isDirectory(target)) {
                val result = walk(base, target, header, footer, meta, out.resolve(targetFileName), setting)
                indexList.addAll(result)

            } else if (targetFileName.endsWith(".md") && !preservedFileNames.contains(targetFileName)) {
                val articleOutputPath = out.resolve(target.getFileNameWithoutExtension() + ".html")
                val settingPath = target.parent.resolve(target.getFileNameWithoutExtension() + ".json")
                Files.createDirectories(out)

                val pageSetting = readJson(settingPath, PageSetting::class.java)

                val breadcrumb = pathToBreadcrumb(base, target, pageSetting.title, setting)
                val absoluteUrlStr = target.toString().substring(base.toString().length)

                Files.newBufferedWriter(articleOutputPath).use {
                    val (article, index) = generateArticle(
                            readAll(target), header, footer, meta, breadcrumb, pageSetting = pageSetting, setting = setting)
                    it.write(article)

                    indexList.add(mapOf(
                            "title" to pageSetting.title,
                            "headers" to index.map {
                                mapOf(
                                        "path" to (absoluteUrlStr + "#" + it.anchorRefId).replace('\\', '/'),
                                        "header" to it.anchorRefText)
                            }))
                }

            } else {
                if (copyingExtensions.any { target.getFileName2().endsWith(it) }) {
                    Files.createDirectories(out)
                    Files.copy(target, out.resolve(target.getFileName2()))
                }
            }
        }
    }

    return indexList
}

private fun pathToBreadcrumb(base: Path, target: Path, currentPageName: String, setting: Setting): String {

    val path = base.relativize(target)
    val elements = (1 until path.nameCount).map {
        path.subpath(0, it)
    }.map {
        // TODO なんか連続するindexが無視される
        val parentDir = base.resolve(it)
        if (Files.exists(parentDir.resolve("index.md"))) {
            if (target.endsWith("index.md")) {
                ""
            } else {
                """<a href="/${it.toString().replace("\\", "/")}">${it.last()}</a>"""
            }
        } else {
            it.last().toString()
        }
    }.filter {
        it.isNotEmpty()
    }.toList()
    return if (target.parent == base) {
        ""
    } else {
        (listOf("""<a href="/">${setting.site}</a>""") + elements + currentPageName).joinToString(setting.breadcrumb.splitter)
    }
}

private fun readAll(path: Path): String = Files.readAllBytes(path).toString(StandardCharsets.UTF_8)

private fun readAllIfExist(path: Path): String = if (Files.exists(path)) readAll(path) else ""

private fun Path.getFileName2(): String = this.fileName.toString()

private fun Path.getFileNameWithoutExtension(): String = this.fileName.toString().substringBeforeLast(".")
