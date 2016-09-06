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

package com.hazelcast.jet.cascading.runtime;

import cascading.CascadingException;
import cascading.flow.FlowProcess;
import cascading.flow.SliceCounters;
import cascading.flow.StepCounters;
import cascading.flow.stream.duct.DuctException;
import cascading.flow.stream.element.SourceStage;
import cascading.tap.Tap;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleEntryIterator;
import com.hazelcast.jet.io.Pair;

import java.util.Iterator;

public class JetSourceStage extends SourceStage implements ProcessorInputSource {

    public JetSourceStage(FlowProcess flowProcess, Tap tap) {
        super(flowProcess, tap);
    }

    public void beforeProcessing() {
        next.start(this);
    }

    @Override
    public void process(Iterator<Pair<Tuple, Tuple>> input, Integer ordinal) throws Throwable {
        // TODO: this should not create new objects at every run()
        TupleEntryIterator iterator = getSource().openForRead(flowProcess, input);
        while (iterator.hasNext()) {
            TupleEntry tupleEntry;
            try {
                tupleEntry = iterator.next();
                flowProcess.increment(StepCounters.Tuples_Read, 1);
                flowProcess.increment(SliceCounters.Tuples_Read, 1);
            } catch (OutOfMemoryError error) {
                handleReThrowableException("out of memory, try increasing task memory allocation", error);
                continue;
            } catch (CascadingException exception) {
                handleException(exception, null);
                continue;
            } catch (Throwable throwable) {
                handleException(new DuctException("internal error", throwable), null);
                continue;
            }
            next.receive(this, tupleEntry);
        }

    }

    @Override
    public void finalizeProcessor() {
        complete(this);
    }
}
