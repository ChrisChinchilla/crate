/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.executor.transport;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import io.crate.Streamer;
import io.crate.core.collections.Bucket;
import io.crate.core.collections.Row;
import io.crate.core.collections.RowN;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;

public class StreamBucket implements Bucket, Streamable {

    private Streamer<?>[] streamers;
    private int size = -1;
    private BytesReference bytes;

    public static class Builder {
        private int size = 0;
        private final Streamer<?>[] streamers;
        private final BytesStreamOutput out;

        public static final Function<Builder, Bucket> BUILD_FUNCTION =
                new Function<Builder, Bucket>() {
                    @Nullable
                    @Override
                    public Bucket apply(StreamBucket.Builder input) {
                        try {
                            return input.build();
                        } catch (IOException e) {
                            Throwables.propagate(e);
                        }
                        return null;
                    }
                };

        public Builder(Streamer<?>[] streamers) {
            this.streamers = streamers;
            this.out = new BytesStreamOutput();
        }

        public void add(Row row) throws IOException {
            size++;
            for (int i = 0; i < streamers.length; i++) {
                streamers[i].writeValueTo(out, row.get(i));
            }
        }

        public StreamBucket build() throws IOException {
            StreamBucket sb = new StreamBucket(streamers);
            sb.size = size;
            sb.bytes = out.bytes();
            return sb;
        }
    }

    public StreamBucket(StreamInput in) throws IOException {
        readFrom(in);
    }

    public StreamBucket(Streamer<?>[] streamers) {
        this.streamers = streamers;
    }

    public StreamBucket(Streamer<?>[] streamers, BytesReference bytes) {
        this(streamers);
        this.bytes = bytes;
    }

    @Override
    public int size() {
        return size;
    }

    public static void writeBucket(StreamOutput out, Streamer<?>[] streamers, Bucket bucket) throws IOException {
        StreamWriter writer = new StreamWriter(out, streamers, bucket.size());
        writer.addAll(bucket);
    }

    private class RowIterator implements Iterator<Row> {

        private final StreamInput input = bytes.streamInput();
        private int pos = 0;
        private final Object[] current = new Object[streamers.length];
        private final Row row = new RowN(current);

        @Override
        public boolean hasNext() {
            return pos < size;
        }

        @Override
        public Row next() {
            for (int c = 0; c < streamers.length; c++) {
                try {
                    current[c] = streamers[c].readValueFrom(input);
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            }
            pos++;
            return row;
        }

        @Override
        public void remove() {

        }
    }

    @Override
    public Iterator<Row> iterator() {
        return new RowIterator();
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        size = in.readVInt();
        BytesStreamOutput memoryStream = new BytesStreamOutput();
        Streams.copy(in, memoryStream);
        bytes = memoryStream.bytes();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        assert size > -1;
        out.writeVInt(size);
        Streams.copy(bytes.streamInput(), out);
    }

    public static class StreamWriter {
        private final StreamOutput out;
        private final Streamer<?>[] streamers;
        private final int size;

        public StreamWriter(StreamOutput out, Streamer<?>[] streamers, int size) {
            this.out = out;
            this.streamers = streamers;
            this.size = size;
            try {
                out.writeVInt(this.size);
            } catch (IOException e) {
                Throwables.propagate(e);
            }

        }

        public void add(Row row) throws IOException {
            for (int i = 0; i < streamers.length; i++) {
                streamers[i].writeValueTo(out, row.get(i));
            }
        }

        public void addAll(Iterable<Row> rows) throws IOException {
            for (Row row : rows) {
                add(row);
            }
        }

    }

}
