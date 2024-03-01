package io.github.jyc228.kotlin.codegen

class CallBuilder(val callName: String, val indent: Indent) {
    var generic = ""
    val params = mutableListOf<String>()

    fun generic(init: GenericBuilder.() -> Unit) = apply { generic = GenericBuilder().apply(init).build() }
    fun stringParameter(value: String) = apply { params += "\"$value\"" }
    fun stringTemplateParameter(value: String) = apply { params += "\"\"\"$value\"\"\"" }
    fun parameter(value: Any) = apply { params += value.toString() }

    fun build(): String = "$callName$generic(${buildParameter()})"

    private fun buildParameter(): String {
        if (callName.length + generic.length + params.sumOf { it.length } >= 80 || params.size >= 3) {
            return params.joinToString(prefix = "\n", separator = ",\n", postfix = "\n$indent") { "${indent.next}$it" }
        }
        return params.joinToString(",")
    }
}
