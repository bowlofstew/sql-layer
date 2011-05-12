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

package com.akiban.qp.physicaloperator;

import com.akiban.ais.model.GroupTable;
import com.akiban.qp.row.Row;
import com.akiban.qp.row.RowHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

class IndexLookup_Default extends PhysicalOperator
{
    // Object interface

    @Override
    public String toString()
    {
        return String.format("%s(%s limit %s)", getClass().getSimpleName(), groupTable.getName().getTableName(), limit);
    }

    // PhysicalOperator interface

    @Override
    protected Cursor cursor(StoreAdapter adapter)
    {
        return new Execution(adapter, inputOperator.cursor(adapter));
    }

    @Override
    public List<PhysicalOperator> getInputOperators()
    {
        List<PhysicalOperator> result = new ArrayList<PhysicalOperator>(1);
        result.add(inputOperator);
        return result;
    }

    @Override
    public boolean cursorAbilitiesInclude(CursorAbility ability) {
        return CursorAbility.MODIFY.equals(ability);
    }

    @Override
    public String describePlan()
    {
        return describePlan(inputOperator);
    }

    // IndexLookup_Default interface

    public IndexLookup_Default(PhysicalOperator inputOperator,
                               GroupTable groupTable,
                               Limit limit)
    {
        this.inputOperator = inputOperator;
        this.groupTable = groupTable;
        this.limit = limit;
    }

    // Class state

    private static final Logger LOG = LoggerFactory.getLogger(IndexLookup_Default.class);

    // Object state

    private final PhysicalOperator inputOperator;
    private final GroupTable groupTable;
    private final Limit limit;

    // Inner classes

    private class Execution extends SingleRowCachingCursor
    {
        // Cursor interface

        @Override
        public void open(Bindings bindings)
        {
            indexInput.open(bindings);
            advanceIndex();
        }

        @Override
        public boolean next()
        {
            if (first) {
                first = false;
            } else {
                if (groupCursor.next()) {
                    groupRow.set(groupCursor.currentRow());
                } else {
                    advanceIndex();
                }
            }
            outputRow(groupRow.get());
            if (LOG.isInfoEnabled()) {
                LOG.info("IndexLookup: {}", groupRow.isNull() ? null : groupRow.get());
            }
            return groupRow.isNotNull();
        }

        @Override
        public void close()
        {
            outputRow(null);
            indexInput.close();
            indexRow.set(null);
            groupCursor.close();
            groupRow.set(null);
        }

        // For use by this class

        private void advanceIndex()
        {
            groupRow.set(null);
            groupCursor.close();
            if (indexInput.next()) {
                indexRow.set(indexInput.currentRow());
                groupCursor.rebind(this.indexRow.get().hKey());
                groupCursor.open(UndefBindings.only());
                if (groupCursor.next()) {
                    Row currentRow = groupCursor.currentRow();
                    if (limit.limitReached(currentRow)) {
                        close();
                    } else {
                        groupRow.set(currentRow);
                    }
                }
            } else {
                indexRow.set(null);
            }
        }

        @Override
        public boolean cursorAbilitiesInclude(CursorAbility ability) {
            return CursorAbility.MODIFY.equals(ability);
        }

        @Override
        public void removeCurrentRow() {
            checkHasRow();
            groupCursor.removeCurrentRow();
            groupRow.set(null);
        }

        @Override
        public void updateCurrentRow(Row newRow) {
            checkHasRow();
            groupCursor.updateCurrentRow(newRow);
            groupRow.set(newRow);
        }

        @Override
        public ModifiableCursorBackingStore backingStore() {
            return groupCursor.backingStore();
        }

        // Execution interface

        Execution(StoreAdapter adapter, Cursor input)
        {
            this.indexInput = input;
            this.groupCursor = adapter.newGroupCursor(groupTable);
        }

        private void checkHasRow() {
            if (groupRow.isNull()) {
                throw new IllegalStateException("no row to modify");
            }
        }

        // Object state

        private final Cursor indexInput;
        private final RowHolder<Row> indexRow = new RowHolder<Row>();
        private GroupCursor groupCursor;
        private final RowHolder<Row> groupRow = new RowHolder<Row>();
        private boolean first = true;
    }
}