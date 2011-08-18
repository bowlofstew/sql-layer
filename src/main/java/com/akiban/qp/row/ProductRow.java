/** *
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

package com.akiban.qp.row;

import com.akiban.qp.physicaloperator.Bindings;
import com.akiban.qp.rowtype.ProductRowType;
import com.akiban.qp.rowtype.RowType;
import com.akiban.server.types.ConversionSource;
import com.akiban.server.types.NullConversionSource;

public class ProductRow extends AbstractRow
{
    // Object interface

    @Override
    public String toString()
    {
        return String.format("%s, %s", left, right);
    }

    // Row interface

    @Override
    public RowType rowType()
    {
        return rowType;
    }

    @Override
    public ConversionSource conversionSource(int i, Bindings bindings) {
        ConversionSource source;
        if (i < nLeftFields) {
            source = left.isNull() ? NullConversionSource.only() : left.get().conversionSource(i, bindings);
        } else {
            source = right.isNull() ? NullConversionSource.only() : right.get().conversionSource(i - nLeftFields, bindings);
        }
        return source;
    }

    @Override
    public HKey hKey()
    {
        return null;
    }

    // ProductRow interface

    public ProductRow(ProductRowType rowType, Row left, Row right)
    {
        this.rowType = rowType;
        this.left.set(left);
        this.right.set(right);
        this.nLeftFields = rowType.leftType().nFields();
        if (left != null && right != null) {
            assert left.runId() == right.runId();
        }
        super.runId(left == null ? right.runId() : left.runId());
    }

    // Object state

    private final ProductRowType rowType;
    private final RowHolder<Row> left = new RowHolder<Row>();
    private final RowHolder<Row> right = new RowHolder<Row>();
    private final int nLeftFields;
}