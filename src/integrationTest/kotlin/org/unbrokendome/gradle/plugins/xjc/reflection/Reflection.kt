package org.unbrokendome.gradle.plugins.xjc.reflection

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


data class ClassInfo(
    val name: String,
    val methods: List<MethodInfo>
)


data class MethodInfo(
    val name: String,
    val descriptor: String
)


fun ClassLoader.loadClassInfo(name: String): ClassInfo {

    lateinit var className: String
    val methods = mutableListOf<MethodInfo>()

    getResourceAsStream("${name.replace('.', '/')}.class").use { input ->

        ClassReader(input).accept(object : ClassVisitor(Opcodes.ASM9) {

            override fun visit(
                version: Int, access: Int, name: String, signature: String?,
                superName: String?, interfaces: Array<out String>?
            ) {
                className = name
            }

            override fun visitMethod(
                access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?
            ): MethodVisitor? {
                val methodInfo = MethodInfo(name, descriptor)
                methods.add(methodInfo)
                return null
            }
        }, 0)

    }

    return ClassInfo(className, methods)
}
