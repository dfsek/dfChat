import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

abstract class ComposeFixingClassVisitorFactory :
    AsmClassVisitorFactory<ComposeFixingClassVisitorFactory.ComposeFixingParams> {
    interface ComposeFixingParams : InstrumentationParameters {
        @get:Input
        @get:Optional
        val invalidate: Property<Long>
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        println("Instrumenting class ${classContext.currentClassData.className}")

        return object : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {
            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                val s = super.visitMethod(access, name, descriptor, signature, exceptions)
                if(name == "invokeSuspend") {
                    println("MODIFYING METHOD: $name")
                    return object : MethodVisitor(Opcodes.ASM9, s) {
                        override fun visitInsn(opcode: Int) {
                            if (opcode == Opcodes.FCONST_0) {
                                println("REPLACING FCONST_0 WITH 1")
                                s.visitInsn(Opcodes.FCONST_1)
                            } else if (opcode == Opcodes.FCONST_1) {
                                println("REPLACING FCONST_1 WITH 0")
                                s.visitInsn(Opcodes.FCONST_0)
                            } else {
                                s.visitInsn(opcode)
                            }
                        }
                    }
                }

                else return s
            }
        }
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.className =="androidx.compose.foundation.text.TextFieldCursorKt\$cursor\$1\$1"
    }
}