/*
 * This file is part of a module with proprietary Enterprise Features.
 *
 * Licensed to Crate.io Inc. ("Crate.io") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * To use this file, Crate.io must have given you permission to enable and
 * use such Enterprise Features and you must have a valid Enterprise or
 * Subscription Agreement with Crate.io.  If you enable or use the Enterprise
 * Features, you represent and warrant that you have a valid Enterprise or
 * Subscription Agreement with Crate.io.  Your use of the Enterprise Features
 * if governed by the terms and conditions of your Enterprise or Subscription
 * Agreement with Crate.io.
 */

package io.crate.beans;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.crate.metadata.sys.ClassifiedMetrics.Metrics;
import io.crate.metadata.sys.MetricsView;
import io.crate.planner.Plan.StatementType;
import io.crate.planner.operators.StatementClassifier.Classification;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.crate.beans.QueryStats.createMetricsMap;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class QueryStatsTest {

    private static final Classification SELECT_CLASSIFICATION = new Classification(StatementType.SELECT);
    private static final Classification UPDATE_CLASSIFICATION = new Classification(StatementType.UPDATE);
    private static final Classification DELETE_CLASSIFICATION = new Classification(StatementType.DELETE);
    private static final Classification INSERT_CLASSIFICATION = new Classification(StatementType.INSERT);
    private static final Classification DDL_CLASSIFICATION = new Classification(StatementType.DDL);
    private static final Classification COPY_CLASSIFICATION = new Classification(StatementType.COPY);

    private final List<MetricsView> metrics = ImmutableList.of(
        createMetric(SELECT_CLASSIFICATION, 35),
        createMetric(SELECT_CLASSIFICATION, 35),
        createMetric(UPDATE_CLASSIFICATION, 20),
        createMetric(INSERT_CLASSIFICATION, 19),
        createMetric(DELETE_CLASSIFICATION, 5),
        createMetric(DELETE_CLASSIFICATION, 10),
        createMetric(DDL_CLASSIFICATION, 1),
        createFailedExecutionMetric(SELECT_CLASSIFICATION, 20),
        createFailedExecutionMetric(COPY_CLASSIFICATION, 0)
    );

    private MetricsView createMetric(Classification classification, long duration) {
        Metrics metrics = new Metrics(classification);
        metrics.recordValue(duration);
        return metrics.createMetricsView();
    }

    private MetricsView createFailedExecutionMetric(Classification classification, long duration) {
        Metrics metrics = new Metrics(classification);
        metrics.recordFailedExecution(duration);
        return metrics.createMetricsView();
    }

    @Test
    public void testTrackedStatementTypes() {
        List<MetricsView> oneMetricForEachStatementType = new ArrayList<>();
        for (StatementType type : StatementType.values()) {
            if(type.equals(StatementType.UNDEFINED)) {
                continue;
            }
            oneMetricForEachStatementType.add(createMetric(new Classification(type), 1));
        }
        Map<StatementType, QueryStats.Metric> metricsByCommand = createMetricsMap(oneMetricForEachStatementType);

        assertThat(metricsByCommand.size(), is(7));
        assertThat(metricsByCommand.get(StatementType.SELECT), is(notNullValue()));
        assertThat(metricsByCommand.get(StatementType.UPDATE), is(notNullValue()));
        assertThat(metricsByCommand.get(StatementType.INSERT), is(notNullValue()));
        assertThat(metricsByCommand.get(StatementType.DELETE), is(notNullValue()));
        assertThat(metricsByCommand.get(StatementType.DDL), is(notNullValue()));
        assertThat(metricsByCommand.get(StatementType.MANAGEMENT), is(notNullValue()));
        assertThat(metricsByCommand.get(StatementType.COPY), is(notNullValue()));

        assertThat(metricsByCommand.get(StatementType.UNDEFINED), is(nullValue()));
    }

    @Test
    public void testCreateMetricsMap() {
        Map<StatementType, QueryStats.Metric> metricsByCommand = createMetricsMap(metrics);
        assertThat(metricsByCommand.size(), is(6));

        assertThat(metricsByCommand.get(StatementType.SELECT).totalCount(), is(3L));
        assertThat(metricsByCommand.get(StatementType.SELECT).failedCount(), is(1L));
        assertThat(metricsByCommand.get(StatementType.SELECT).sumOfDurations(), is(90L));

        assertThat(metricsByCommand.get(StatementType.INSERT).totalCount(), is(1L));
        assertThat(metricsByCommand.get(StatementType.INSERT).failedCount(), is(0L));
        assertThat(metricsByCommand.get(StatementType.INSERT).sumOfDurations(), is(19L));

        assertThat(metricsByCommand.get(StatementType.UPDATE).totalCount(), is(1L));
        assertThat(metricsByCommand.get(StatementType.UPDATE).failedCount(), is(0L));
        assertThat(metricsByCommand.get(StatementType.UPDATE).sumOfDurations(), is(20L));

        assertThat(metricsByCommand.get(StatementType.DELETE).totalCount(), is(2L));
        assertThat(metricsByCommand.get(StatementType.DELETE).failedCount(), is(0L));
        assertThat(metricsByCommand.get(StatementType.DELETE).sumOfDurations(), is(15L));

        assertThat(metricsByCommand.get(StatementType.DDL).totalCount(), is(1L));
        assertThat(metricsByCommand.get(StatementType.DDL).failedCount(), is(0L));
        assertThat(metricsByCommand.get(StatementType.DDL).sumOfDurations(), is(1L));

        assertThat(metricsByCommand.get(StatementType.COPY).totalCount(), is(1L));
        assertThat(metricsByCommand.get(StatementType.COPY).failedCount(), is(1L));
        assertThat(metricsByCommand.get(StatementType.COPY).sumOfDurations(), is(0L));
    }

    @Test
    public void testSameTypeWithDifferentLabelsClassificationsAreMerged() {
        List<MetricsView> selectMetrics = ImmutableList.of(
            createMetric(SELECT_CLASSIFICATION, 35),
            createMetric(new Classification(StatementType.SELECT, ImmutableSet.of("lookup")), 55)
        );
        Map<StatementType, QueryStats.Metric> metricsByCommand = createMetricsMap(selectMetrics);
        assertThat(metricsByCommand.size(), is(1));

        assertThat(metricsByCommand.get(StatementType.SELECT).totalCount(), is(2L));
        assertThat(metricsByCommand.get(StatementType.SELECT).sumOfDurations(), is(90L));
    }
}
