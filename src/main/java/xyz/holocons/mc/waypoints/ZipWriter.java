package xyz.holocons.mc.waypoints;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipWriter extends ZipOutputStream {

    public ZipWriter(final File file) throws FileNotFoundException {
        super(new BufferedOutputStream(new FileOutputStream(file)));
    }

    public void addFile(final File... files) throws IOException {
        for (final var file : files) {
            if (!file.exists()) {
                continue;
            }

            final var inputStream = new BufferedInputStream(new FileInputStream(file));
            putNextEntry(new ZipEntry(file.getName()));
            inputStream.transferTo(this);
            closeEntry();
            inputStream.close();
        }
    }
}
