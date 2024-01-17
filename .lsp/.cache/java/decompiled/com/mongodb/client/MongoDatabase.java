/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.ReadConcern
 *  com.mongodb.ReadPreference
 *  com.mongodb.WriteConcern
 *  com.mongodb.annotations.ThreadSafe
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.ChangeStreamIterable
 *  com.mongodb.client.ListCollectionsIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.model.CreateCollectionOptions
 *  com.mongodb.client.model.CreateViewOptions
 *  java.lang.Class
 *  java.lang.Object
 *  java.lang.String
 *  java.util.List
 *  org.bson.Document
 *  org.bson.codecs.configuration.CodecRegistry
 *  org.bson.conversions.Bson
 */
package com.mongodb.client;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.annotations.ThreadSafe;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.ListCollectionsIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.CreateViewOptions;
import java.util.List;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

@ThreadSafe
public interface MongoDatabase {
    public String getName();

    public CodecRegistry getCodecRegistry();

    public ReadPreference getReadPreference();

    public WriteConcern getWriteConcern();

    public ReadConcern getReadConcern();

    public MongoDatabase withCodecRegistry(CodecRegistry var1);

    public MongoDatabase withReadPreference(ReadPreference var1);

    public MongoDatabase withWriteConcern(WriteConcern var1);

    public MongoDatabase withReadConcern(ReadConcern var1);

    public MongoCollection<Document> getCollection(String var1);

    public <TDocument> MongoCollection<TDocument> getCollection(String var1, Class<TDocument> var2);

    public Document runCommand(Bson var1);

    public Document runCommand(Bson var1, ReadPreference var2);

    public <TResult> TResult runCommand(Bson var1, Class<TResult> var2);

    public <TResult> TResult runCommand(Bson var1, ReadPreference var2, Class<TResult> var3);

    public Document runCommand(ClientSession var1, Bson var2);

    public Document runCommand(ClientSession var1, Bson var2, ReadPreference var3);

    public <TResult> TResult runCommand(ClientSession var1, Bson var2, Class<TResult> var3);

    public <TResult> TResult runCommand(ClientSession var1, Bson var2, ReadPreference var3, Class<TResult> var4);

    public void drop();

    public void drop(ClientSession var1);

    public MongoIterable<String> listCollectionNames();

    public ListCollectionsIterable<Document> listCollections();

    public <TResult> ListCollectionsIterable<TResult> listCollections(Class<TResult> var1);

    public MongoIterable<String> listCollectionNames(ClientSession var1);

    public ListCollectionsIterable<Document> listCollections(ClientSession var1);

    public <TResult> ListCollectionsIterable<TResult> listCollections(ClientSession var1, Class<TResult> var2);

    public void createCollection(String var1);

    public void createCollection(String var1, CreateCollectionOptions var2);

    public void createCollection(ClientSession var1, String var2);

    public void createCollection(ClientSession var1, String var2, CreateCollectionOptions var3);

    public void createView(String var1, String var2, List<? extends Bson> var3);

    public void createView(String var1, String var2, List<? extends Bson> var3, CreateViewOptions var4);

    public void createView(ClientSession var1, String var2, String var3, List<? extends Bson> var4);

    public void createView(ClientSession var1, String var2, String var3, List<? extends Bson> var4, CreateViewOptions var5);

    public ChangeStreamIterable<Document> watch();

    public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> var1);

    public ChangeStreamIterable<Document> watch(List<? extends Bson> var1);

    public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> var1, Class<TResult> var2);

    public ChangeStreamIterable<Document> watch(ClientSession var1);

    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession var1, Class<TResult> var2);

    public ChangeStreamIterable<Document> watch(ClientSession var1, List<? extends Bson> var2);

    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession var1, List<? extends Bson> var2, Class<TResult> var3);

    public AggregateIterable<Document> aggregate(List<? extends Bson> var1);

    public <TResult> AggregateIterable<TResult> aggregate(List<? extends Bson> var1, Class<TResult> var2);

    public AggregateIterable<Document> aggregate(ClientSession var1, List<? extends Bson> var2);

    public <TResult> AggregateIterable<TResult> aggregate(ClientSession var1, List<? extends Bson> var2, Class<TResult> var3);
}
