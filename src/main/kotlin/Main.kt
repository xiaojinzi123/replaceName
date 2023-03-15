import java.io.File
import java.io.InputStreamReader
import java.util.*

const val ARG_HELP = "help"
const val MODE_REPLACE = "r"
const val MODE_PREFIX_REPLACE = "pr"
const val MODE_SUFFIX_REPLACE = "sr"


/**
 * m: 模式 支持 replace, prefixReplace, suffixReplace
 */
fun main(args: Array<String>) {

    // 组成命令的参数
    val command = args.joinToString(separator = " ").lowercase(Locale.getDefault())

    if (command.contains(other = "-$ARG_HELP") || command.contains(other = "--$ARG_HELP")) {
        println("replaceName -m [r, pr, sr] -from [from] -to [to]")
        println("r: replace")
        println("pr: prefixReplace")
        println("sr: suffixReplace")
        return
    }

    println("command = replaceName $command")

    val pattern = "-(\\w+)\\s(\\S+)"
    val regex = Regex(pattern)
    val matches = regex.findAll(command)

    val argMap = mutableMapOf<String, String>()
    for (match in matches) {
        val (name, value) = match.destructured
        argMap[name] = value
    }

    println("commandArgMap = $argMap")

    // 如果没有模式的参数, 就提示错误
    if (!argMap.containsKey(key = "m")) {
        throw IllegalArgumentException("args -m not found")
    }

    // 获取命令调用的目录
    val targetFolderPath = InputStreamReader(Runtime.getRuntime().exec("pwd").inputStream).readText().trim()
    val folderFile = File(targetFolderPath)

    // 对这个文件夹下的所有文件进行重命名

    val replaceMode = argMap["m"]!!

    println("replaceMode = $replaceMode")

    // 对各种模式的参数进行校验
    when (replaceMode) {

        MODE_REPLACE, MODE_PREFIX_REPLACE, MODE_SUFFIX_REPLACE -> {
            if (!argMap.containsKey(key = "from")) {
                throw IllegalArgumentException("args -from not found")
            }
            if (!argMap.containsKey(key = "to")) {
                throw IllegalArgumentException("args -to not found")
            }
        }

        else -> {
            throw IllegalArgumentException("replaceMode = $replaceMode not support")
        }

    }

    renameFolder(folderFile = folderFile, replaceMode = replaceMode, argMap = argMap)

}

/**
 * 递归重命名此文件夹中的所有文件
 */
fun renameFolder(folderFile: File, replaceMode: String, argMap: Map<String, String>) {
    folderFile
        .listFiles()
        ?.forEach { itemFile ->
            if (itemFile.isDirectory) {
                renameFolder(folderFile = itemFile, replaceMode = replaceMode, argMap = argMap)
            } else {
                // 如果是文件, 就重命名
                renameByMode(
                    targetFile = itemFile,
                    replaceMode = replaceMode,
                    argMap = argMap
                )
            }
        }
}

/**
 * 根据模式重命名
 */
fun renameByMode(targetFile: File, replaceMode: String, argMap: Map<String, String>) {
    val from = argMap["from"]!!
    val to = argMap["to"]!!
    when (replaceMode) {

        MODE_REPLACE -> {
            val nameWithoutExtension = targetFile.nameWithoutExtension.replace(
                oldValue = from, newValue = to, ignoreCase = false,
            )
            val targetName = when (val fileExtension = targetFile.extension) {
                "" -> {
                    nameWithoutExtension
                }

                else -> {
                    "$nameWithoutExtension.$fileExtension"
                }
            }
            targetFile.renameTo(
                File(
                    targetFile.parentFile,
                    targetName
                )
            )
        }

        MODE_PREFIX_REPLACE -> {
            val nameWithoutExtension = targetFile.nameWithoutExtension
            // 如果不包含 from, 就不作处理, 如果包含, 就替换前缀
            if (nameWithoutExtension.startsWith(prefix = from, ignoreCase = false)) {
                val targetNameWithoutExtension = "$to${nameWithoutExtension.substring(from.length)}"
                val targetName = when (val fileExtension = targetFile.extension) {
                    "" -> {
                        targetNameWithoutExtension
                    }

                    else -> {
                        "$targetNameWithoutExtension.$fileExtension"
                    }
                }
                targetFile.renameTo(
                    File(
                        targetFile.parentFile,
                        targetName
                    )
                )
            }
        }

        MODE_SUFFIX_REPLACE -> {
            val nameWithoutExtension = targetFile.nameWithoutExtension
            // 如果不包含 from, 就不作处理, 如果包含, 就替换后缀
            if (nameWithoutExtension.endsWith(suffix = from, ignoreCase = false)) {
                val targetNameWithoutExtension =
                    "${nameWithoutExtension.substring(0, nameWithoutExtension.length - from.length)}$to"
                val targetName = when (val fileExtension = targetFile.extension) {
                    "" -> {
                        targetNameWithoutExtension
                    }

                    else -> {
                        "$targetNameWithoutExtension.$fileExtension"
                    }
                }
                targetFile.renameTo(
                    File(
                        targetFile.parentFile,
                        targetName
                    )
                )
            }
        }

        else -> {
            throw IllegalArgumentException("replaceMode = $replaceMode not support")
        }
    }
}
