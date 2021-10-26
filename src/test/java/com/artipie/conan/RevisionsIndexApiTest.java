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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for RevisionsIndexApi class.
 * @since 0.1
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseVarargs"})
class RevisionsIndexApiTest {

    /**
     * ZLIB binary package dir. name (hash).
     */
    private static final String ZLIB_BIN_PKG = "6af9cc7cb931c5ad942174fd7838eb655717c709";

    /**
     * Path to zlib package binary index file.
     */
    private static final String ZLIB_BIN_INDEX =
        "zlib/1.2.11/_/_/0/package/6af9cc7cb931c5ad942174fd7838eb655717c709/revisions.txt";

    /**
     * Path to zlib package recipe index file.
     */
    private static final String ZLIB_SRC_INDEX = "zlib/1.2.11/_/_/revisions.txt";

    /**
     * Path prefix for conan repository test data.
     */
    private static final String DIR_PREFIX = "conan-test/data/";

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test instance.
     */
    private RevisionsIndexApi index;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.index = new RevisionsIndexApi(this.storage, new Key.From("zlib/1.2.11/_/_"));
    }

    @ParameterizedTest
    @MethodSource("indexingTestFilesList")
    void updateRecipeIndex(final String[] files) {
        for (final String file : files) {
            new TestResource(String.join("", RevisionsIndexApiTest.DIR_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final List<Integer> result = this.index.updateRecipeIndex().toCompletableFuture().join();
        final Key key = new Key.From(RevisionsIndexApiTest.ZLIB_SRC_INDEX);
        final JsonParser parser = Json.createParser(
            new StringReader(new String(new BlockingStorage(this.storage).value(key)))
        );
        parser.next();
        final JsonArray revs = parser.getObject().getJsonArray("revisions");
        final String time = RevisionsIndexApiTest.getJsonStr(revs.get(0), "time");
        final String revision = RevisionsIndexApiTest.getJsonStr(revs.get(0), "revision");
        MatcherAssert.assertThat(
            "The revision object fields have incorrect format",
            time.length() > 0 && revision.length() > 0 && result.size() == revs.size()
        );
        MatcherAssert.assertThat(
            "The revision field of revision object has incorrect value",
            result.get(0) == Integer.parseInt(revision)
        );
        MatcherAssert.assertThat(
            "The time field of the revision object has incorrect value",
            Instant.parse(time).getEpochSecond() > 0
        );
    }

    @ParameterizedTest
    @MethodSource("indexingTestFilesList")
    void updateBinaryIndex(final String[] files) {
        for (final String file : files) {
            new TestResource(String.join("", RevisionsIndexApiTest.DIR_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final List<Integer> result = this.index.updateBinaryIndex(
            0, RevisionsIndexApiTest.ZLIB_BIN_PKG
        ).toCompletableFuture().join();
        final Key key = new Key.From(RevisionsIndexApiTest.ZLIB_BIN_INDEX);
        final JsonParser parser = Json.createParser(
            new StringReader(new String(new BlockingStorage(this.storage).value(key)))
        );
        parser.next();
        final JsonArray revs = parser.getObject().getJsonArray("revisions");
        final String time = RevisionsIndexApiTest.getJsonStr(revs.get(0), "time");
        final String revision = RevisionsIndexApiTest.getJsonStr(revs.get(0), "revision");
        MatcherAssert.assertThat(
            "The revision object fields have incorrect format",
            time.length() > 0 && revision.length() > 0 && result.size() == revs.size()
        );
        MatcherAssert.assertThat(
            "The revision field of revision object has incorrect value",
            result.get(0) == Integer.parseInt(revision)
        );
        MatcherAssert.assertThat(
            "The time field of the revision object has incorrect value",
            Instant.parse(time).getEpochSecond() > 0
        );
    }

    private static String getJsonStr(final JsonValue object, final String key) {
        return object.asJsonObject().get(key).toString().replaceAll("\"", "");
    }

    /**
     * Returns test package files list for indexing tests (without revisions.txt files).
     * @return List of files, as Stream of junit Arguments.
     * @checkstyle LineLengthCheck (20 lines)
     */
    @SuppressWarnings({"PMD.UnusedPrivateMethod", "PMD.LineLengthCheck"})
    private static Stream<Arguments> indexingTestFilesList() {
        final String[] files = new String[]{
            "zlib/1.2.11/_/_/0/package/6af9cc7cb931c5ad942174fd7838eb655717c709/0/conanmanifest.txt",
            "zlib/1.2.11/_/_/0/package/6af9cc7cb931c5ad942174fd7838eb655717c709/0/conaninfo.txt",
            "zlib/1.2.11/_/_/0/package/6af9cc7cb931c5ad942174fd7838eb655717c709/0/conan_package.tgz",
            "zlib/1.2.11/_/_/0/export/conan_sources.tgz",
            "zlib/1.2.11/_/_/0/export/conan_export.tgz",
            "zlib/1.2.11/_/_/0/export/conanfile.py",
            "zlib/1.2.11/_/_/0/export/conanmanifest.txt",
        };
        return Stream.of(Arguments.of((Object) files));
    }
}
