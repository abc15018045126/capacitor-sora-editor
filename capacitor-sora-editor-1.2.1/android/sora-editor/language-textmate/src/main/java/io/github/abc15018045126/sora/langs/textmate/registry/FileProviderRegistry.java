
package io.github.abc15018045126.sora.langs.textmate.registry;

import org.eclipse.jdt.annotation.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.github.abc15018045126.sora.langs.textmate.registry.provider.FileResolver;

public class FileProviderRegistry {


    private final List<FileResolver> allFileResolvers = new ArrayList<>();

    private static FileProviderRegistry fileProviderRegistry;

    private FileProviderRegistry() {
        allFileResolvers.add(FileResolver.DEFAULT);
    }

    public static synchronized FileProviderRegistry getInstance() {
        if (fileProviderRegistry == null)
            fileProviderRegistry = new FileProviderRegistry();
        return fileProviderRegistry;
    }

    public synchronized void addFileProvider(FileResolver fileResolver) {
        if (fileResolver != FileResolver.DEFAULT) {
            allFileResolvers.add(fileResolver);
        }
    }

    public synchronized void removeFileProvider(FileResolver fileResolver) {
        if (fileResolver != FileResolver.DEFAULT) {
            allFileResolvers.remove(fileResolver);
        }
    }

    @Nullable
    public InputStream tryGetInputStream(String path) {
        for (var provider : allFileResolvers) {
            var stream = provider.resolveStreamByPath(path);
            if (stream != null) {
                return stream;
            }
        }
        return null;
    }

    public void dispose() {
        for (var provider : allFileResolvers) {
            provider.dispose();
        }

        allFileResolvers.clear();
    }

}

