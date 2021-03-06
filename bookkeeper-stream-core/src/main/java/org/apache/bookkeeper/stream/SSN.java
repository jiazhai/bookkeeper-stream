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
package org.apache.bookkeeper.stream;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.apache.bookkeeper.stream.exceptions.InvalidSSNException;
import org.apache.bookkeeper.stream.proto.DataFormats;
import org.apache.commons.codec.binary.Base64;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Stream Sequence Number (SSN): A unique id for a record in the stream.
 */
public final class SSN implements Comparable<SSN> {

    public static final byte VERSION1 = (byte) 1;
    private static final byte CUR_VERSION = VERSION1;
    private static final int LENGTH = (Long.SIZE * 3 / Byte.SIZE) + 1;

    public static final SSN INVALID_SSN = of(-1L, -1L, -1L);

    public static SSN of(long segmentId, long entryId, long slotId) {
        return new SSN(segmentId, entryId, slotId);
    }

    public static SSN of(DataFormats.SSN dfSSN) {
        return new SSN(dfSSN.getSegmentId(), dfSSN.getEntryId(), dfSSN.getSlotId());
    }

    private final long segmentId;
    private final long entryId;
    private final long slotId;

    public SSN(long segmentId, long entryId, long slotId) {
        this.segmentId = segmentId;
        this.entryId = entryId;
        this.slotId = slotId;
    }

    @Override
    public int compareTo(SSN that) {
        if (this.segmentId != that.segmentId) {
            return (this.segmentId < that.segmentId) ? -1 : 1;
        } else if (this.entryId != that.entryId) {
            return (this.entryId < that.entryId) ? -1 : 1;
        } else {
            return (this.slotId < that.slotId) ? -1 : ((this.slotId == that.slotId) ? 0 : 1);
        }
    }

    public boolean notGreaterThan(SSN that) {
        if (this.segmentId < that.segmentId) {
            return true;
        } else if (this.segmentId > that.segmentId) {
            return false;
        } else {
            if (this.entryId < that.entryId) {
                return true;
            } else if (this.entryId > that.entryId) {
                return false;
            } else {
                return this.slotId <= that.slotId;
            }
        }
    }

    public DataFormats.SSN toProtoSSN() {
        DataFormats.SSN.Builder builder = DataFormats.SSN.newBuilder();
        builder.setSegmentId(segmentId).setEntryId(entryId).setSlotId(slotId);
        return builder.build();
    }

    /**
     * Serialize the SSN into a string as current version format.
     *
     * @return serialized ssn
     */
    public String serialize() {
        return serialize(CUR_VERSION);
    }

    /**
     * Serialize the SSN into a string using given <i>version</i>.
     *
     * @param version version used to serialize SSN
     * @return serialized ssn
     */
    public String serialize(byte version) {
        Preconditions.checkArgument(version == CUR_VERSION,
                "Only support version less than or equal to " + CUR_VERSION);
        byte[] data = new byte[LENGTH];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(version);
        bb.putLong(segmentId);
        bb.putLong(entryId);
        bb.putLong(slotId);
        return Base64.encodeBase64String(data);
    }

    /**
     * Deserialize SSN from the string <i>data</i>.
     *
     * @param data ssn string representation
     * @return ssn instance
     */
    public static SSN deserialize(String data) throws InvalidSSNException {
        byte[] dataBytes = Base64.decodeBase64(data);
        ByteBuffer bb = ByteBuffer.wrap(dataBytes);
        byte version = bb.get();
        if (CUR_VERSION != version) {
            throw new InvalidSSNException(data, "Only support version not greater than " + CUR_VERSION
                    + ", but found version " + version);
        }
        try {
            return new SSN(bb.getLong(), bb.getLong(), bb.getLong());
        } catch (BufferUnderflowException bufe) {
            throw new InvalidSSNException(data, bufe);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(segmentId, entryId, slotId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SSN)) {
            return false;
        }
        SSN that = (SSN) o;
        return this.segmentId == that.segmentId &&
                this.entryId == that.entryId &&
                this.slotId == that.slotId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SSN(sid=").append(segmentId)
          .append(", eid=").append(entryId)
          .append(", tid=").append(slotId).append(")");
        return sb.toString();
    }

    public static boolean notGreaterThan(DataFormats.SSN ssn1, DataFormats.SSN ssn2) {
        if (ssn1.getSegmentId() < ssn2.getSegmentId()) {
            return true;
        } else if (ssn1.getSegmentId() > ssn2.getSegmentId()) {
            return false;
        } else {
            if (ssn1.getEntryId() < ssn2.getEntryId()) {
                return true;
            } else if (ssn1.getEntryId() > ssn2.getEntryId()) {
                return false;
            } else {
                return ssn1.getSlotId() <= ssn2.getSlotId();
            }
        }
    }

}
