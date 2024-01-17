/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.Function
 *  com.mongodb.client.MongoCursor
 *  com.mongodb.lang.Nullable
 *  java.lang.Iterable
 *  java.lang.Object
 *  java.util.Collection
 */
package com.mongodb.client;

import com.mongodb.Function;
import com.mongodb.client.MongoCursor;
import com.mongodb.lang.Nullable;
import java.util.Collection;

public interface MongoIterable<TResult>
extends Iterable<TResult> {
    public MongoCursor<TResult> iterator();

    public MongoCursor<TResult> cursor();

    @Nullable
    public TResult first();

    public <U> MongoIterable<U> map(Function<TResult, U> var1);

    public <A extends Collection<? super TResult>> A into(A var1);

    public MongoIterable<TResult> batchSize(int var1);
}
