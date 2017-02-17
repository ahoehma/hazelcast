/*
 * Copyright (c) 2008, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.aggregation.impl;

import com.hazelcast.aggregation.Aggregator;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class BigIntegerAverageAggregator<I> extends AbstractAggregator<I, BigInteger, BigDecimal>
        implements IdentifiedDataSerializable {

    private BigInteger sum = BigInteger.ZERO;
    private long count;

    public BigIntegerAverageAggregator() {
        super();
    }

    public BigIntegerAverageAggregator(String attributePath) {
        super(attributePath);
    }

    @Override
    public void accumulateExtracted(BigInteger value) {
        count++;
        sum = sum.add(value);
    }

    @Override
    public void combine(Aggregator aggregator) {
        BigIntegerAverageAggregator typedAggregator = (BigIntegerAverageAggregator) aggregator;
        this.sum = this.sum.add(typedAggregator.sum);
        this.count += typedAggregator.count;
    }

    @Override
    public BigDecimal aggregate() {
        if (count == 0) {
            return null;
        }
        return new BigDecimal(sum)
                .divide(BigDecimal.valueOf(count));
    }

    @Override
    public int getFactoryId() {
        return AggregatorDataSerializerHook.F_ID;
    }

    @Override
    public int getId() {
        return AggregatorDataSerializerHook.BIG_INT_AVG;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(attributePath);
        out.writeObject(sum);
        out.writeLong(count);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        this.attributePath = in.readUTF();
        this.sum = in.readObject();
        this.count = in.readLong();
    }

}
