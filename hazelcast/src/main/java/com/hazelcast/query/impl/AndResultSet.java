/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.query.impl;

import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.query.Predicate;

import java.io.IOException;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.hazelcast.util.Preconditions.isNotNull;

/**
 * And Result set for Predicates.
 */
public class AndResultSet extends AbstractSet<QueryableEntry> {
    private final Set<QueryableEntry> setSmallest;
    private final List<Set<QueryableEntry>> otherIndexedResults;
    private final List<Predicate> lsNoIndexPredicates;
    private int size;

    public AndResultSet(Set<QueryableEntry> setSmallest, List<Set<QueryableEntry>> otherIndexedResults,
                        List<Predicate> lsNoIndexPredicates) {
        this.setSmallest = isNotNull(setSmallest, "setSmallest");
        this.otherIndexedResults = otherIndexedResults;
        this.lsNoIndexPredicates = lsNoIndexPredicates;
        this.size = -1;
    }

    public byte[] toByteArray(ObjectDataOutput out) throws IOException {
        for (QueryableEntry entry : this) {
            out.writeData(entry.getKeyData());
        }
        return out.toByteArray();
    }

    @Override
    public boolean contains(Object o) {
        if (!setSmallest.contains(o)) {
            return false;
        }

        if (otherIndexedResults != null) {
            for (Set<QueryableEntry> otherIndexedResult : otherIndexedResults) {
                if (!otherIndexedResult.contains(o)) {
                    return false;
                }
            }
        }
        if (lsNoIndexPredicates != null) {
            for (Predicate noIndexPredicate : lsNoIndexPredicates) {
                if (!noIndexPredicate.apply((Map.Entry) o)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Iterator<QueryableEntry> iterator() {
        return new It();
    }

    class It implements Iterator<QueryableEntry> {

        QueryableEntry currentEntry;
        final Iterator<QueryableEntry> it = setSmallest.iterator();

        @Override
        public boolean hasNext() {
            if (currentEntry != null) {
                return true;
            }

            for (; it.hasNext(); ) {
                QueryableEntry entry = it.next();

                if (checkOtherIndexedResults(entry) && checkNoIndexPredicates(entry)) {
                    currentEntry = entry;
                    return true;
                }
            }

            return false;
        }

        private boolean checkNoIndexPredicates(QueryableEntry currentEntry) {
            if (lsNoIndexPredicates == null) {
                return true;
            }

            for (Predicate noIndexPredicate : lsNoIndexPredicates) {
                if (!noIndexPredicate.apply(currentEntry)) {
                    return false;
                }
            }

            return true;
        }

        private boolean checkOtherIndexedResults(QueryableEntry currentEntry) {
            if (otherIndexedResults == null) {
                return true;
            }

            for (Set<QueryableEntry> otherIndexedResult : otherIndexedResults) {
                if (!otherIndexedResult.contains(currentEntry)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public QueryableEntry next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            QueryableEntry result = currentEntry;
            currentEntry = null;
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int size() {
        if (size == -1) {
            int calculatedSize = 0;
            for (Iterator<QueryableEntry> it = iterator(); it.hasNext(); it.next()) {
                calculatedSize++;
            }
            size = calculatedSize;
        }
        return size;
    }

}
