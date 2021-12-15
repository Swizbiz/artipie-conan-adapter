/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.conan;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for IniFile class.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class IniFileTest {

    @Test
    void testGetValueTypes() throws IOException, URISyntaxException {
        final URL url = this.getClass().getResource("/conan-test/conaninfo.txt");
        final IniFile ini = IniFile.loadIniFile(Paths.get(url.toURI()));
        final int gccver = 9;
        MatcherAssert.assertThat(
            "Invalid compiler.version value",
            ini.getValue("full_settings", "compiler.version", 0).equals(gccver)
        );
        MatcherAssert.assertThat(
            "Invalid os field value",
            ini.getValue("full_settings", "os", "").equals("Linux")
        );
        MatcherAssert.assertThat(
            "Invalid fPIC field value",
            ini.getValue("options", "fPIC", false).equals(true)
        );
        MatcherAssert.assertThat(
            "Invalid \"shared\" field value",
            ini.getValue("options", "shared", true).equals(false)
        );
        MatcherAssert.assertThat(
            "Invalid field in recipe_hash hash value",
            ini.getValue("recipe_hash", "cb005523f87beefc615e1ff49724883e", "").equals("")
        );
        MatcherAssert.assertThat(
            "Invalid \"shared\" field value",
            ini.getValue("options", "shared", true).equals(false)
        );
    }

    @Test
    @SuppressWarnings("PMD.ProhibitFilesCreateFileInTests")
    void testEmptyIni(@TempDir final Path tmp) throws IOException {
        final Path tmpfile = tmp.resolve("test.ini");
        Files.createFile(tmpfile);
        final IniFile ini = IniFile.loadIniFile(tmpfile);
        MatcherAssert.assertThat(
            "No entries, but no exceptions thrown", ini.getEntries().isEmpty()
        );
    }

    @Test
    @SuppressWarnings("PMD.ProhibitFilesCreateFileInTests")
    void testEmptyIniReloading(@TempDir final Path tmp) throws IOException {
        final Path tmpfile = tmp.resolve("test.ini");
        Files.createFile(tmpfile);
        final IniFile ini = IniFile.loadIniFile(tmpfile);
        MatcherAssert.assertThat(
            "No entries, but no exceptions thrown", ini.getEntries().isEmpty()
        );
        final Path tmpsave = tmp.resolve("test.ini");
        ini.save(tmpsave.toAbsolutePath().toString());
        final IniFile tmpini = IniFile.loadIniFile(tmpsave);
        MatcherAssert.assertThat(
            "Saved copy must be identical", ini.equals(tmpini)
        );
    }

    @Test
    void testIniFileLoading() throws IOException {
        final URL url = this.getClass().getResource("/conan-test/conaninfo.txt");
        final IniFile ini = IniFile.loadIniFile(Paths.get(url.getPath()));
        final int conanentries = 8;
        MatcherAssert.assertThat(
            "8 sections expected.", ini.getEntries().size() == conanentries
        );
        for (final String section: ini.getEntries().keySet()) {
            final Map<String, String> kvals = ini.getEntries().get(section);
            MatcherAssert.assertThat(
                "Section must not be empty", !"".equals(section)
            );
            for (final String key: kvals.keySet()) {
                MatcherAssert.assertThat(
                    "Key must not be empty", !"".equals(key)
                );
            }
        }
    }

    @Test
    void testIniReloading(@TempDir final Path tmp) throws IOException {
        final URL url = this.getClass().getResource("/conan-test/conaninfo.txt");
        final IniFile ini = IniFile.loadIniFile(Paths.get(url.getPath()));
        final Path tmpfile = tmp.resolve("test.ini");
        ini.save(tmpfile.toAbsolutePath().toString());
        final IniFile tmpini = IniFile.loadIniFile(tmpfile);
        MatcherAssert.assertThat(
            "Saved copy must be identical", ini.equals(tmpini)
        );
    }

    @Test
    void testKeysWithoutValues() throws IOException {
        final URL url = this.getClass().getResource("/conan-test/conanfile.txt");
        final IniFile ini = IniFile.loadIniFile(Paths.get(url.getPath()));
        MatcherAssert.assertThat(
            "Two sections expected.", ini.getEntries().size() == 2
        );
        int entries = 0;
        for (final String section: ini.getEntries().keySet()) {
            final Map<String, String> kvals = ini.getEntries().get(section);
            MatcherAssert.assertThat(
                "Section must not be empty", !"".equals(section)
            );
            for (final String key: kvals.keySet()) {
                final String value = kvals.get(key);
                MatcherAssert.assertThat(
                    "Key must not be empty", !"".equals(key)
                );
                MatcherAssert.assertThat(
                    "Value must be empty", "".equals(value)
                );
                final String teststr = ini.getString(section, key, null);
                MatcherAssert.assertThat(
                    "Value must be empty", "".equals(teststr)
                );
                final String testval = ini.getValue(section, key, "<not found>");
                MatcherAssert.assertThat(
                    "Value must be empty", "".equals(testval)
                );
                ++entries;
            }
        }
        final int conanentries = 8;
        MatcherAssert.assertThat(
            "8 entries expected.", entries == conanentries
        );
    }

    @Test
    void testKeysWithoutValuesReloading(@TempDir final Path tmp) throws IOException {
        final URL url = this.getClass().getResource("/conan-test/conanfile.txt");
        final IniFile ini = IniFile.loadIniFile(Paths.get(url.getPath()));
        final Path tmpfile = tmp.resolve("test.ini");
        ini.save(tmpfile.toAbsolutePath().toString());
        final IniFile tmpini = IniFile.loadIniFile(tmpfile);
        MatcherAssert.assertThat(
            "Saved copy must be identical", ini.equals(tmpini)
        );
    }
}
