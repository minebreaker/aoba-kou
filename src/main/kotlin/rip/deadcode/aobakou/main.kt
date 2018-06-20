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
    walk(content = contentRoot, out = outputPath, setting = setting)
}

fun clean(out: Path) {
    if (Files.exists(out)) {
        print("Removing: '${out}'. Are you sure? (y/n) > ")
        if (System.`in`.read() != 'y'.toInt()) {
            return
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

fun walk(content: Path, out: Path, setting: Setting) {

    checkState(Files.isDirectory(content))

    Files.list(content).use {
        for (target in it) {
            val targetFileName = target.getFileName2()
            if (Files.isDirectory(target)) {
                walk(target, out.resolve(targetFileName), setting)
                continue
            }

            if (targetFileName.endsWith(".md")) {
                val articleOutputPath = out.resolve(target.getFileNameWithoutExtension() + ".html")
                val settingPath = target.parent.resolve(target.getFileNameWithoutExtension() + ".json")
                Files.createDirectories(out)
                Files.newBufferedWriter(articleOutputPath).use {
                    val article = generateArticle(
                            readAll(target), pageSetting = readJson(settingPath, PageSetting::class.java), setting = setting)
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

private fun readAll(path: Path): String = Files.readAllBytes(path).toString(StandardCharsets.UTF_8)

private fun Path.getFileName2(): String = this.fileName.toString()

private fun Path.getFileNameWithoutExtension(): String = this.fileName.toString().substringBeforeLast(".")
