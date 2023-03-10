package org.unbrokendome.gradle.plugins.xjc.work.xjc21

import com.sun.tools.xjc.Options
import org.unbrokendome.gradle.plugins.xjc.work.common.*
import javax.inject.Inject


@Suppress("unused") // instantiated dynamically
abstract class XjcGeneratorLegacyWorkAction
@Inject constructor(): AbstractXjcGeneratorWorkAction() {

    override fun getContextClassLoaderHolder(): IContextClassLoaderHolder = ContextClassLoaderHolderV21()

    override fun getOptionsAccessor(options: Options): IOptionsAccessor = OptionsAccessorV21(options)

}
