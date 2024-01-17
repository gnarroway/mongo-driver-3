/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.ReadConcern
 *  com.mongodb.ReadPreference
 *  com.mongodb.TransactionOptions$Builder
 *  com.mongodb.WriteConcern
 *  com.mongodb.annotations.Immutable
 *  com.mongodb.assertions.Assertions
 *  com.mongodb.lang.Nullable
 *  java.lang.Long
 *  java.lang.Object
 *  java.lang.String
 *  java.util.concurrent.TimeUnit
 */
package com.mongodb;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.annotations.Immutable;
import com.mongodb.assertions.Assertions;
import com.mongodb.lang.Nullable;
import java.util.concurrent.TimeUnit;

/*
 * Exception performing whole class analysis ignored.
 */
@Immutable
public final class TransactionOptions {
    private final ReadConcern readConcern;
    private final WriteConcern writeConcern;
    private final ReadPreference readPreference;
    private final Long maxCommitTimeMS;

    @Nullable
    public ReadConcern getReadConcern() {
        return this.readConcern;
    }

    @Nullable
    public WriteConcern getWriteConcern() {
        return this.writeConcern;
    }

    @Nullable
    public ReadPreference getReadPreference() {
        return this.readPreference;
    }

    @Nullable
    public Long getMaxCommitTime(TimeUnit timeUnit) {
        Assertions.notNull((String)"timeUnit", (Object)timeUnit);
        if (this.maxCommitTimeMS == null) {
            return null;
        }
        return timeUnit.convert(this.maxCommitTimeMS.longValue(), TimeUnit.MILLISECONDS);
    }

    public static Builder builder() {
        return new Builder(null);
    }

    public static TransactionOptions merge(TransactionOptions options, TransactionOptions defaultOptions) {
        Assertions.notNull((String)"options", (Object)options);
        Assertions.notNull((String)"defaultOptions", (Object)defaultOptions);
        return TransactionOptions.builder().writeConcern(options.getWriteConcern() == null ? defaultOptions.getWriteConcern() : options.getWriteConcern()).readConcern(options.getReadConcern() == null ? defaultOptions.getReadConcern() : options.getReadConcern()).readPreference(options.getReadPreference() == null ? defaultOptions.getReadPreference() : options.getReadPreference()).maxCommitTime(options.getMaxCommitTime(TimeUnit.MILLISECONDS) == null ? defaultOptions.getMaxCommitTime(TimeUnit.MILLISECONDS) : options.getMaxCommitTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS).build();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        TransactionOptions that = (TransactionOptions)o;
        if (this.maxCommitTimeMS != null ? !this.maxCommitTimeMS.equals((Object)that.maxCommitTimeMS) : that.maxCommitTimeMS != null) {
            return false;
        }
        if (this.readConcern != null ? !this.readConcern.equals((Object)that.readConcern) : that.readConcern != null) {
            return false;
        }
        if (this.writeConcern != null ? !this.writeConcern.equals((Object)that.writeConcern) : that.writeConcern != null) {
            return false;
        }
        return !(this.readPreference != null ? !this.readPreference.equals((Object)that.readPreference) : that.readPreference != null);
    }

    public int hashCode() {
        int result = this.readConcern != null ? this.readConcern.hashCode() : 0;
        result = 31 * result + (this.writeConcern != null ? this.writeConcern.hashCode() : 0);
        result = 31 * result + (this.readPreference != null ? this.readPreference.hashCode() : 0);
        result = 31 * result + (this.maxCommitTimeMS != null ? this.maxCommitTimeMS.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "TransactionOptions{readConcern=" + this.readConcern + ", writeConcern=" + this.writeConcern + ", readPreference=" + this.readPreference + ", maxCommitTimeMS" + this.maxCommitTimeMS + '}';
    }

    private TransactionOptions(Builder builder) {
        this.readConcern = Builder.access$200((Builder)builder);
        this.writeConcern = Builder.access$300((Builder)builder);
        this.readPreference = Builder.access$400((Builder)builder);
        this.maxCommitTimeMS = Builder.access$500((Builder)builder);
    }
}
