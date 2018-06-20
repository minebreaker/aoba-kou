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

    clean(outputPath)
    walk(contentRoot, content = contentRoot, out = outputPath, setting = setting)
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

private val copyingExtensions = listOf(".html", ".css", ".js", ".png", ".jpg", ".gif")

fun walk(base: Path, content: Path, out: Path, setting: Setting) {

    checkState(Files.isDirectory(content))

    Files.list(content).use {
        it.forEach { target ->
            val targetFileName = target.getFileName2()
            if (Files.isDirectory(target)) {
                walk(base, target, out.resolve(targetFileName), setting)
                return@forEach
            }

            if (targetFileName.endsWith(".md")) {
                val articleOutputPath = out.resolve(target.getFileNameWithoutExtension() + ".html")
                val settingPath = target.parent.resolve(target.getFileNameWithoutExtension() + ".json")
                Files.createDirectories(out)

                val pageSetting = readJson(settingPath, PageSetting::class.java)

                val breadcrumb = pathToBreadcrumb(base, target, pageSetting.title, setting)

                Files.newBufferedWriter(articleOutputPath).use {
                    val article = generateArticle(
                            readAll(target), breadcrumb, pageSetting = pageSetting, setting = setting)
                    it.write(article)
                }
            } else {
                if (copyingExtensions.any { target.getFileName2().endsWith(it) }) {
                    Files.createDirectories(out)
                    Files.copy(target, out.resolve(target.getFileName2()))
                }
            }
        }
    }
}

private fun pathToBreadcrumb(base: Path, target: Path, currentPageName: String, setting: Setting): String {

    val path = base.relativize(target)
    val elements = (1 until path.nameCount).map {
        path.subpath(0, it)
    }.map {
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

private fun Path.getFileName2(): String = this.fileName.toString()

private fun Path.getFileNameWithoutExtension(): String = this.fileName.toString().substringBeforeLast(".")
