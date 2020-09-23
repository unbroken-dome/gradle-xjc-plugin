package org.unbrokendome.gradle.plugins.xjc.work.common

import java.util.Locale


fun withDefaultLocale(locale: Locale?, action: () -> Unit) {
    if (locale != null && locale != Locale.getDefault()) {
        val oldLocale = Locale.getDefault()
        Locale.setDefault(locale)

        try {
            action()

        } finally {
            Locale.setDefault(oldLocale)
        }

    } else {
        action()
    }
}

