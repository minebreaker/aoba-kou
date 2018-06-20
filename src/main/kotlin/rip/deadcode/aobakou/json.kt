package rip.deadcode.aobakou

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path

val gson: Gson = GsonBuilder().create()

fun <T> readJson(path: Path, cls: Class<T>): T {
    Files.newInputStream(path).use {
        return gson.fromJson(InputStreamReader(it), cls)
    }
}
