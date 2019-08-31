/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.execution.engine.window;

import io.crate.breaker.RamAccountingContext;
import io.crate.data.ArrayRow;
import io.crate.data.Input;
import io.crate.data.Row;
import io.crate.execution.engine.aggregation.AggregationFunction;
import io.crate.execution.engine.collect.CollectExpression;
import io.crate.expression.symbol.Function;
import io.crate.expression.symbol.Symbol;
import io.crate.metadata.FunctionInfo;
import io.crate.metadata.TransactionContext;
import org.elasticsearch.Version;
import org.elasticsearch.common.util.BigArrays;

import javax.annotation.Nullable;
import java.util.List;

import static io.crate.execution.engine.window.WindowFrameState.isShrinkingWindow;

public class AggregateToWindowFunctionAdapter implements WindowFunction {

    private final AggregationFunction aggregationFunction;
    private final RamAccountingContext ramAccountingContext;
    private final Version indexVersionCreated;
    private final BigArrays bigArrays;
    private Object accumulatedState;

    private int seenFrameLowerBound = -1;
    private int seenFrameUpperBound = -1;
    private Object resultForCurrentFrame;

    public AggregateToWindowFunctionAdapter(AggregationFunction aggregationFunction,
                                            Version indexVersionCreated,
                                            BigArrays bigArrays,
                                            RamAccountingContext ramAccountingContext) {
        this.aggregationFunction = aggregationFunction.optimizeForExecutionAsWindowFunction();
        this.ramAccountingContext = ramAccountingContext;
        this.indexVersionCreated = indexVersionCreated;
        this.bigArrays = bigArrays;
        this.accumulatedState = this.aggregationFunction.newState(ramAccountingContext, indexVersionCreated, bigArrays);
    }

    @Override
    public FunctionInfo info() {
        return aggregationFunction.info();
    }

    @Override
    public Symbol normalizeSymbol(Function function, @Nullable TransactionContext txnCtx) {
        return aggregationFunction.normalizeSymbol(function, txnCtx);
    }

    @Override
    public Object execute(int idxInPartition,
                          WindowFrameState frame,
                          List<? extends CollectExpression<Row, ?>> expressions,
                          Input... args) {
        if (idxInPartition == 0) {
            recomputeFunction(frame, expressions, args);
        } else if (isShrinkingWindow(frame, seenFrameLowerBound, seenFrameUpperBound)) {
            if (aggregationFunction.isRemovableCumulative()) {
                removeSeenRowsFromAccumulatedState(frame, expressions, args);
                resultForCurrentFrame = aggregationFunction.terminatePartial(ramAccountingContext, accumulatedState);
                seenFrameLowerBound = frame.lowerBound();
                seenFrameUpperBound = frame.upperBoundExclusive();
            } else {
                recomputeFunction(frame, expressions, args);
            }
        } else if (frame.upperBoundExclusive() > seenFrameUpperBound) {
            executeAggregateForFrame(frame, expressions, args);
        }
        return resultForCurrentFrame;
    }

    private void removeSeenRowsFromAccumulatedState(WindowFrameState frame,
                                                    List<? extends CollectExpression<Row, ?>> expressions,
                                                    Input[] args) {
        var row = new ArrayRow();
        for (int i = seenFrameLowerBound; i < frame.lowerBound(); i++) {
            Object[] cells = frame.getRowInPartitionAtIndexOrNull(i);
            assert cells != null : "No row at idx=" + i + " in current partition=" + frame;
            row.cells(cells);
            for (int j = 0, expressionsSize = expressions.size(); j < expressionsSize; j++) {
                expressions.get(j).setNextRow(row);
            }
            accumulatedState = aggregationFunction.removeFromAggregatedState(ramAccountingContext,
                                                                             accumulatedState,
                                                                             args);

        }
    }

    private void recomputeFunction(WindowFrameState frame,
                                   List<? extends CollectExpression<Row, ?>> expressions,
                                   Input[] args) {
        accumulatedState = aggregationFunction.newState(ramAccountingContext, indexVersionCreated, bigArrays);
        seenFrameUpperBound = -1;
        seenFrameLowerBound = -1;
        executeAggregateForFrame(frame, expressions, args);
    }

    private void executeAggregateForFrame(WindowFrameState frame,
                                          List<? extends CollectExpression<Row, ?>> expressions,
                                          Input... inputs) {
        /*
         * If the window is not shrinking (eg. UNBOUNDED PRECEDING - CURRENT_ROW) the successive frames will have
         * overlapping rows. In this case we want to accumulate the rows we haven't processed yet (the difference
         * between the rows in the current frame to the rows in the previous frame, if any).
         */
        int unseenRowsInCurrentFrameStart =
            !isShrinkingWindow(frame, seenFrameLowerBound, seenFrameUpperBound) && seenFrameUpperBound > 0
                ? seenFrameUpperBound
                : frame.lowerBound();
        var row = new ArrayRow();
        for (int i = unseenRowsInCurrentFrameStart; i < frame.upperBoundExclusive(); i++) {
            Object[] cells = frame.getRowInFrameAtIndexOrNull(i);
            assert cells != null : "No row at idx=" + i + " in current frame=" + frame;
            row.cells(cells);
            for (int j = 0, expressionsSize = expressions.size(); j < expressionsSize; j++) {
                expressions.get(j).setNextRow(row);
            }
            accumulatedState = aggregationFunction.iterate(ramAccountingContext, accumulatedState, inputs);
        }
        
        resultForCurrentFrame = aggregationFunction.terminatePartial(ramAccountingContext, accumulatedState);
        seenFrameLowerBound = frame.lowerBound();
        seenFrameUpperBound = frame.upperBoundExclusive();
    }
}
