package com.bomber.swan.resource.matrix.analyzer

/**
 * @author youngtr
 * @data 2022/4/26
 */
sealed class Analysis(val result: String)

class AnalysisSuccess(result: String) : Analysis(result)
class AnalysisFailure(result: String) : Analysis(result)