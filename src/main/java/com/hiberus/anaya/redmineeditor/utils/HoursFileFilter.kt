package com.hiberus.anaya.redmineeditor.utils

import java.io.FilenameFilter
import java.io.File

class HoursFileFilter : FilenameFilter {
    override fun accept(dir: File, name: String): Boolean {
        return name.endsWith(".hours")
    }
}