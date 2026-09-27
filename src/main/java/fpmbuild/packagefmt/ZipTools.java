package fpmbuild.packagefmt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipTools {
    private ZipTools() {}

    public static void extractJar(Path jar, Path destination) throws IOException {
        Files.createDirectories(destination);
        try (JarFile jf = new JarFile(jar.toFile())) {
            var entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                Path out = destination.resolve(e.getName()).normalize();
                if (!out.startsWith(destination)) throw new IOException("Unsafe archive entry: " + e.getName());
                if (e.isDirectory()) { Files.createDirectories(out); continue; }
                Files.createDirectories(out.getParent());
                try (InputStream in = jf.getInputStream(e)) { Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING); }
            }
        }
    }

    public static void zipDirectory(Path source, Path output) throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());
        Set<String> names = new HashSet<>();
        try (OutputStream raw = Files.newOutputStream(output); ZipOutputStream zip = new ZipOutputStream(raw)) {
            if (!Files.exists(source)) return;
            try (var stream = Files.walk(source)) {
                for (Path p : stream.filter(Files::isRegularFile).sorted().toList()) {
                    String name = source.relativize(p).toString().replace('\\', '/');
                    if (!names.add(name)) continue;
                    ZipEntry e = new ZipEntry(name); e.setTime(0L); zip.putNextEntry(e); Files.copy(p, zip); zip.closeEntry();
                }
            }
        }
    }
}
