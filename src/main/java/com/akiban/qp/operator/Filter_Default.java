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

package com.akiban.qp.operator;

import com.akiban.qp.row.Row;
import com.akiban.qp.rowtype.RowType;
import com.akiban.util.ArgumentValidation;

import java.util.*;

class Filter_Default extends Operator
{
    // Object interface

    @Override
    public String toString()
    {
        TreeSet<String> keepTypesStrings = new TreeSet<String>();
        for (RowType keepType : keepTypes) {
            keepTypesStrings.add(String.valueOf(keepType));
        }
        return String.format("%s(%s)", getClass().getSimpleName(), keepTypesStrings);
    }

    // Operator interface

    @Override
    public void findDerivedTypes(Set<RowType> derivedTypes)
    {
        inputOperator.findDerivedTypes(derivedTypes);
    }

    @Override
    public List<Operator> getInputOperators()
    {
        return Collections.singletonList(inputOperator);
    }

    @Override
    protected Cursor cursor(StoreAdapter adapter)
    {
        return new Execution(adapter, inputOperator.cursor(adapter));
    }

    @Override
    public String describePlan()
    {
        return describePlan(inputOperator);
    }

    // Filter_Default interface

    public Filter_Default(Operator inputOperator, Collection<RowType> keepTypes)
    {
        ArgumentValidation.notEmpty("keepTypes", keepTypes);
        this.inputOperator = inputOperator;
        this.keepTypes = new HashSet<RowType>(keepTypes);
    }

    // Object state

    private final Operator inputOperator;
    private final Set<RowType> keepTypes;

    // Inner classes

    private class Execution implements Cursor
    {
        // Cursor interface

        @Override
        public void open(Bindings bindings)
        {
            input.open(bindings);
            closed = false;
        }

        @Override
        public Row next()
        {
            Row row;
            do {
                row = input.next();
                if (row == null) {
                    close();
                } else if (!keepTypes.contains(row.rowType())) {
                    row = null;
                }
            } while (row == null && !closed);
            return row;
        }

        @Override
        public void close()
        {
            input.close();
            closed = true;
        }

        // Execution interface

        Execution(StoreAdapter adapter, Cursor input)
        {
            this.input = input;
        }

        // Object state

        private final Cursor input;
        private boolean closed = false;
    }
}