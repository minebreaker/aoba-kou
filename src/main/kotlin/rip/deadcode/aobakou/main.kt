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

    val contentRoot = Paths.get(contentRootPathStr)
    val outputPath = Paths.get(outputPathStr)
    val configPath = contentRoot.resolve("setting.json")
    val setting = readJson(configPath, Setting::class.java)

    clean(outputPath)
    walk(content = contentRoot, out = outputPath, setting = setting)

}

fun clean(out: Path) {
    if (Files.exists(out)) {
        Files.walk(out).use {
            it.sorted(Comparator.reverseOrder()).forEach {
                if (it != out) {
                    Files.delete(it)
                }
            }
        }
    }
}

fun walk(content: Path, out: Path, setting: Setting) {

    checkState(Files.isDirectory(content))

    Files.list(content).use {
        for (target in it) {
            val targetFileName = target.getFileName2()
            if (Files.isDirectory(target)) {
                walk(target, out, setting)
            }

            val targetParent = target.parent
            val targetPath = if (targetParent.nameCount == 1) {
                out
            } else {
                out.resolve(targetParent.subpath(1, targetParent.nameCount))
            }
            if (targetFileName.endsWith(".md")) {
                val articleOutputPath = targetPath.resolve(target.getFileNameWithoutExtension() + ".html")
                val settingPath = target.parent.resolve(target.getFileNameWithoutExtension() + ".json")
                Files.createDirectories(targetPath)
                Files.newBufferedWriter(articleOutputPath).use {
                    val article = generateArticle(
                            readAll(target), pageSetting = readJson(settingPath, PageSetting::class.java), setting = setting)
                    it.write(article)
                }
            } else {
                val accepts = listOf(".html", ".css", ".js", ".png", ".jpg", ".gif")
                if (accepts.any { target.getFileName2().endsWith(it) }) {
                    Files.createDirectories(targetPath)
                    Files.copy(target, targetPath.resolve(target.getFileName2()))
                }
            }
        }
    }
}

private fun readAll(path: Path): String = Files.readAllBytes(path).toString(StandardCharsets.UTF_8)

private fun Path.getFileName2(): String = this.fileName.toString()

private fun Path.getFileNameWithoutExtension(): String = this.fileName.toString().substringBeforeLast(".")
