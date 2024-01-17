/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.assertions.Assertions
 *  com.mongodb.client.model.ChangeStreamPreAndPostImagesOptions
 *  com.mongodb.client.model.ClusteredIndexOptions
 *  com.mongodb.client.model.Collation
 *  com.mongodb.client.model.IndexOptionDefaults
 *  com.mongodb.client.model.TimeSeriesOptions
 *  com.mongodb.client.model.ValidationOptions
 *  com.mongodb.lang.Nullable
 *  java.lang.IllegalArgumentException
 *  java.lang.Object
 *  java.lang.String
 *  java.util.concurrent.TimeUnit
 *  org.bson.conversions.Bson
 */
package com.mongodb.client.model;

import com.mongodb.assertions.Assertions;
import com.mongodb.client.model.ChangeStreamPreAndPostImagesOptions;
import com.mongodb.client.model.ClusteredIndexOptions;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.IndexOptionDefaults;
import com.mongodb.client.model.TimeSeriesOptions;
import com.mongodb.client.model.ValidationOptions;
import com.mongodb.lang.Nullable;
import java.util.concurrent.TimeUnit;
import org.bson.conversions.Bson;

public class CreateCollectionOptions {
    private long maxDocuments;
    private boolean capped;
    private long sizeInBytes;
    private Bson storageEngineOptions;
    private IndexOptionDefaults indexOptionDefaults = new IndexOptionDefaults();
    private ValidationOptions validationOptions = new ValidationOptions();
    private Collation collation;
    private long expireAfterSeconds;
    private TimeSeriesOptions timeSeriesOptions;
    private ChangeStreamPreAndPostImagesOptions changeStreamPreAndPostImagesOptions;
    private ClusteredIndexOptions clusteredIndexOptions;
    private Bson encryptedFields;

    public long getMaxDocuments() {
        return this.maxDocuments;
    }

    public CreateCollectionOptions maxDocuments(long maxDocuments) {
        this.maxDocuments = maxDocuments;
        return this;
    }

    public boolean isCapped() {
        return this.capped;
    }

    public CreateCollectionOptions capped(boolean capped) {
        this.capped = capped;
        return this;
    }

    public long getSizeInBytes() {
        return this.sizeInBytes;
    }

    public CreateCollectionOptions sizeInBytes(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
        return this;
    }

    @Nullable
    public Bson getStorageEngineOptions() {
        return this.storageEngineOptions;
    }

    public CreateCollectionOptions storageEngineOptions(@Nullable Bson storageEngineOptions) {
        this.storageEngineOptions = storageEngineOptions;
        return this;
    }

    public IndexOptionDefaults getIndexOptionDefaults() {
        return this.indexOptionDefaults;
    }

    public CreateCollectionOptions indexOptionDefaults(IndexOptionDefaults indexOptionDefaults) {
        this.indexOptionDefaults = (IndexOptionDefaults)Assertions.notNull((String)"indexOptionDefaults", (Object)indexOptionDefaults);
        return this;
    }

    public ValidationOptions getValidationOptions() {
        return this.validationOptions;
    }

    public CreateCollectionOptions validationOptions(ValidationOptions validationOptions) {
        this.validationOptions = (ValidationOptions)Assertions.notNull((String)"validationOptions", (Object)validationOptions);
        return this;
    }

    @Nullable
    public Collation getCollation() {
        return this.collation;
    }

    public CreateCollectionOptions collation(@Nullable Collation collation) {
        this.collation = collation;
        return this;
    }

    public long getExpireAfter(TimeUnit timeUnit) {
        Assertions.notNull((String)"timeUnit", (Object)timeUnit);
        return timeUnit.convert(this.expireAfterSeconds, TimeUnit.SECONDS);
    }

    public CreateCollectionOptions expireAfter(long expireAfter, TimeUnit timeUnit) {
        Assertions.notNull((String)"timeUnit", (Object)timeUnit);
        long asSeconds = TimeUnit.SECONDS.convert(expireAfter, timeUnit);
        if (asSeconds < 0L) {
            throw new IllegalArgumentException("expireAfter, after conversion to seconds, must be >= 0");
        }
        this.expireAfterSeconds = asSeconds;
        return this;
    }

    @Nullable
    public TimeSeriesOptions getTimeSeriesOptions() {
        return this.timeSeriesOptions;
    }

    public CreateCollectionOptions timeSeriesOptions(TimeSeriesOptions timeSeriesOptions) {
        this.timeSeriesOptions = timeSeriesOptions;
        return this;
    }

    @Nullable
    public ClusteredIndexOptions getClusteredIndexOptions() {
        return this.clusteredIndexOptions;
    }

    public CreateCollectionOptions clusteredIndexOptions(ClusteredIndexOptions clusteredIndexOptions) {
        this.clusteredIndexOptions = clusteredIndexOptions;
        return this;
    }

    @Nullable
    public ChangeStreamPreAndPostImagesOptions getChangeStreamPreAndPostImagesOptions() {
        return this.changeStreamPreAndPostImagesOptions;
    }

    public CreateCollectionOptions changeStreamPreAndPostImagesOptions(ChangeStreamPreAndPostImagesOptions changeStreamPreAndPostImagesOptions) {
        this.changeStreamPreAndPostImagesOptions = changeStreamPreAndPostImagesOptions;
        return this;
    }

    @Nullable
    public Bson getEncryptedFields() {
        return this.encryptedFields;
    }

    public CreateCollectionOptions encryptedFields(@Nullable Bson encryptedFields) {
        this.encryptedFields = encryptedFields;
        return this;
    }

    public String toString() {
        return "CreateCollectionOptions{, maxDocuments=" + this.maxDocuments + ", capped=" + this.capped + ", sizeInBytes=" + this.sizeInBytes + ", storageEngineOptions=" + this.storageEngineOptions + ", indexOptionDefaults=" + this.indexOptionDefaults + ", validationOptions=" + this.validationOptions + ", collation=" + this.collation + ", expireAfterSeconds=" + this.expireAfterSeconds + ", timeSeriesOptions=" + this.timeSeriesOptions + ", changeStreamPreAndPostImagesOptions=" + this.changeStreamPreAndPostImagesOptions + ", encryptedFields=" + this.encryptedFields + '}';
    }
}
