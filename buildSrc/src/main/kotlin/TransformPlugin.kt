import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.internal.utils.setDisallowChanges
import org.gradle.api.Plugin
import org.gradle.api.Project

// We target and transform androidx/compose/foundation/text/TextFieldCursorKt$cursorAnimationSpec$1

class TransformPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.withPlugin("com.android.application") {
            val androidComponentsExtension =
                target.extensions.getByType(AndroidComponentsExtension::class.java)
            androidComponentsExtension.onVariants { variant ->
                variant.instrumentation.transformClassesWith(
                    ComposeFixingClassVisitorFactory::class.java,
                    InstrumentationScope.ALL
                ) { params ->
                    params.invalidate.setDisallowChanges(System.currentTimeMillis())
                }
            }
        }
    }
}