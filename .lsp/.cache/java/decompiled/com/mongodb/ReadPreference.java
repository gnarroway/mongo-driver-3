/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.ReadPreference$1
 *  com.mongodb.ReadPreference$PrimaryReadPreference
 *  com.mongodb.ReadPreferenceHedgeOptions
 *  com.mongodb.TagSet
 *  com.mongodb.TaggableReadPreference
 *  com.mongodb.TaggableReadPreference$NearestReadPreference
 *  com.mongodb.TaggableReadPreference$PrimaryPreferredReadPreference
 *  com.mongodb.TaggableReadPreference$SecondaryPreferredReadPreference
 *  com.mongodb.TaggableReadPreference$SecondaryReadPreference
 *  com.mongodb.annotations.Immutable
 *  com.mongodb.assertions.Assertions
 *  com.mongodb.connection.ClusterDescription
 *  com.mongodb.connection.ServerDescription
 *  com.mongodb.lang.Nullable
 *  java.lang.Deprecated
 *  java.lang.IllegalArgumentException
 *  java.lang.Long
 *  java.lang.Object
 *  java.lang.String
 *  java.lang.UnsupportedOperationException
 *  java.util.Collections
 *  java.util.List
 *  java.util.concurrent.TimeUnit
 *  org.bson.BsonDocument
 */
package com.mongodb;

import com.mongodb.ReadPreference;
import com.mongodb.ReadPreferenceHedgeOptions;
import com.mongodb.TagSet;
import com.mongodb.TaggableReadPreference;
import com.mongodb.annotations.Immutable;
import com.mongodb.assertions.Assertions;
import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ServerDescription;
import com.mongodb.lang.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bson.BsonDocument;

@Immutable
public abstract class ReadPreference {
    private static final ReadPreference PRIMARY = new PrimaryReadPreference(null);
    private static final ReadPreference SECONDARY = new TaggableReadPreference.SecondaryReadPreference();
    private static final ReadPreference SECONDARY_PREFERRED = new TaggableReadPreference.SecondaryPreferredReadPreference();
    private static final ReadPreference PRIMARY_PREFERRED = new TaggableReadPreference.PrimaryPreferredReadPreference();
    private static final ReadPreference NEAREST = new TaggableReadPreference.NearestReadPreference();

    ReadPreference() {
    }

    public abstract ReadPreference withTagSet(TagSet var1);

    public abstract ReadPreference withTagSetList(List<TagSet> var1);

    public abstract ReadPreference withMaxStalenessMS(Long var1, TimeUnit var2);

    public abstract ReadPreference withHedgeOptions(ReadPreferenceHedgeOptions var1);

    @Deprecated
    public abstract boolean isSlaveOk();

    public abstract boolean isSecondaryOk();

    public abstract String getName();

    public abstract BsonDocument toDocument();

    public final List<ServerDescription> choose(ClusterDescription clusterDescription) {
        switch (1.$SwitchMap$com$mongodb$connection$ClusterType[clusterDescription.getType().ordinal()]) {
            case 1: {
                return this.chooseForReplicaSet(clusterDescription);
            }
            case 2: 
            case 3: {
                return this.chooseForNonReplicaSet(clusterDescription);
            }
            case 4: {
                return clusterDescription.getServerDescriptions();
            }
            case 5: {
                return Collections.emptyList();
            }
        }
        throw new UnsupportedOperationException("Unsupported cluster type: " + clusterDescription.getType());
    }

    protected abstract List<ServerDescription> chooseForNonReplicaSet(ClusterDescription var1);

    protected abstract List<ServerDescription> chooseForReplicaSet(ClusterDescription var1);

    public static ReadPreference primary() {
        return PRIMARY;
    }

    public static ReadPreference primaryPreferred() {
        return PRIMARY_PREFERRED;
    }

    public static ReadPreference secondary() {
        return SECONDARY;
    }

    public static ReadPreference secondaryPreferred() {
        return SECONDARY_PREFERRED;
    }

    public static ReadPreference nearest() {
        return NEAREST;
    }

