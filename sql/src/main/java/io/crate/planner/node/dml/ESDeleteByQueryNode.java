/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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

package io.crate.planner.node.dml;

import com.google.common.collect.Iterators;
import io.crate.analyze.WhereClause;
import io.crate.core.StringUtils;
import io.crate.planner.node.PlanNodeVisitor;
import io.crate.planner.symbol.ValueSymbolVisitor;

public class ESDeleteByQueryNode extends DMLPlanNode {

    private final String[] indices;
    private final WhereClause whereClause;
    private final String routing;

    public ESDeleteByQueryNode(String[] indices, WhereClause whereClause) {
        assert whereClause != null;
        this.indices = indices;
        this.whereClause = whereClause;
        if (whereClause.clusteredBy().isPresent()){
            routing = StringUtils.ROUTING_JOINER.join(Iterators.transform(
                    whereClause.clusteredBy().get().iterator(), ValueSymbolVisitor.STRING.function));
        } else {
            this.routing = null;
        }
    }

    public String[] indices() {
        return indices;
    }

    public String routing() {
        return routing;
    }

    public WhereClause whereClause() {
        return whereClause;
    }

    @Override
    public <C, R> R accept(PlanNodeVisitor<C, R> visitor, C context) {
        return visitor.visitESDeleteByQueryNode(this, context);
    }
}
