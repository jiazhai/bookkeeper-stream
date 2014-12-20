/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bookkeeper.stream.io;

import org.apache.bookkeeper.stream.SSN;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Reader to read records from an input stream.
 */
public class RecordReader {

    /**
     * SSN Stream will not generate ssn.
     */
    private static SSNStream NONE_SSN_STREAM = new SSNStream() {
        @Override
        public SSN getCurrentSSN() {
            return SSN.INVALID_SSN;
        }

        @Override
        public void advance() {
            // no-op
        }
    };

    public static RecordReader of(DataInputStream in) {
        return new RecordReader(NONE_SSN_STREAM, in);
    }

    public static RecordReader of(SSNStream ssnStream, DataInputStream in) {
        return new RecordReader(ssnStream, in);
    }

    private final SSNStream ssnStream;
    private final DataInputStream in;

    /**
     * Reader to read records from input stream <i>in</i>. Each record
     * will be assigned SSN that generated by <i>ssnStream</i>.
     *
     * @param ssnStream
     *          SSN Stream to generate ssn for records.
     * @param in
     *          input stream for records
     */
    protected RecordReader(SSNStream ssnStream, DataInputStream in) {
        this.ssnStream = ssnStream;
        this.in = in;
    }

    /**
     * Read a record from the stream.
     * it would return null.
     *
     * @return record.
     * @throws IOException
     */
    public Record readRecord() throws IOException {
        try {
            return readRecord0();
        } catch (EOFException eof) {
            // reach end of the stream
        }
        return null;
    }

    private Record readRecord0() throws IOException {
        Record.Builder rBuilder = Record.read(in);
        rBuilder.setSSN(ssnStream.getCurrentSSN());
        // move ssn stream to next record
        ssnStream.advance();
        return rBuilder.build();
    }

    /**
     * Skip to given <i>ssn</i>.
     *
     * @param ssn
     * @return true if successfully skip to given ssn
     * @throws IOException
     */
    public boolean skipTo(SSN ssn) throws IOException {
        boolean found = false;
        while (true) {
            if (ssnStream.getCurrentSSN().compareTo(ssn) >= 0) {
                found = true;
                break;
            }
            try {
                // skip record id
                in.readLong();
                int len = in.readInt();
                // skin data length
                while (len > 0) {
                    int nBytes = in.skipBytes(len);
                    len -= nBytes;
                }
            } catch (EOFException eof) {
                break;
            }
            ssnStream.advance();
        }
        return found;
    }
}