    public static ReadPreference primaryPreferred(long maxStaleness, TimeUnit timeUnit) {
        return new TaggableReadPreference.PrimaryPreferredReadPreference(Collections.emptyList(), Long.valueOf((long)maxStaleness), timeUnit);
    }

    public static ReadPreference secondary(long maxStaleness, TimeUnit timeUnit) {
        return new TaggableReadPreference.SecondaryReadPreference(Collections.emptyList(), Long.valueOf((long)maxStaleness), timeUnit);
    }

    public static ReadPreference secondaryPreferred(long maxStaleness, TimeUnit timeUnit) {
        return new TaggableReadPreference.SecondaryPreferredReadPreference(Collections.emptyList(), Long.valueOf((long)maxStaleness), timeUnit);
    }

    public static ReadPreference nearest(long maxStaleness, TimeUnit timeUnit) {
        return new TaggableReadPreference.NearestReadPreference(Collections.emptyList(), Long.valueOf((long)maxStaleness), timeUnit);
    }

    public static TaggableReadPreference primaryPreferred(TagSet tagSet) {
        return new TaggableReadPreference.PrimaryPreferredReadPreference(Collections.singletonList((Object)tagSet), null, TimeUnit.MILLISECONDS);
    }

    public static TaggableReadPreference secondary(TagSet tagSet) {
        return new TaggableReadPreference.SecondaryReadPreference(Collections.singletonList((Object)tagSet), null, TimeUnit.MILLISECONDS);
    }

    public static TaggableReadPreference secondaryPreferred(TagSet tagSet) {
        return new TaggableReadPreference.SecondaryPreferredReadPreference(Collections.singletonList((Object)tagSet), null, TimeUnit.MILLISECONDS);
    }

    public static TaggableReadPreference nearest(TagSet tagSet) {
        return new TaggableReadPreference.NearestReadPreference(Collections.singletonList((Object)tagSet), null, TimeUnit.MILLISECONDS);
    }

    public static TaggableReadPreference primaryPreferred(TagSet tagSet, long maxStaleness, TimeUnit timeUnit) {
        return new TaggableReadPreference.PrimaryPreferredReadPreference(Collections.singletonList((Object)tagSet), Long.valueOf((long)maxStaleness), timeUnit);
    }

    public static TaggableReadPreference secondary(TagSet tagSet, long maxStaleness, TimeUnit timeUnit) {
        return new TaggableReadPreference.SecondaryReadPreference(Collections.singletonList((Object)tagSet), Long.valueOf((long)maxStaleness), timeUnit);
    }

    public static TaggableReadPreference secondaryPreferred(TagSet tagSet, long maxStaleness, TimeUnit timeUnit) {
        return new TaggableReadPreference.SecondaryPreferredReadPreference(Collections.singletonList((Object)tagSet), Long.valueOf((long)maxStaleness), timeUnit);
    }

    public static TaggableReadPreference nearest(TagSet tagSet, long maxStaleness, TimeUnit timeUnit) {
        return new TaggableReadPreference.NearestReadPreference(Collections.singletonList((Object)tagSet), Long.valueOf((long)maxStaleness), timeUnit);
    }

    public static TaggableReadPreference primaryPreferred(List<TagSet> tagSetList) {
        return new TaggableReadPreference.PrimaryPreferredReadPreference(tagSetList, null, TimeUnit.MILLISECONDS);
    }

    public static TaggableReadPreference secondary(List<TagSet> tagSetList) {
        return new TaggableReadPreference.SecondaryReadPreference(tagSetList, null, TimeUnit.MILLISECONDS);
    }

    public static TaggableReadPreference secondaryPreferred(List<TagSet> tagSetList) {
        return new TaggableReadPreference.SecondaryPreferredReadPreference(tagSetList, null, TimeUnit.MILLISECONDS);
    }

    public static TaggableReadPreference nearest(List<TagSet> tagSetList) {
        return new TaggableReadPreference.NearestReadPreference(tagSetList, null, TimeUnit.MILLISECONDS);
    }

