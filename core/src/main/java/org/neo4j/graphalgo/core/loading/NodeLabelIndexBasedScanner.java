/*
 * Copyright (c) 2017-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.graphalgo.core.loading;

import org.neo4j.graphalgo.core.SecureTransaction;
import org.neo4j.internal.kernel.api.NodeLabelIndexCursor;
import org.neo4j.internal.kernel.api.Scan;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.impl.store.NeoStores;
import org.neo4j.kernel.impl.store.NodeStore;

import java.util.Arrays;

import static org.neo4j.graphalgo.core.GraphDimensions.ANY_LABEL;

final class NodeLabelIndexBasedScanner extends AbstractCursorBasedScanner<NodeReference, NodeLabelIndexCursor, NodeStore, Integer> {

    static Factory<NodeReference> factory(int[] labelIds) {
        if (Arrays.stream(labelIds).anyMatch(labelId -> labelId == ANY_LABEL)) {
            return NodeCursorBasedScanner.FACTORY;
//        } else if (labelIds.length == 1) {
//            return (prefetchSize, transaction) -> new NodeLabelIndexBasedScanner(labelIds[0], prefetchSize, transaction);
        } else {
            return MultipleNodeLabelIndexBasedScanner.factory(labelIds);
        }
    }

    private final int labelId;

    private NodeLabelIndexBasedScanner(int labelId, int prefetchSize, SecureTransaction transaction) {
        super(prefetchSize, transaction, labelId);
        this.labelId = labelId;
    }

    @Override
    NodeStore store(NeoStores neoStores) {
        return neoStores.getNodeStore();
    }

    @Override
    NodeLabelIndexCursor entityCursor(KernelTransaction transaction) {
        return transaction.cursors().allocateNodeLabelIndexCursor();
    }

    @Override
    Scan<NodeLabelIndexCursor> entityCursorScan(KernelTransaction transaction, Integer labelId) {
        var read = transaction.dataRead();
        read.prepareForLabelScans();
        return read.nodeLabelScan(labelId);
    }

    @Override
    NodeReference cursorReference(KernelTransaction transaction, NodeLabelIndexCursor cursor) {
        return new NodeLabelIndexReference(
            cursor,
            transaction.dataRead(),
            transaction.cursors().allocateNodeCursor(),
            new long[]{labelId}
        );
    }
}
