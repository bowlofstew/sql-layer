/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.sql.optimizer.rule;

import java.util.Arrays;
import java.util.List;

public class DefaultRules
{
    /** These are the rules that get run for normal compilation. */
    public static final List<BaseRule> DEFAULT_RULES = Arrays.asList(
        // These aren't singletons because someday they will have options.
        new ASTStatementLoader(),
        new AggregateMapper(),
        new ConstantFolder(),
        new OuterJoinPromoter(),
        new ColumnEquivalenceFinder(),
        new GroupJoinFinder(),
        new InConditionReverser(),
        new IndexPicker(),
        new NestedLoopMapper(),
        new BranchJoiner(),
        new SelectPreponer(),
        new AggregateSplitter(),
        new SortSplitter(),
        new MapFolder(),
        new ExpressionCompactor(),
        new EnvironmentFunctionFinder(),
        new OperatorAssembler()
     );

    private DefaultRules() {
    }
}