    public static TaggableReadPreference primaryPreferred(List<TagSet> tagSetList, long maxStaleness, TimeUnit timeUnit) {
        return new TaggableReadPreference.PrimaryPreferredReadPreference(tagSetList, Long.valueOf((long)maxStaleness), timeUnit);
    }

    public static TaggableReadPreference secondary(List<TagSet> tagSetList, long maxStaleness, TimeUnit timeUnit) {
        return new TaggableReadPreference.SecondaryReadPreference(tagSetList, Long.valueOf((long)maxStaleness), timeUnit);
    }

    public static TaggableReadPreference secondaryPreferred(List<TagSet> tagSetList, long maxStaleness, TimeUnit timeUnit) {
        return new TaggableReadPreference.SecondaryPreferredReadPreference(tagSetList, Long.valueOf((long)maxStaleness), timeUnit);
    }

    public static TaggableReadPreference nearest(List<TagSet> tagSetList, long maxStaleness, TimeUnit timeUnit) {
        return new TaggableReadPreference.NearestReadPreference(tagSetList, Long.valueOf((long)maxStaleness), timeUnit);
    }

    public static ReadPreference valueOf(String name) {
        Assertions.notNull((String)"name", (Object)name);
        String nameToCheck = name.toLowerCase();
        if (nameToCheck.equals((Object)PRIMARY.getName().toLowerCase())) {
            return PRIMARY;
        }
        if (nameToCheck.equals((Object)SECONDARY.getName().toLowerCase())) {
            return SECONDARY;
        }
        if (nameToCheck.equals((Object)SECONDARY_PREFERRED.getName().toLowerCase())) {
            return SECONDARY_PREFERRED;
        }
        if (nameToCheck.equals((Object)PRIMARY_PREFERRED.getName().toLowerCase())) {
            return PRIMARY_PREFERRED;
        }
        if (nameToCheck.equals((Object)NEAREST.getName().toLowerCase())) {
            return NEAREST;
        }
        throw new IllegalArgumentException("No match for read preference of " + name);
    }

    public static TaggableReadPreference valueOf(String name, List<TagSet> tagSetList) {
        return ReadPreference.valueOf(name, tagSetList, null, TimeUnit.MILLISECONDS);
    }

    public static TaggableReadPreference valueOf(String name, List<TagSet> tagSetList, long maxStaleness, TimeUnit timeUnit) {
        return ReadPreference.valueOf(name, tagSetList, (Long)maxStaleness, timeUnit);
    }

    private static TaggableReadPreference valueOf(String name, List<TagSet> tagSetList, @Nullable Long maxStaleness, TimeUnit timeUnit) {
        Assertions.notNull((String)"name", (Object)name);
        Assertions.notNull((String)"tagSetList", tagSetList);
        Assertions.notNull((String)"timeUnit", (Object)timeUnit);
        String nameToCheck = name.toLowerCase();
        if (nameToCheck.equals((Object)PRIMARY.getName().toLowerCase())) {
            throw new IllegalArgumentException("Primary read preference can not also specify tag sets, max staleness or hedge");
        }
        if (nameToCheck.equals((Object)SECONDARY.getName().toLowerCase())) {
            return new TaggableReadPreference.SecondaryReadPreference(tagSetList, maxStaleness, timeUnit);
        }
        if (nameToCheck.equals((Object)SECONDARY_PREFERRED.getName().toLowerCase())) {
            return new TaggableReadPreference.SecondaryPreferredReadPreference(tagSetList, maxStaleness, timeUnit);
        }
        if (nameToCheck.equals((Object)PRIMARY_PREFERRED.getName().toLowerCase())) {
            return new TaggableReadPreference.PrimaryPreferredReadPreference(tagSetList, maxStaleness, timeUnit);
        }
        if (nameToCheck.equals((Object)NEAREST.getName().toLowerCase())) {
            return new TaggableReadPreference.NearestReadPreference(tagSetList, maxStaleness, timeUnit);
        }
        throw new IllegalArgumentException("No match for read preference of " + name);
    }
}
