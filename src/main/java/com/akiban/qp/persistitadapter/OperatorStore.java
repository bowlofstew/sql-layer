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

package com.akiban.qp.persistitadapter;

import com.akiban.ais.model.AkibanInformationSchema;
import com.akiban.ais.model.GroupTable;
import com.akiban.ais.model.Index;
import com.akiban.ais.model.UserTable;
import com.akiban.message.ErrorCode;
import com.akiban.qp.expression.IndexBound;
import com.akiban.qp.expression.IndexKeyRange;
import com.akiban.qp.physicaloperator.API;
import com.akiban.qp.physicaloperator.Bindings;
import com.akiban.qp.physicaloperator.Cursor;
import com.akiban.qp.physicaloperator.CursorUpdateException;
import com.akiban.qp.physicaloperator.NoLimit;
import com.akiban.qp.physicaloperator.PhysicalOperator;
import com.akiban.qp.physicaloperator.UndefBindings;
import com.akiban.qp.physicaloperator.UpdateLambda;
import com.akiban.qp.physicaloperator.Update_Default;
import com.akiban.qp.row.Row;
import com.akiban.qp.util.SchemaCache;
import com.akiban.server.InvalidOperationException;
import com.akiban.server.RowData;
import com.akiban.server.RowDef;
import com.akiban.server.api.dml.ColumnSelector;
import com.akiban.server.api.dml.ConstantColumnSelector;
import com.akiban.server.api.dml.DuplicateKeyException;
import com.akiban.server.api.dml.NoSuchRowException;
import com.akiban.server.api.dml.scan.NewRow;
import com.akiban.server.api.dml.scan.NiceRow;
import com.akiban.server.service.ServiceManagerImpl;
import com.akiban.server.service.session.Session;
import com.akiban.server.store.DelegatingStore;
import com.akiban.server.store.PersistitStore;
import com.persistit.Transaction;

import static com.akiban.qp.physicaloperator.API.indexLookup_Default;
import static com.akiban.qp.physicaloperator.API.indexScan_Default;

public final class OperatorStore extends DelegatingStore<PersistitStore> {

    // Store interface

    @Override
    public void updateRow(Session session, RowData oldRowData, RowData newRowData, ColumnSelector columnSelector)
            throws Exception
    {
        PersistitStore persistitStore = getPersistitStore();
        AkibanInformationSchema ais = persistitStore.getRowDefCache().ais();
        PersistitAdapter adapter = new PersistitAdapter(SchemaCache.globalSchema(ais), persistitStore, session);

        PersistitGroupRow oldRow = PersistitGroupRow.newPersistitGroupRow(adapter, oldRowData);
        RowDef rowDef = persistitStore.getRowDefCache().rowDef(oldRowData.getRowDefId());
        UpdateLambda updateLambda = new InternalUpdateLambda(adapter, rowDef, newRowData, columnSelector);

        UserTable userTable = ais.getUserTable(oldRowData.getRowDefId());
        GroupTable groupTable = userTable.getGroup().getGroupTable();
        IndexBound bound = new IndexBound(userTable, oldRow, ConstantColumnSelector.ALL_ON);
        IndexKeyRange range = new IndexKeyRange(bound, true, bound, true);

        final PhysicalOperator scanOp;
        if (rowDef.getPKIndexDef() != null && rowDef.getPKIndexDef().isHKeyEquivalent()) {
            scanOp = API.groupScan_Default(groupTable, false, NoLimit.instance(), range);
        }
        else {
            Index index = userTable.getIndex("PRIMARY");
            PhysicalOperator indexScan = indexScan_Default(index, false, range);
            scanOp = indexLookup_Default(indexScan, groupTable);
        }

        Update_Default updateOp = new Update_Default(scanOp, updateLambda);

        Cursor updateCursor = API.cursor(updateOp, adapter);
        Transaction transaction = ServiceManagerImpl.get().getTreeService().getTransaction(session);
        try {
            transaction.begin();
            runCursor(oldRowData, rowDef, updateCursor);
            transaction.commit();
        } finally {
            transaction.end();
        }
    }

    private static void runCursor(RowData oldRowData, RowDef rowDef, Cursor updateCursor)
            throws DuplicateKeyException, NoSuchRowException
    {
        updateCursor.open(UndefBindings.only());
        try {
            try {
                if (!updateCursor.next()) {
                    String rowDescription;
                    try {
                        rowDescription = oldRowData.toString(rowDef);
                    } catch (Exception e) {
                        rowDescription = "error in generating RowData.toString";
                    }
                    throw new NoSuchRowException(rowDescription);
                }
            } catch (CursorUpdateException e) {
                Throwable cause = e.getCause();
                if ( (cause instanceof InvalidOperationException)
                        && ErrorCode.DUPLICATE_KEY.equals(((InvalidOperationException) cause).getCode()))
                {
                    throw new DuplicateKeyException((InvalidOperationException)cause);
                }
                throw e;
            }
        } finally {
            updateCursor.close();
        }
    }

    // OperatorStore interface

    public OperatorStore() {
        super(new PersistitStore());
    }

    public PersistitStore getPersistitStore() {
        return super.getDelegate();
    }

    // inner classes

    private static class InternalUpdateLambda implements UpdateLambda {
        private final PersistitAdapter adapter;
        private final RowData newRowData;
        private final ColumnSelector columnSelector;
        private final RowDef rowDef;

        private InternalUpdateLambda(PersistitAdapter adapter, RowDef rowDef, RowData newRowData, ColumnSelector columnSelector) {
            this.newRowData = newRowData;
            this.columnSelector = columnSelector;
            this.rowDef = rowDef;
            this.adapter = adapter;
        }

        @Override
        public boolean rowIsApplicable(Row row) {
            return row.rowType().typeId() == rowDef.getRowDefId();
        }

        @Override
        public Row applyUpdate(Row original, Bindings bindings) {
            // TODO
            // ideally we'd like to use an OverlayingRow, but ModifiablePersistitGroupCursor requires
            // a PersistitGroupRow if an hkey changes
//            OverlayingRow overlay = new OverlayingRow(original);
//            for (int i=0; i < rowDef.getFieldCount(); ++i) {
//                if (columnSelector == null || columnSelector.includesColumn(i)) {
//                    overlay.overlay(i, newRowData.toObject(rowDef, i));
//                }
//            }
//            return overlay;
            NewRow newRow = new NiceRow(rowDef.getRowDefId());
            for (int i=0; i < original.rowType().nFields(); ++i) {
                if (columnSelector == null || columnSelector.includesColumn(i)) {
                    newRow.put(i, newRowData.toObject(rowDef, i));
                }
                else {
                    newRow.put(i, original.field(i, bindings));
                }
            }
            return PersistitGroupRow.newPersistitGroupRow(adapter, newRow.toRowData());
        }
    }
}