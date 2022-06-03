package com.bomber.swan.plugin.trace

import com.android.build.api.transform.Status
import com.android.utils.FileUtils
import com.bomber.swan.javalib.util.IOUtil
import com.bomber.swan.javalib.util.Log
import com.bomber.swan.javalib.util.Util
import com.bomber.swan.plugin.trace.item.TraceMethod
import com.bomber.swan.plugin.trace.retrace.MappingCollector
import com.bomber.swan.plugin.trace.retrace.MappingReader
import com.google.common.hash.Hashing
import org.gradle.api.Project
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

class SwanTrace(
    private val ignoreMethodMapFilePath: String,
    private val methodMapFilePath: String,
    private val baseMethodMapPath: String,
    private val blockListFilePath: String,
    private val mappingDir: String,
    private val project: Project

) {


    /**
     * [classInputs] transform 输入的文件
     * [changedFiles] 改动的文件
     * [inputToOutput] transform 输入文件对应的输出文件
     * [skipCheckClass] 跳过检查 class
     * [traceClassDirectoryOutput] transform 的根目录，[inputToOutput]中的文件存放在该目录
     */
    fun doTransform(
        classInputs: Collection<File>,
        changedFiles: Map<File, Status>,
        inputToOutput: Map<File, File>,
        isIncremental: Boolean,
        skipCheckClass: Boolean,
        traceClassDirectoryOutput: File,
        legacyReplaceChangedFile: ((File, Map<File, Status>) -> Any)?,
        legacyReplaceFile: ((File, File) -> Any)?,
        uniqueOutputName: Boolean
    ) {
        val executor = Executors.newFixedThreadPool(16)
        val config = Configuration.Builder()
            .setIgnoreMethodMapFilePath(ignoreMethodMapFilePath)
            .setMethodMapFilePath(methodMapFilePath)
            .setBaseMethodMap(baseMethodMapPath)
            .setBlockListFile(blockListFilePath)
            .setMappingPath(mappingDir)
            .setSkipCheckClass(skipCheckClass)
            .build()

        /**
         * step 1
         */
        var start = System.currentTimeMillis()

        val futures = LinkedList<Future<*>>()

        val mappingCollector = MappingCollector()
        val methodId = AtomicInteger(0)
        val collectedMethodMap = ConcurrentHashMap<String, TraceMethod>()

        futures.add(
            executor.submit(
                ParseMappingTask(
                    mappingCollector,
                    collectedMethodMap,
                    methodId,
                    config
                )
            )
        )

        /**
         * dirInputOutMap 需要进行插桩的 class 文件
         */
        val dirInputOutMap = ConcurrentHashMap<File, File>()

        /**
         * jarInputOutMap 需要进行插桩的 class 文件
         */
        val jarInputOutMap = ConcurrentHashMap<File, File>()

        for (file in classInputs) {
            if (file.isDirectory) {
                futures.add(
                    executor.submit(
                        CollectDirectoryInputTask(
                            file,
                            changedFiles,
                            inputToOutput,
                            isIncremental,
                            traceClassDirectoryOutput,
                            legacyReplaceChangedFile,
                            legacyReplaceFile,
                            dirInputOutMap
                        )
                    )
                )
            } else {
                val status = Status.CHANGED
                futures.add(
                    executor.submit(
                        CollectJarInputTask(
                            inputJar = file,
                            inputJarStatus = status,
                            inputToOutput = inputToOutput,
                            isIncremental = isIncremental,
                            traceClassFileOutput = traceClassDirectoryOutput,
                            legacyReplaceFile = legacyReplaceFile,
                            uniqueOutputName = uniqueOutputName,

                            // result
                            resultOfDirInputToOut = dirInputOutMap,
                            resultOfJarInputToOut = jarInputOutMap
                        )
                    )
                )
            }
        }

        for (future in futures) {
            future.get()
        }
        futures.clear()

        Log.i(TAG, "[doTransform] Step(1)[Parse]... cost:%sms", System.currentTimeMillis() - start)

        Log.d(TAG, "dirInputOutMap: $dirInputOutMap")
        Log.d(TAG, "jarInputOutMap: $jarInputOutMap")

        /**
         * step 2
         */
        start = System.currentTimeMillis()
        val methodCollector =
            MethodCollector(executor, mappingCollector, methodId, config, collectedMethodMap)

        methodCollector.collect(dirInputOutMap.keys, jarInputOutMap.keys)
        Log.i(
            TAG,
            "[doTransform] Step(2)[Collection]... cost:%sms",
            System.currentTimeMillis() - start
        )

        /**
         * step 3
         */
        start = System.currentTimeMillis()
        val methodTracer = MethodTracer(
            executor,
            mappingCollector,
            config,
            methodCollector.collectedMethodMap,
            methodCollector.collectedClassExtendMap
        )
        val allInputs = ArrayList<File>().also {
            it.addAll(dirInputOutMap.keys)
            it.addAll(jarInputOutMap.keys)
        }
        val traceClassLoader = TraceClassLoader.getClassLoader(project, allInputs)
        methodTracer.trace(dirInputOutMap, jarInputOutMap, traceClassLoader, skipCheckClass)

        Log.i(TAG, "[doTransform] Step(3)[Trace]... cost:%sms", System.currentTimeMillis() - start)

    }

    /**
     * 解析 mapping.txt 文件
     */
    class ParseMappingTask(
        private val mappingCollector: MappingCollector,
        private val collectedMethodMap: ConcurrentHashMap<String, TraceMethod>,
        private val methodId: AtomicInteger,
        private val config: Configuration
    ) : Runnable {
        override fun run() {
            val start = System.currentTimeMillis()

            val mappingFile = File(config.mappingDir, "mapping.txt")
            /**
             * 读取混淆 mapping 文件
             */
            if (mappingFile.isFile) {
                val mappingReader = MappingReader(mappingFile)
                mappingReader.read(mappingCollector)
            }
            /**
             * 读取不进行插桩的 package class
             */
            val size = config.parseBlockFile(mappingCollector)

            val baseMethodMapFile = File(config.baseMethodMapPath)
            getMethodFromBaseMethod(baseMethodMapFile, collectedMethodMap)
            retraceMethodMap(mappingCollector, collectedMethodMap)

            Log.i(
                TAG,
                "[ParseMappingTask#run] cost:%sms, black size:%s, collect %s method from %s",
                System.currentTimeMillis() - start,
                size,
                collectedMethodMap.size,
                config.baseMethodMapPath
            )
        }


        /**
         * 读取固定id的方法
         * {id},{access} {{class name} {method name} {desc}}
         */
        private fun getMethodFromBaseMethod(
            baseMethodMapFile: File,
            collectedMethodMap: ConcurrentHashMap<String, TraceMethod>
        ) {
            if (!baseMethodMapFile.exists()) {
                Log.w(TAG, "[getMethodFromBaseMethod] not exist!%s", baseMethodMapFile.absolutePath)
                return
            }

            try {

                try {
                    Scanner(baseMethodMapFile, "UTF-8").use { fileReader ->
                        while (fileReader.hasNext()) {
                            var nextLine = fileReader.nextLine()
                            if (!Util.isNullOrNil(nextLine)) {
                                nextLine = nextLine.trim()
                                if (nextLine.startsWith("#")) {
                                    Log.i("[getMethodFromBaseMethod] comment %s", nextLine)
                                    continue
                                }
                                val fields = nextLine.split(",")
                                val traceMethod = TraceMethod()
                                traceMethod.id = Integer.parseInt(fields[0])
                                traceMethod.accessFlag = Integer.parseInt(fields[1])
                                val methodField = fields[2].split(" ")
                                traceMethod.className = methodField[0].replace("/", ".")
                                traceMethod.methodName = methodField[1]
                                if (methodField.size > 2) {
                                    traceMethod.desc = methodField[2].replace("/", ".")
                                }
                                collectedMethodMap[traceMethod.getMethodName()] = traceMethod
                                if (methodId.get() < traceMethod.id && traceMethod.id != TraceBuildConstants.METHOD_ID_DISPATCH) {
                                    methodId.set(traceMethod.id)
                                }

                            }
                        }
                    }
                } catch (e: Throwable) {
                    Log.printErrStackTrace(TAG, e, "")
                }

            } catch (e: Exception) {
                Log.printErrStackTrace(TAG, e, "")
            }


        }

        private fun retraceMethodMap(
            mappingCollector: MappingCollector,
            collectedMethodMap: ConcurrentHashMap<String, TraceMethod>
        ) {
            val retraceMethodMap = mutableMapOf<String, TraceMethod>()
            for (traceMethod in collectedMethodMap.values) {
                traceMethod.proguard(mappingCollector)
                retraceMethodMap[traceMethod.getMethodName()] = traceMethod
            }
            collectedMethodMap.clear()
            collectedMethodMap.putAll(retraceMethodMap)
            retraceMethodMap.clear()

        }
    }

    class CollectDirectoryInputTask(
        private val directoryInput: File,
        private val mapOfChangedFiles: Map<File, Status>,
        private val mapOfInputToOut: Map<File, File>,
        private val isIncremental: Boolean,
        private val traceClassDirectoryOutput: File,
        private val legacyReplaceChangedFile: ((File, Map<File, Status>) -> Any)?,
        private val legacyReplaceFile: ((File, File) -> Any)?,
        private val resultOfDirInputToOut: MutableMap<File, File>
    ) : Runnable {
        override fun run() {
            try {
                handler()
            } catch (e: Exception) {
                Log.printErrStackTrace(TAG, e, "")
            }
        }

        private fun handler() {
            val dirInput = directoryInput
            val dirOutput = if (mapOfInputToOut.containsKey(dirInput))
                mapOfInputToOut[dirInput]!!
            else
                File(traceClassDirectoryOutput, dirInput.name)
            val inputFullPath = dirInput.absolutePath
            val outputFullPath = dirOutput.absolutePath

            Log.d(TAG, "CollectDirectoryInputTask input %s -> output %s", dirInput, dirOutput)


            if (!dirOutput.exists()) {
                dirOutput.mkdirs()
            }

            if (!dirInput.exists() && dirOutput.exists()) {
                if (dirOutput.isDirectory) {
                    FileUtils.deletePath(dirOutput)
                } else {
                    FileUtils.delete(dirOutput)
                }
            }

            if (isIncremental) {
                val outChangedFiles = HashMap<File, Status>()

                for ((changedFileInput, status) in mapOfChangedFiles) {
                    val changedFileInputFullPath = changedFileInput.absolutePath

                    // mapOfChangedFiles is contains all. each collectDirectoryInputTask should handle itself, should not handle other file
                    if (!changedFileInputFullPath.contains(inputFullPath)) {
                        continue
                    }

                    val changedFileOutput =
                        File(changedFileInputFullPath.replace(inputFullPath, outputFullPath))

                    if (status == Status.ADDED || status == Status.CHANGED) {
                        resultOfDirInputToOut[changedFileInput] = changedFileOutput
                    } else if (status == Status.REMOVED) {
                        changedFileOutput.delete()
                    }
                    outChangedFiles[changedFileOutput] = status
                }

                legacyReplaceChangedFile?.invoke(dirInput, outChangedFiles)
            } else {
                resultOfDirInputToOut[dirInput] = dirOutput
            }

            legacyReplaceFile?.invoke(dirInput, dirOutput)

        }


    }

    class CollectJarInputTask(
        private val inputJar: File,
        private val inputJarStatus: Status,
        private val inputToOutput: Map<File, File>,
        private val isIncremental: Boolean,
        private val traceClassFileOutput: File,
        private val legacyReplaceFile: ((File, File) -> Any)?,             // Will be removed in the future
        private val uniqueOutputName: Boolean,
        private val resultOfDirInputToOut: MutableMap<File, File>,
        private val resultOfJarInputToOut: MutableMap<File, File>
    ) : Runnable {

        override fun run() {
            try {
                handle()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "%s", e.toString())
            }
        }

        private fun handle() {

            val jarInput = inputJar
            val jarOutput = if (inputToOutput.containsKey(jarInput)) {
                inputToOutput[jarInput]!!
            } else {
                val outputJarName = if (uniqueOutputName)
                    getUniqueJarName(jarInput)
                else
                    appendSuffix(jarInput, "traced")
                File(traceClassFileOutput, outputJarName)
            }

            Log.d(TAG, "CollectJarInputTask input %s -> output %s", jarInput, jarOutput)

            if (!isIncremental && jarOutput.exists()) {
                jarOutput.delete()
            }
            if (!jarOutput.parentFile.exists()) {
                jarOutput.parentFile.mkdirs()
            }

            if (IOUtil.isRealZipOrJar(jarInput)) {
                if (isIncremental) {
                    if (inputJarStatus == Status.ADDED || inputJarStatus == Status.CHANGED) {
                        resultOfJarInputToOut[jarInput] = jarOutput
                    } else if (inputJarStatus == Status.REMOVED) {
                        jarOutput.delete()
                    }

                } else {
                    resultOfJarInputToOut[jarInput] = jarOutput
                }

            } else {
                // Special case for WeChat AutoDex. Its rootInput jar file is actually
                // a txt file contains path list.
                jarInput.inputStream().bufferedReader().useLines { lines ->
                    lines.forEach { realJarInputFullPath ->
                        val realJarInput = File(realJarInputFullPath)
                        // dest jar, moved to extra guard intermediate output dir.
                        val realJarOutput =
                            File(traceClassFileOutput, getUniqueJarName(realJarInput))

                        if (realJarInput.exists() && IOUtil.isRealZipOrJar(realJarInput)) {
                            resultOfJarInputToOut[realJarInput] = realJarOutput
                        } else {
                            realJarOutput.delete()
                            if (realJarInput.exists() && realJarInput.isDirectory) {
                                val realJarOutputDir = File(traceClassFileOutput, realJarInput.name)
                                if (!realJarOutput.exists()) {
                                    realJarOutput.mkdirs()
                                }
                                resultOfDirInputToOut[realJarInput] = realJarOutputDir
                            }

                        }
                        // write real output full path to the fake jar at rootOutput.
                        jarOutput.outputStream().bufferedWriter().use { bw ->
                            bw.write(realJarOutput.absolutePath)
                            bw.newLine()
                        }
                    }
                }

                jarInput.delete() // delete raw inputList
            }

            legacyReplaceFile?.invoke(jarInput, jarOutput)
        }
    }


    companion object {
        private const val TAG = "Swan.Trace"

        @Suppress("DEPRECATION")
        fun getUniqueJarName(jarFile: File): String {
            val origJarName = jarFile.name
            val hashing = Hashing.sha1().hashString(jarFile.path, Charsets.UTF_16LE).toString()
            val dotPos = origJarName.lastIndexOf('.')
            return if (dotPos < 0) {
                String.format("%s_%s", origJarName, hashing)
            } else {
                val nameWithoutDotExt = origJarName.substring(0, dotPos)
                val dotExt = origJarName.substring(dotPos)
                String.format("%s_%s%s", nameWithoutDotExt, hashing, dotExt)
            }
        }

        fun appendSuffix(jarFile: File, suffix: String): String {
            val origJarName = jarFile.name
            val dotPos = origJarName.lastIndexOf('.')
            return if (dotPos < 0) {
                String.format("%s_%s", origJarName, suffix)
            } else {
                val nameWithoutDotExt = origJarName.substring(0, dotPos)
                val dotExt = origJarName.substring(dotPos)
                String.format("%s_%s%s", nameWithoutDotExt, suffix, dotExt)
            }
        }
    }
}