
package io.github.abc15018045126.sora.util;

import android.content.ClipData;
import android.content.Intent;

import androidx.annotation.Nullable;

public class ClipDataUtils {

    public static String clipDataToString(@Nullable ClipData clipData) {
        if (clipData == null) {
            return "";
        }
        var sb = new StringBuilder();
        for (int i = 0; i < clipData.getItemCount(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            var item = clipData.getItemAt(i);
            if (item.getText() != null) {
                sb.append(item.getText());
            } else if (item.getUri() != null) {
                sb.append(item.getUri().toString());
            } else if (item.getIntent() != null) {
                sb.append(item.getIntent().toUri(Intent.URI_INTENT_SCHEME));
            }
        }
        return sb.toString();
    }

}

