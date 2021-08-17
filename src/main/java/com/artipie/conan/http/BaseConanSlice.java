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
package com.artipie.conan.http;

import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import org.reactivestreams.Publisher;

/**
 * Base slice class for Conan REST APIs.
 * @since 0.1
 */
abstract class BaseConanSlice implements Slice {

    /**
     * Error message string for the client.
     */
    private static final String URI_S_NOT_FOUND = "URI %s not found. Handler: %s";

    /**
     * HTTP Content-type header name.
     */
    private static final String CONTENT_TYPE = "Content-Type";

    /**
     * Current Artipie storage instance.
     */
    private final Storage storage;

    /**
     * Request path wrapper object, corresponding to this Slice instance.
     */
    private final PathWrap pathwrap;

    /**
     * Ctor.
     * @param storage Current Artipie storage instance.
     * @param pathwrap Current path wrapper instance.
     */
    BaseConanSlice(final Storage storage, final PathWrap pathwrap) {
        this.storage = storage;
        this.pathwrap = pathwrap;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final String hostname = new RqHeaders.Single(headers, "Host").asString();
        final RequestLineFrom request = new RequestLineFrom(line);
        final Matcher matcher = this.pathwrap.getPattern().matcher(request.uri().getPath());
        final CompletableFuture<RequestResult> content;
        if (matcher.matches()) {
            content = this.getResult(request, hostname, matcher);
        } else {
            content = CompletableFuture.completedFuture(new RequestResult());
        }
        return new AsyncResponse(
            content.thenApply(
                data -> {
                    final Response result;
                    if (data.isEmpty()) {
                        result = new RsWithBody(
                            StandardRs.NOT_FOUND,
                            String.format(
                                BaseConanSlice.URI_S_NOT_FOUND, request.uri(), this.getClass()
                            ),
                            StandardCharsets.UTF_8
                        );
                    } else {
                        result = new RsWithHeaders(
                            new RsWithBody(StandardRs.OK, data.getData()),
                            BaseConanSlice.CONTENT_TYPE, data.getType()
                        );
                    }
                    return result;
                }
            )
        );
    }

    /**
     * Returns current Artipie storage instance.
     * @return Storage object instance.
     */
    protected Storage getStorage() {
        return this.storage;
    }

    /**
     * Processess the request and returns result data for this request.
     * @param request Artipie request line helper object instance.
     * @param hostname Current server host name string to construct and process URLs.
     * @param matcher Matched paattern matcher object for the current path wrapper.
     * @return Future object, providing request result data.
     */
    protected abstract CompletableFuture<RequestResult> getResult(
        RequestLineFrom request, String hostname, Matcher matcher
    );

    /**
     * HTTP Request result bytes + Content-type string.
     * @since 0.1
     */
    protected static final class RequestResult {

        /**
         * Request result data bytes.
         */
        private final byte[] data;

        /**
         * Request result Content-type.
         */
        private final String type;

        /**
         * Initializes object with data bytes array and Content-Type string.
         * @param data Request result data bytes.
         * @param type Request result Content-type.
         */
        public RequestResult(final byte[] data, final String type) {
            this.data = Arrays.copyOf(data, data.length);
            this.type = type;
        }

        /**
         * Initializes object with data string, and json content type.
         * @param data Result data as String.
         */
        public RequestResult(final String data) {
            this(data.getBytes(StandardCharsets.UTF_8), "application/json");
        }

        /**
         * Initializes object with empty string, and json content type.
         */
        public RequestResult() {
            this("");
        }

        /**
         * Returns response data bytes.
         * @return Respose data as array of bytes.
         */
        public byte[] getData() {
            return this.data;
        }

        /**
         * Returns response Content-type string.
         * @return Respose Content-type as String.
         */
        public String getType() {
            return this.type;
        }

        /**
         * Checks if data is empty.
         * @return True, if data is empty.
         */
        public boolean isEmpty() {
            return this.data.length == 0;
        }
    }
}
