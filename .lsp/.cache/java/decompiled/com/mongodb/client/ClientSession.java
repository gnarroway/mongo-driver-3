/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.ServerAddress
 *  com.mongodb.client.TransactionBody
 *  com.mongodb.lang.Nullable
 *  com.mongodb.session.ClientSession
 *  java.lang.Object
 */
package com.mongodb.client;

import com.mongodb.ServerAddress;
import com.mongodb.TransactionOptions;
import com.mongodb.client.TransactionBody;
import com.mongodb.lang.Nullable;

public interface ClientSession
extends com.mongodb.session.ClientSession {
    @Nullable
    public ServerAddress getPinnedServerAddress();

    public boolean hasActiveTransaction();

    public boolean notifyMessageSent();

    public void notifyOperationInitiated(Object var1);

    public TransactionOptions getTransactionOptions();

    public void startTransaction();

    public void startTransaction(TransactionOptions var1);

    public void commitTransaction();

    public void abortTransaction();

    public <T> T withTransaction(TransactionBody<T> var1);

    public <T> T withTransaction(TransactionBody<T> var1, TransactionOptions var2);
}
