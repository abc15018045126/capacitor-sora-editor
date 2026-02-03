package io.github.abc15018045126.sora.lang.analysis

import android.os.Bundle
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.ContentReference


interface AnalyzeManager {


    var receiver: StyleReceiver?


    fun reset(content: ContentReference, extraArguments: Bundle)


    fun insert(start: CharPosition, end: CharPosition, insertedContent: CharSequence)


    fun delete(start: CharPosition, end: CharPosition, deletedContent: CharSequence)


    fun rerun()


    fun destroy()

}
