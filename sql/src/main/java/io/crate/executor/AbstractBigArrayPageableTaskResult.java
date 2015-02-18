/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package io.crate.executor;

import com.google.common.base.Preconditions;
import org.elasticsearch.common.lease.Releasables;
import org.elasticsearch.common.util.ObjectArray;

import javax.annotation.Nullable;
import java.io.IOException;

public abstract class AbstractBigArrayPageableTaskResult implements TaskResult {

    protected final ObjectArray<Object[]> backingArray;
    protected final long backingArrayStartIdx;
    protected final Page page;

    public AbstractBigArrayPageableTaskResult(ObjectArray<Object[]> backingArray,
                                         long backingArrayStartIndex,
                                         long size) {
        Preconditions.checkArgument(
                backingArrayStartIndex <= backingArray.size(),
                "backingArray exceeded");
        this.backingArray = backingArray;
        this.backingArrayStartIdx = backingArrayStartIndex;
        this.page = new BigArrayPage(backingArray, backingArrayStartIndex, size, false); // TODO
    }

    @Override
    public Object[][] rows() {
        throw new UnsupportedOperationException("rows() is not supported");
    }

    @Nullable
    @Override
    public String errorMessage() {
        return null;
    }


    @Override
    public Page page() {
        return page;
    }

    @Override
    public void close() throws IOException {
        Releasables.close(backingArray);
    }
}