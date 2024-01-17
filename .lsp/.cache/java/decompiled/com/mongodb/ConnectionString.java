/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mongodb.AuthenticationMechanism
 *  com.mongodb.ConnectionString$1
 *  com.mongodb.MongoCompressor
 *  com.mongodb.MongoConfigurationException
 *  com.mongodb.MongoCredential
 *  com.mongodb.MongoNamespace
 *  com.mongodb.ReadConcern
 *  com.mongodb.ReadConcernLevel
 *  com.mongodb.ReadPreference
 *  com.mongodb.Tag
 *  com.mongodb.TagSet
 *  com.mongodb.WriteConcern
 *  com.mongodb.diagnostics.logging.Logger
 *  com.mongodb.diagnostics.logging.Loggers
 *  com.mongodb.internal.dns.DefaultDnsResolver
 *  com.mongodb.lang.Nullable
 *  java.io.UnsupportedEncodingException
 *  java.lang.Boolean
 *  java.lang.CharSequence
 *  java.lang.IllegalArgumentException
 *  java.lang.Integer
 *  java.lang.NumberFormatException
 *  java.lang.Object
 *  java.lang.String
 *  java.lang.UnsupportedOperationException
 *  java.net.URLDecoder
 *  java.nio.charset.StandardCharsets
 *  java.util.ArrayList
 *  java.util.Arrays
 *  java.util.Collection
 *  java.util.Collections
 *  java.util.HashMap
 *  java.util.HashSet
 *  java.util.LinkedHashSet
 *  java.util.List
 *  java.util.Map
 *  java.util.Map$Entry
 *  java.util.Objects
 *  java.util.Set
 *  java.util.concurrent.TimeUnit
 *  org.bson.UuidRepresentation
 */
package com.mongodb;

import com.mongodb.AuthenticationMechanism;
import com.mongodb.ConnectionString;
import com.mongodb.MongoCompressor;
import com.mongodb.MongoConfigurationException;
import com.mongodb.MongoCredential;
import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ReadConcernLevel;
import com.mongodb.ReadPreference;
import com.mongodb.Tag;
import com.mongodb.TagSet;
import com.mongodb.WriteConcern;
import com.mongodb.diagnostics.logging.Logger;
import com.mongodb.diagnostics.logging.Loggers;
import com.mongodb.internal.dns.DefaultDnsResolver;
import com.mongodb.lang.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.bson.UuidRepresentation;

public class ConnectionString {
    private static final String MONGODB_PREFIX = "mongodb://";
    private static final String MONGODB_SRV_PREFIX = "mongodb+srv://";
    private static final Set<String> ALLOWED_OPTIONS_IN_TXT_RECORD = new HashSet((Collection)Arrays.asList((Object[])new String[]{"authsource", "replicaset", "loadbalanced"}));
    private static final Logger LOGGER = Loggers.getLogger((String)"uri");
    private final MongoCredential credential;
    private final boolean isSrvProtocol;
    private final List<String> hosts;
    private final String database;
    private final String collection;
    private final String connectionString;
    private Integer srvMaxHosts;
    private String srvServiceName;
    private Boolean directConnection;
    private Boolean loadBalanced;
    private ReadPreference readPreference;
    private WriteConcern writeConcern;
    private Boolean retryWrites;
    private Boolean retryReads;
    private ReadConcern readConcern;
    private Integer minConnectionPoolSize;
    private Integer maxConnectionPoolSize;
    private Integer maxWaitTime;
    private Integer maxConnectionIdleTime;
    private Integer maxConnectionLifeTime;
    private Integer maxConnecting;
    private Integer connectTimeout;
    private Integer socketTimeout;
    private Boolean sslEnabled;
    private Boolean sslInvalidHostnameAllowed;
    private String requiredReplicaSetName;
    private Integer serverSelectionTimeout;
    private Integer localThreshold;
    private Integer heartbeatFrequency;
    private String applicationName;
    private List<MongoCompressor> compressorList;
    private UuidRepresentation uuidRepresentation;
    private static final Set<String> GENERAL_OPTIONS_KEYS = new LinkedHashSet();
    private static final Set<String> AUTH_KEYS = new HashSet();
    private static final Set<String> READ_PREFERENCE_KEYS = new HashSet();
    private static final Set<String> WRITE_CONCERN_KEYS = new HashSet();
    private static final Set<String> COMPRESSOR_KEYS = new HashSet();
    private static final Set<String> ALL_KEYS = new HashSet();
    private static final Set<String> TRUE_VALUES;
    private static final Set<String> FALSE_VALUES;

    public ConnectionString(String connectionString) {
        String nsPart;
        String hostIdentifier;
        String userAndHostInformation;
        this.connectionString = connectionString;
        boolean isMongoDBProtocol = connectionString.startsWith(MONGODB_PREFIX);
        this.isSrvProtocol = connectionString.startsWith(MONGODB_SRV_PREFIX);
        if (!isMongoDBProtocol && !this.isSrvProtocol) {
            throw new IllegalArgumentException(String.format((String)"The connection string is invalid. Connection strings must start with either '%s' or '%s", (Object[])new Object[]{MONGODB_PREFIX, MONGODB_SRV_PREFIX}));
        }
        String unprocessedConnectionString = isMongoDBProtocol ? connectionString.substring(MONGODB_PREFIX.length()) : connectionString.substring(MONGODB_SRV_PREFIX.length());
        int idx = unprocessedConnectionString.indexOf("/");
        if (idx == -1) {
            if (unprocessedConnectionString.contains((CharSequence)"?")) {
                throw new IllegalArgumentException("The connection string contains options without trailing slash");
            }
            userAndHostInformation = unprocessedConnectionString;
            unprocessedConnectionString = "";
        } else {
            userAndHostInformation = unprocessedConnectionString.substring(0, idx);
            unprocessedConnectionString = unprocessedConnectionString.substring(idx + 1);
        }
        String userName = null;
        char[] password = null;
        idx = userAndHostInformation.lastIndexOf("@");
        if (idx > 0) {
            String userInfo = userAndHostInformation.substring(0, idx).replace((CharSequence)"+", (CharSequence)"%2B");
            hostIdentifier = userAndHostInformation.substring(idx + 1);
            int colonCount = this.countOccurrences(userInfo, ":");
            if (userInfo.contains((CharSequence)"@") || colonCount > 1) {
                throw new IllegalArgumentException("The connection string contains invalid user information. If the username or password contains a colon (:) or an at-sign (@) then it must be urlencoded");
            }
            if (colonCount == 0) {
                userName = this.urldecode(userInfo);
            } else {
                idx = userInfo.indexOf(":");
                if (idx == 0) {
                    throw new IllegalArgumentException("No username is provided in the connection string");
                }
                userName = this.urldecode(userInfo.substring(0, idx));
                password = this.urldecode(userInfo.substring(idx + 1), true).toCharArray();
            }
        } else {
            if (idx == 0) {
                throw new IllegalArgumentException("The connection string contains an at-sign (@) without a user name");
            }
            hostIdentifier = userAndHostInformation;
        }
        List unresolvedHosts = Collections.unmodifiableList(this.parseHosts((List<String>)Arrays.asList((Object[])hostIdentifier.split(","))));
        if (this.isSrvProtocol) {
            if (unresolvedHosts.size() > 1) {
                throw new IllegalArgumentException("Only one host allowed when using mongodb+srv protocol");
            }
            if (((String)unresolvedHosts.get(0)).contains((CharSequence)":")) {
                throw new IllegalArgumentException("Host for when using mongodb+srv protocol can not contain a port");
            }
        }
        this.hosts = unresolvedHosts;
        idx = unprocessedConnectionString.indexOf("?");
        if (idx == -1) {
            nsPart = unprocessedConnectionString;
            unprocessedConnectionString = "";
        } else {
            nsPart = unprocessedConnectionString.substring(0, idx);
            unprocessedConnectionString = unprocessedConnectionString.substring(idx + 1);
        }
        if (nsPart.length() > 0) {
            idx = (nsPart = this.urldecode(nsPart)).indexOf(".");
            if (idx < 0) {
                this.database = nsPart;
                this.collection = null;
            } else {
                this.database = nsPart.substring(0, idx);
                this.collection = nsPart.substring(idx + 1);
            }
            MongoNamespace.checkDatabaseNameValidity((String)this.database);
        } else {
            this.database = null;
            this.collection = null;
        }
        String txtRecordsQueryParameters = this.isSrvProtocol ? new DefaultDnsResolver().resolveAdditionalQueryParametersFromTxtRecords((String)unresolvedHosts.get(0)) : "";
        String connectionStringQueryParamenters = unprocessedConnectionString;
        Map<String, List<String>> connectionStringOptionsMap = this.parseOptions(connectionStringQueryParamenters);
        Map<String, List<String>> txtRecordsOptionsMap = this.parseOptions(txtRecordsQueryParameters);
        if (!ALLOWED_OPTIONS_IN_TXT_RECORD.containsAll((Collection)txtRecordsOptionsMap.keySet())) {
            throw new MongoConfigurationException(String.format((String)"A TXT record is only permitted to contain the keys %s, but the TXT record for '%s' contains the keys %s", (Object[])new Object[]{ALLOWED_OPTIONS_IN_TXT_RECORD, unresolvedHosts.get(0), txtRecordsOptionsMap.keySet()}));
        }
        Map<String, List<String>> combinedOptionsMaps = this.combineOptionsMaps(txtRecordsOptionsMap, connectionStringOptionsMap);
        if (this.isSrvProtocol && !combinedOptionsMaps.containsKey((Object)"ssl")) {
            combinedOptionsMaps.put((Object)"ssl", (Object)Collections.singletonList((Object)"true"));
        }
        this.translateOptions(combinedOptionsMaps);
        if (!this.isSrvProtocol && this.srvMaxHosts != null) {
            throw new IllegalArgumentException("srvMaxHosts can only be specified with mongodb+srv protocol");
        }
        if (!this.isSrvProtocol && this.srvServiceName != null) {
            throw new IllegalArgumentException("srvServiceName can only be specified with mongodb+srv protocol");
        }
        if (this.directConnection != null && this.directConnection.booleanValue()) {
            if (this.isSrvProtocol) {
                throw new IllegalArgumentException("Direct connections are not supported when using mongodb+srv protocol");
            }
            if (this.hosts.size() > 1) {
                throw new IllegalArgumentException("Direct connections are not supported when using multiple hosts");
            }
        }
        if (this.loadBalanced != null && this.loadBalanced.booleanValue()) {
            if (this.directConnection != null && this.directConnection.booleanValue()) {
                throw new IllegalArgumentException("directConnection=true can not be specified with loadBalanced=true");
            }
            if (this.requiredReplicaSetName != null) {
                throw new IllegalArgumentException("replicaSet can not be specified with loadBalanced=true");
            }
            if (this.hosts.size() > 1) {
                throw new IllegalArgumentException("Only one host can be specified with loadBalanced=true");
            }
            if (this.srvMaxHosts != null && this.srvMaxHosts > 0) {
                throw new IllegalArgumentException("srvMaxHosts can not be specified with loadBalanced=true");
            }
        }
        if (this.requiredReplicaSetName != null && this.srvMaxHosts != null && this.srvMaxHosts > 0) {
            throw new IllegalArgumentException("srvMaxHosts can not be specified with replica set name");
        }
        this.credential = this.createCredentials(combinedOptionsMaps, userName, password);
        this.warnOnUnsupportedOptions(combinedOptionsMaps);
    }

    private Map<String, List<String>> combineOptionsMaps(Map<String, List<String>> txtRecordsOptionsMap, Map<String, List<String>> connectionStringOptionsMap) {
        HashMap combinedOptionsMaps = new HashMap(txtRecordsOptionsMap);
        for (Map.Entry entry : connectionStringOptionsMap.entrySet()) {
            combinedOptionsMaps.put((Object)((String)entry.getKey()), (Object)((List)entry.getValue()));
        }
        return combinedOptionsMaps;
    }

    private void warnOnUnsupportedOptions(Map<String, List<String>> optionsMap) {
        for (String key : optionsMap.keySet()) {
            if (ALL_KEYS.contains((Object)key) || !LOGGER.isWarnEnabled()) continue;
            LOGGER.warn(String.format((String)"Connection string contains unsupported option '%s'.", (Object[])new Object[]{key}));
        }
    }

    private void translateOptions(Map<String, List<String>> optionsMap) {
        boolean tlsInsecureSet = false;
        boolean tlsAllowInvalidHostnamesSet = false;
        for (String key : GENERAL_OPTIONS_KEYS) {
            String value = this.getLastValue(optionsMap, key);
            if (value == null) continue;
            if (key.equals((Object)"maxpoolsize")) {
                this.maxConnectionPoolSize = this.parseInteger(value, "maxpoolsize");
                continue;
            }
            if (key.equals((Object)"minpoolsize")) {
                this.minConnectionPoolSize = this.parseInteger(value, "minpoolsize");
                continue;
            }
            if (key.equals((Object)"maxidletimems")) {
                this.maxConnectionIdleTime = this.parseInteger(value, "maxidletimems");
                continue;
            }
            if (key.equals((Object)"maxlifetimems")) {
                this.maxConnectionLifeTime = this.parseInteger(value, "maxlifetimems");
                continue;
            }
            if (key.equals((Object)"maxconnecting")) {
                this.maxConnecting = this.parseInteger(value, "maxConnecting");
                continue;
            }
            if (key.equals((Object)"waitqueuetimeoutms")) {
                this.maxWaitTime = this.parseInteger(value, "waitqueuetimeoutms");
                continue;
            }
            if (key.equals((Object)"connecttimeoutms")) {
                this.connectTimeout = this.parseInteger(value, "connecttimeoutms");
                continue;
            }
            if (key.equals((Object)"sockettimeoutms")) {
                this.socketTimeout = this.parseInteger(value, "sockettimeoutms");
                continue;
            }
            if (key.equals((Object)"tlsallowinvalidhostnames")) {
                this.sslInvalidHostnameAllowed = this.parseBoolean(value, "tlsAllowInvalidHostnames");
                tlsAllowInvalidHostnamesSet = true;
                continue;
            }
            if (key.equals((Object)"sslinvalidhostnameallowed")) {
                this.sslInvalidHostnameAllowed = this.parseBoolean(value, "sslinvalidhostnameallowed");
                tlsAllowInvalidHostnamesSet = true;
                continue;
            }
            if (key.equals((Object)"tlsinsecure")) {
                this.sslInvalidHostnameAllowed = this.parseBoolean(value, "tlsinsecure");
                tlsInsecureSet = true;
                continue;
            }
            if (key.equals((Object)"ssl")) {
                this.initializeSslEnabled("ssl", value);
                continue;
            }
            if (key.equals((Object)"tls")) {
                this.initializeSslEnabled("tls", value);
                continue;
            }
            if (key.equals((Object)"replicaset")) {
                this.requiredReplicaSetName = value;
                continue;
            }
            if (key.equals((Object)"readconcernlevel")) {
                this.readConcern = new ReadConcern(ReadConcernLevel.fromString((String)value));
                continue;
            }
            if (key.equals((Object)"serverselectiontimeoutms")) {
                this.serverSelectionTimeout = this.parseInteger(value, "serverselectiontimeoutms");
                continue;
            }
            if (key.equals((Object)"localthresholdms")) {
                this.localThreshold = this.parseInteger(value, "localthresholdms");
                continue;
            }
            if (key.equals((Object)"heartbeatfrequencyms")) {
                this.heartbeatFrequency = this.parseInteger(value, "heartbeatfrequencyms");
                continue;
            }
            if (key.equals((Object)"appname")) {
                this.applicationName = value;
                continue;
            }
            if (key.equals((Object)"retrywrites")) {
                this.retryWrites = this.parseBoolean(value, "retrywrites");
                continue;
            }
            if (key.equals((Object)"retryreads")) {
                this.retryReads = this.parseBoolean(value, "retryreads");
                continue;
            }
            if (key.equals((Object)"uuidrepresentation")) {
                this.uuidRepresentation = this.createUuidRepresentation(value);
                continue;
            }
            if (key.equals((Object)"directconnection")) {
                this.directConnection = this.parseBoolean(value, "directconnection");
                continue;
            }
            if (key.equals((Object)"loadbalanced")) {
                this.loadBalanced = this.parseBoolean(value, "loadbalanced");
                continue;
            }
            if (key.equals((Object)"srvmaxhosts")) {
                this.srvMaxHosts = this.parseInteger(value, "srvmaxhosts");
                if (this.srvMaxHosts >= 0) continue;
                throw new IllegalArgumentException("srvMaxHosts must be >= 0");
            }
            if (!key.equals((Object)"srvservicename")) continue;
            this.srvServiceName = value;
        }
        if (tlsInsecureSet && tlsAllowInvalidHostnamesSet) {
            throw new IllegalArgumentException("tlsAllowInvalidHostnames or sslInvalidHostnameAllowed set along with tlsInsecure is not allowed");
        }
        this.writeConcern = this.createWriteConcern(optionsMap);
        this.readPreference = this.createReadPreference(optionsMap);
        this.compressorList = this.createCompressors(optionsMap);
    }

    private void initializeSslEnabled(String key, String value) {
        Boolean booleanValue = this.parseBoolean(value, key);
        if (this.sslEnabled != null && !this.sslEnabled.equals((Object)booleanValue)) {
            throw new IllegalArgumentException("Conflicting tls and ssl parameter values are not allowed");
        }
        this.sslEnabled = booleanValue;
    }

    private List<MongoCompressor> createCompressors(Map<String, List<String>> optionsMap) {
        String compressors = "";
        Integer zlibCompressionLevel = null;
        for (String key : COMPRESSOR_KEYS) {
            String value = this.getLastValue(optionsMap, key);
            if (value == null) continue;
            if (key.equals((Object)"compressors")) {
                compressors = value;
                continue;
            }
            if (!key.equals((Object)"zlibcompressionlevel")) continue;
            zlibCompressionLevel = Integer.parseInt((String)value);
        }
        return this.buildCompressors(compressors, zlibCompressionLevel);
    }

    private List<MongoCompressor> buildCompressors(String compressors, @Nullable Integer zlibCompressionLevel) {
        ArrayList compressorsList = new ArrayList();
        for (String cur : compressors.split(",")) {
            if (cur.equals((Object)"zlib")) {
                MongoCompressor zlibCompressor = MongoCompressor.createZlibCompressor();
                if (zlibCompressionLevel != null) {
                    zlibCompressor = zlibCompressor.withProperty("LEVEL", (Object)zlibCompressionLevel);
                }
                compressorsList.add((Object)zlibCompressor);
                continue;
            }
            if (cur.equals((Object)"snappy")) {
                compressorsList.add((Object)MongoCompressor.createSnappyCompressor());
                continue;
            }
            if (cur.equals((Object)"zstd")) {
                compressorsList.add((Object)MongoCompressor.createZstdCompressor());
                continue;
            }
            if (cur.isEmpty()) continue;
            throw new IllegalArgumentException("Unsupported compressor '" + cur + "'");
        }
        return Collections.unmodifiableList((List)compressorsList);
    }

    @Nullable
    private WriteConcern createWriteConcern(Map<String, List<String>> optionsMap) {
        String w = null;
        Integer wTimeout = null;
        Boolean safe = null;
        Boolean journal = null;
        for (String key : WRITE_CONCERN_KEYS) {
            String value = this.getLastValue(optionsMap, key);
            if (value == null) continue;
            if (key.equals((Object)"safe")) {
                safe = this.parseBoolean(value, "safe");
                continue;
            }
            if (key.equals((Object)"w")) {
                w = value;
                continue;
            }
            if (key.equals((Object)"wtimeoutms")) {
                wTimeout = Integer.parseInt((String)value);
                continue;
            }
            if (!key.equals((Object)"journal")) continue;
            journal = this.parseBoolean(value, "journal");
        }
        return this.buildWriteConcern(safe, w, wTimeout, journal);
    }

    @Nullable
    private ReadPreference createReadPreference(Map<String, List<String>> optionsMap) {
        String readPreferenceType = null;
        ArrayList tagSetList = new ArrayList();
        long maxStalenessSeconds = -1L;
        for (String key : READ_PREFERENCE_KEYS) {
            String value = this.getLastValue(optionsMap, key);
            if (value == null) continue;
            if (key.equals((Object)"readpreference")) {
                readPreferenceType = value;
                continue;
            }
            if (key.equals((Object)"maxstalenessseconds")) {
                maxStalenessSeconds = this.parseInteger(value, "maxstalenessseconds");
                continue;
            }
            if (!key.equals((Object)"readpreferencetags")) continue;
            for (String cur : (List)optionsMap.get((Object)key)) {
                TagSet tagSet = this.getTags(cur.trim());
                tagSetList.add((Object)tagSet);
            }
        }
        return this.buildReadPreference(readPreferenceType, (List<TagSet>)tagSetList, maxStalenessSeconds);
    }

    private UuidRepresentation createUuidRepresentation(String value) {
        if (value.equalsIgnoreCase("unspecified")) {
            return UuidRepresentation.UNSPECIFIED;
        }
        if (value.equalsIgnoreCase("javaLegacy")) {
            return UuidRepresentation.JAVA_LEGACY;
        }
        if (value.equalsIgnoreCase("csharpLegacy")) {
            return UuidRepresentation.C_SHARP_LEGACY;
        }
        if (value.equalsIgnoreCase("pythonLegacy")) {
            return UuidRepresentation.PYTHON_LEGACY;
        }
        if (value.equalsIgnoreCase("standard")) {
            return UuidRepresentation.STANDARD;
        }
        throw new IllegalArgumentException("Unknown uuid representation: " + value);
    }

    @Nullable
    private MongoCredential createCredentials(Map<String, List<String>> optionsMap, @Nullable String userName, @Nullable char[] password) {
        AuthenticationMechanism mechanism = null;
        String authSource = null;
        String gssapiServiceName = null;
        String authMechanismProperties = null;
        for (String key : AUTH_KEYS) {
            String value = this.getLastValue(optionsMap, key);
            if (value == null) continue;
            if (key.equals((Object)"authmechanism")) {
                if (value.equals((Object)"MONGODB-CR")) {
                    if (userName == null) {
                        throw new IllegalArgumentException("username can not be null");
                    }
                    LOGGER.warn("Deprecated MONGDOB-CR authentication mechanism used in connection string");
                    continue;
                }
                mechanism = AuthenticationMechanism.fromMechanismName((String)value);
                continue;
            }
            if (key.equals((Object)"authsource")) {
                if (value.equals((Object)"")) {
                    throw new IllegalArgumentException("authSource can not be an empty string");
                }
                authSource = value;
                continue;
            }
            if (key.equals((Object)"gssapiservicename")) {
                gssapiServiceName = value;
                continue;
            }
            if (!key.equals((Object)"authmechanismproperties")) continue;
            authMechanismProperties = value;
        }
        MongoCredential credential = null;
        if (mechanism != null) {
            credential = this.createMongoCredentialWithMechanism(mechanism, userName, password, authSource, gssapiServiceName);
        } else if (userName != null) {
            credential = MongoCredential.createCredential((String)userName, (String)this.getAuthSourceOrDefault(authSource, this.database != null ? this.database : "admin"), (char[])password);
        }
        if (credential != null && authMechanismProperties != null) {
            for (String part : authMechanismProperties.split(",")) {
                String[] mechanismPropertyKeyValue = part.split(":");
                if (mechanismPropertyKeyValue.length != 2) {
                    throw new IllegalArgumentException(String.format((String)"The connection string contains invalid authentication properties. '%s' is not a key value pair", (Object[])new Object[]{part}));
                }
                String key = mechanismPropertyKeyValue[0].trim().toLowerCase();
                String value = mechanismPropertyKeyValue[1].trim();
                credential = key.equals((Object)"canonicalize_host_name") ? credential.withMechanismProperty(key, (Object)Boolean.valueOf((String)value)) : credential.withMechanismProperty(key, (Object)value);
            }
        }
        return credential;
    }

    private MongoCredential createMongoCredentialWithMechanism(AuthenticationMechanism mechanism, String userName, @Nullable char[] password, @Nullable String authSource, @Nullable String gssapiServiceName) {
        MongoCredential credential;
        String mechanismAuthSource;
        switch (1.$SwitchMap$com$mongodb$AuthenticationMechanism[mechanism.ordinal()]) {
            case 1: {
                mechanismAuthSource = this.getAuthSourceOrDefault(authSource, this.database != null ? this.database : "$external");
                break;
            }
            case 2: 
            case 3: {
                mechanismAuthSource = this.getAuthSourceOrDefault(authSource, "$external");
                if (mechanismAuthSource.equals((Object)"$external")) break;
                throw new IllegalArgumentException(String.format((String)"Invalid authSource for %s, it must be '$external'", (Object[])new Object[]{mechanism}));
            }
            default: {
                mechanismAuthSource = this.getAuthSourceOrDefault(authSource, this.database != null ? this.database : "admin");
            }
        }
        switch (1.$SwitchMap$com$mongodb$AuthenticationMechanism[mechanism.ordinal()]) {
            case 2: {
                credential = MongoCredential.createGSSAPICredential((String)userName);
                if (gssapiServiceName != null) {
                    credential = credential.withMechanismProperty("SERVICE_NAME", (Object)gssapiServiceName);
                }
                if (password == null || !LOGGER.isWarnEnabled()) break;
                LOGGER.warn("Password in connection string not used with MONGODB_X509 authentication mechanism.");
                break;
            }
            case 1: {
                credential = MongoCredential.createPlainCredential((String)userName, (String)mechanismAuthSource, (char[])password);
                break;
            }
            case 3: {
                if (password != null) {
                    throw new IllegalArgumentException("Invalid mechanism, MONGODB_x509 does not support passwords");
                }
                credential = MongoCredential.createMongoX509Credential((String)userName);
                break;
            }
            case 4: {
                credential = MongoCredential.createScramSha1Credential((String)userName, (String)mechanismAuthSource, (char[])password);
                break;
            }
            case 5: {
                credential = MongoCredential.createScramSha256Credential((String)userName, (String)mechanismAuthSource, (char[])password);
                break;
            }
            case 6: {
                credential = MongoCredential.createAwsCredential((String)userName, (char[])password);
                break;
            }
            default: {
                throw new UnsupportedOperationException(String.format((String)"The connection string contains an invalid authentication mechanism'. '%s' is not a supported authentication mechanism", (Object[])new Object[]{mechanism}));
            }
        }
        return credential;
    }

    private String getAuthSourceOrDefault(@Nullable String authSource, String defaultAuthSource) {
        if (authSource != null) {
            return authSource;
        }
        return defaultAuthSource;
    }

    @Nullable
    private String getLastValue(Map<String, List<String>> optionsMap, String key) {
        List valueList = (List)optionsMap.get((Object)key);
        if (valueList == null) {
            return null;
        }
        return (String)valueList.get(valueList.size() - 1);
    }

    private Map<String, List<String>> parseOptions(String optionsPart) {
        String legacySecondaryOkOption;
        String legacySecondaryOk;
        HashMap optionsMap = new HashMap();
        if (optionsPart.length() == 0) {
            return optionsMap;
        }
        for (String part : optionsPart.split("&|;")) {
            if (part.length() == 0) continue;
            int idx = part.indexOf("=");
            if (idx >= 0) {
                String key = part.substring(0, idx).toLowerCase();
                String value = part.substring(idx + 1);
                List valueList = (List)optionsMap.get((Object)key);
                if (valueList == null) {
                    valueList = new ArrayList(1);
                }
                valueList.add((Object)this.urldecode(value));
                optionsMap.put((Object)key, (Object)valueList);
                continue;
            }
            throw new IllegalArgumentException(String.format((String)"The connection string contains an invalid option '%s'. '%s' is missing the value delimiter eg '%s=value'", (Object[])new Object[]{optionsPart, part, part}));
        }
        if (optionsMap.containsKey((Object)"wtimeout") && !optionsMap.containsKey((Object)"wtimeoutms")) {
            optionsMap.put((Object)"wtimeoutms", (Object)((List)optionsMap.remove((Object)"wtimeout")));
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Uri option 'wtimeout' has been deprecated, use 'wtimeoutms' instead.");
            }
        }
        if ((legacySecondaryOk = this.getLastValue((Map<String, List<String>>)optionsMap, legacySecondaryOkOption = "slaveok")) != null && !optionsMap.containsKey((Object)"readpreference")) {
            String readPreference = Boolean.TRUE.equals((Object)this.parseBoolean(legacySecondaryOk, legacySecondaryOkOption)) ? "secondaryPreferred" : "primary";
            optionsMap.put((Object)"readpreference", (Object)Collections.singletonList((Object)readPreference));
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(String.format((String)"Uri option '%s' has been deprecated, use 'readpreference' instead.", (Object[])new Object[]{legacySecondaryOkOption}));
            }
        }
        if (optionsMap.containsKey((Object)"j") && !optionsMap.containsKey((Object)"journal")) {
            optionsMap.put((Object)"journal", (Object)((List)optionsMap.remove((Object)"j")));
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Uri option 'j' has been deprecated, use 'journal' instead.");
            }
        }
        return optionsMap;
    }

    @Nullable
    private ReadPreference buildReadPreference(@Nullable String readPreferenceType, List<TagSet> tagSetList, long maxStalenessSeconds) {
        if (readPreferenceType != null) {
            if (tagSetList.isEmpty() && maxStalenessSeconds == -1L) {
                return ReadPreference.valueOf((String)readPreferenceType);
            }
            if (maxStalenessSeconds == -1L) {
                return ReadPreference.valueOf((String)readPreferenceType, tagSetList);
            }
            return ReadPreference.valueOf((String)readPreferenceType, tagSetList, (long)maxStalenessSeconds, (TimeUnit)TimeUnit.SECONDS);
        }
        if (!tagSetList.isEmpty() || maxStalenessSeconds != -1L) {
            throw new IllegalArgumentException("Read preference mode must be specified if either read preference tags or max staleness is specified");
        }
        return null;
    }

    @Nullable
    private WriteConcern buildWriteConcern(@Nullable Boolean safe, @Nullable String w, @Nullable Integer wTimeout, @Nullable Boolean journal) {
        WriteConcern retVal = null;
        if (w != null || wTimeout != null || journal != null) {
            if (w == null) {
                retVal = WriteConcern.ACKNOWLEDGED;
            } else {
                try {
                    retVal = new WriteConcern(Integer.parseInt((String)w));
                }
                catch (NumberFormatException e) {
                    retVal = new WriteConcern(w);
                }
            }
            if (wTimeout != null) {
                retVal = retVal.withWTimeout((long)wTimeout.intValue(), TimeUnit.MILLISECONDS);
            }
            if (journal != null) {
                retVal = retVal.withJournal(journal);
            }
            return retVal;
        }
        if (safe != null) {
            retVal = safe != false ? WriteConcern.ACKNOWLEDGED : WriteConcern.UNACKNOWLEDGED;
        }
        return retVal;
    }

    private TagSet getTags(String tagSetString) {
        ArrayList tagList = new ArrayList();
        if (tagSetString.length() > 0) {
            for (String tag : tagSetString.split(",")) {
                String[] tagKeyValuePair = tag.split(":");
                if (tagKeyValuePair.length != 2) {
                    throw new IllegalArgumentException(String.format((String)"The connection string contains an invalid read preference tag. '%s' is not a key value pair", (Object[])new Object[]{tagSetString}));
                }
                tagList.add((Object)new Tag(tagKeyValuePair[0].trim(), tagKeyValuePair[1].trim()));
            }
        }
        return new TagSet((List)tagList);
    }

    @Nullable
    private Boolean parseBoolean(String input, String key) {
        String trimmedInput = input.trim().toLowerCase();
        if (TRUE_VALUES.contains((Object)trimmedInput)) {
            if (!trimmedInput.equals((Object)"true")) {
                LOGGER.warn(String.format((String)"Deprecated boolean value '%s' in the connection string for '%s'. Replace with 'true'", (Object[])new Object[]{trimmedInput, key}));
            }
            return true;
        }
        if (FALSE_VALUES.contains((Object)trimmedInput)) {
            if (!trimmedInput.equals((Object)"false")) {
                LOGGER.warn(String.format((String)"Deprecated boolean value '%s' in the connection string for '%s'. Replace with'false'", (Object[])new Object[]{trimmedInput, key}));
            }
            return false;
        }
        LOGGER.warn(String.format((String)"Ignoring unrecognized boolean value '%s' in the connection string for '%s'. Replace with either 'true' or 'false'", (Object[])new Object[]{trimmedInput, key}));
        return null;
    }

    private int parseInteger(String input, String key) {
        try {
            return Integer.parseInt((String)input);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format((String)"The connection string contains an invalid value for '%s'. '%s' is not a valid integer", (Object[])new Object[]{key, input}));
        }
    }

    private List<String> parseHosts(List<String> rawHosts) {
        if (rawHosts.size() == 0) {
            throw new IllegalArgumentException("The connection string must contain at least one host");
        }
        ArrayList hosts = new ArrayList();
        for (String host : rawHosts) {
            if (host.length() == 0) {
                throw new IllegalArgumentException(String.format((String)"The connection string contains an empty host '%s'. ", (Object[])new Object[]{rawHosts}));
            }
            if (host.endsWith(".sock")) {
                host = this.urldecode(host);
            } else if (host.startsWith("[")) {
                if (!host.contains((CharSequence)"]")) {
                    throw new IllegalArgumentException(String.format((String)"The connection string contains an invalid host '%s'. IPv6 address literals must be enclosed in '[' and ']' according to RFC 2732", (Object[])new Object[]{host}));
                }
                int idx = host.indexOf("]:");
                if (idx != -1) {
                    this.validatePort(host, host.substring(idx + 2));
                }
            } else {
                int colonCount = this.countOccurrences(host, ":");
                if (colonCount > 1) {
                    throw new IllegalArgumentException(String.format((String)"The connection string contains an invalid host '%s'. Reserved characters such as ':' must be escaped according RFC 2396. Any IPv6 address literal must be enclosed in '[' and ']' according to RFC 2732.", (Object[])new Object[]{host}));
                }
                if (colonCount == 1) {
                    this.validatePort(host, host.substring(host.indexOf(":") + 1));
                }
            }
            hosts.add((Object)host);
        }
        Collections.sort((List)hosts);
        return hosts;
    }

    private void validatePort(String host, String port) {
        boolean invalidPort = false;
        try {
            int portInt = Integer.parseInt((String)port);
            if (portInt <= 0 || portInt > 65535) {
                invalidPort = true;
            }
        }
        catch (NumberFormatException e) {
            invalidPort = true;
        }
        if (invalidPort) {
            throw new IllegalArgumentException(String.format((String)"The connection string contains an invalid host '%s'. The port '%s' is not a valid, it must be an integer between 0 and 65535", (Object[])new Object[]{host, port}));
        }
    }

    private int countOccurrences(String haystack, String needle) {
        return haystack.length() - haystack.replace((CharSequence)needle, (CharSequence)"").length();
    }

    private String urldecode(String input) {
        return this.urldecode(input, false);
    }

    private String urldecode(String input, boolean password) {
        try {
            return URLDecoder.decode((String)input, (String)StandardCharsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException e) {
            if (password) {
                throw new IllegalArgumentException("The connection string contained unsupported characters in the password.");
            }
            throw new IllegalArgumentException(String.format((String)"The connection string contained unsupported characters: '%s'.Decoding produced the following error: %s", (Object[])new Object[]{input, e.getMessage()}));
        }
    }

    @Nullable
    public String getUsername() {
        return this.credential != null ? this.credential.getUserName() : null;
    }

    @Nullable
    public char[] getPassword() {
        return this.credential != null ? this.credential.getPassword() : null;
    }

    public boolean isSrvProtocol() {
        return this.isSrvProtocol;
    }

    @Nullable
    public Integer getSrvMaxHosts() {
        return this.srvMaxHosts;
    }

    @Nullable
    public String getSrvServiceName() {
        return this.srvServiceName;
    }

    public List<String> getHosts() {
        return this.hosts;
    }

    @Nullable
    public String getDatabase() {
        return this.database;
    }

    @Nullable
    public String getCollection() {
        return this.collection;
    }

    @Nullable
    public Boolean isDirectConnection() {
        return this.directConnection;
    }

    @Nullable
    public Boolean isLoadBalanced() {
        return this.loadBalanced;
    }

    public String getConnectionString() {
        return this.connectionString;
    }

    @Nullable
    public MongoCredential getCredential() {
        return this.credential;
    }

    @Nullable
    public ReadPreference getReadPreference() {
        return this.readPreference;
    }

    @Nullable
    public ReadConcern getReadConcern() {
        return this.readConcern;
    }

    @Nullable
    public WriteConcern getWriteConcern() {
        return this.writeConcern;
    }

    @Nullable
    public Boolean getRetryWritesValue() {
        return this.retryWrites;
    }

    @Nullable
    public Boolean getRetryReads() {
        return this.retryReads;
    }

    @Nullable
    public Integer getMinConnectionPoolSize() {
        return this.minConnectionPoolSize;
    }

    @Nullable
    public Integer getMaxConnectionPoolSize() {
        return this.maxConnectionPoolSize;
    }

    @Nullable
    public Integer getMaxWaitTime() {
        return this.maxWaitTime;
    }

    @Nullable
    public Integer getMaxConnectionIdleTime() {
        return this.maxConnectionIdleTime;
    }

    @Nullable
    public Integer getMaxConnectionLifeTime() {
        return this.maxConnectionLifeTime;
    }

    @Nullable
    public Integer getMaxConnecting() {
        return this.maxConnecting;
    }

    @Nullable
    public Integer getConnectTimeout() {
        return this.connectTimeout;
    }

    @Nullable
    public Integer getSocketTimeout() {
        return this.socketTimeout;
    }

    @Nullable
    public Boolean getSslEnabled() {
        return this.sslEnabled;
    }

    @Nullable
    public Boolean getSslInvalidHostnameAllowed() {
        return this.sslInvalidHostnameAllowed;
    }

    @Nullable
    public String getRequiredReplicaSetName() {
        return this.requiredReplicaSetName;
    }

    @Nullable
    public Integer getServerSelectionTimeout() {
        return this.serverSelectionTimeout;
    }

    @Nullable
    public Integer getLocalThreshold() {
        return this.localThreshold;
    }

    @Nullable
    public Integer getHeartbeatFrequency() {
        return this.heartbeatFrequency;
    }

    @Nullable
    public String getApplicationName() {
        return this.applicationName;
    }

    public List<MongoCompressor> getCompressorList() {
        return this.compressorList;
    }

    @Nullable
    public UuidRepresentation getUuidRepresentation() {
        return this.uuidRepresentation;
    }

    public String toString() {
        return this.connectionString;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ConnectionString that = (ConnectionString)o;
        return this.isSrvProtocol == that.isSrvProtocol && Objects.equals((Object)this.directConnection, (Object)that.directConnection) && Objects.equals((Object)this.credential, (Object)that.credential) && Objects.equals(this.hosts, that.hosts) && Objects.equals((Object)this.database, (Object)that.database) && Objects.equals((Object)this.collection, (Object)that.collection) && Objects.equals((Object)this.readPreference, (Object)that.readPreference) && Objects.equals((Object)this.writeConcern, (Object)that.writeConcern) && Objects.equals((Object)this.retryWrites, (Object)that.retryWrites) && Objects.equals((Object)this.retryReads, (Object)that.retryReads) && Objects.equals((Object)this.readConcern, (Object)that.readConcern) && Objects.equals((Object)this.minConnectionPoolSize, (Object)that.minConnectionPoolSize) && Objects.equals((Object)this.maxConnectionPoolSize, (Object)that.maxConnectionPoolSize) && Objects.equals((Object)this.maxWaitTime, (Object)that.maxWaitTime) && Objects.equals((Object)this.maxConnectionIdleTime, (Object)that.maxConnectionIdleTime) && Objects.equals((Object)this.maxConnectionLifeTime, (Object)that.maxConnectionLifeTime) && Objects.equals((Object)this.maxConnecting, (Object)that.maxConnecting) && Objects.equals((Object)this.connectTimeout, (Object)that.connectTimeout) && Objects.equals((Object)this.socketTimeout, (Object)that.socketTimeout) && Objects.equals((Object)this.sslEnabled, (Object)that.sslEnabled) && Objects.equals((Object)this.sslInvalidHostnameAllowed, (Object)that.sslInvalidHostnameAllowed) && Objects.equals((Object)this.requiredReplicaSetName, (Object)that.requiredReplicaSetName) && Objects.equals((Object)this.serverSelectionTimeout, (Object)that.serverSelectionTimeout) && Objects.equals((Object)this.localThreshold, (Object)that.localThreshold) && Objects.equals((Object)this.heartbeatFrequency, (Object)that.heartbeatFrequency) && Objects.equals((Object)this.applicationName, (Object)that.applicationName) && Objects.equals(this.compressorList, that.compressorList) && Objects.equals((Object)this.uuidRepresentation, (Object)that.uuidRepresentation) && Objects.equals((Object)this.srvServiceName, (Object)that.srvServiceName) && Objects.equals((Object)this.srvMaxHosts, (Object)that.srvMaxHosts);
    }

    public int hashCode() {
        return Objects.hash((Object[])new Object[]{this.credential, this.isSrvProtocol, this.hosts, this.database, this.collection, this.directConnection, this.readPreference, this.writeConcern, this.retryWrites, this.retryReads, this.readConcern, this.minConnectionPoolSize, this.maxConnectionPoolSize, this.maxWaitTime, this.maxConnectionIdleTime, this.maxConnectionLifeTime, this.maxConnecting, this.connectTimeout, this.socketTimeout, this.sslEnabled, this.sslInvalidHostnameAllowed, this.requiredReplicaSetName, this.serverSelectionTimeout, this.localThreshold, this.heartbeatFrequency, this.applicationName, this.compressorList, this.uuidRepresentation, this.srvServiceName, this.srvMaxHosts});
    }

    static {
        GENERAL_OPTIONS_KEYS.add((Object)"minpoolsize");
        GENERAL_OPTIONS_KEYS.add((Object)"maxpoolsize");
        GENERAL_OPTIONS_KEYS.add((Object)"waitqueuetimeoutms");
        GENERAL_OPTIONS_KEYS.add((Object)"connecttimeoutms");
        GENERAL_OPTIONS_KEYS.add((Object)"maxidletimems");
        GENERAL_OPTIONS_KEYS.add((Object)"maxlifetimems");
        GENERAL_OPTIONS_KEYS.add((Object)"maxconnecting");
        GENERAL_OPTIONS_KEYS.add((Object)"sockettimeoutms");
        GENERAL_OPTIONS_KEYS.add((Object)"ssl");
        GENERAL_OPTIONS_KEYS.add((Object)"tls");
        GENERAL_OPTIONS_KEYS.add((Object)"tlsinsecure");
        GENERAL_OPTIONS_KEYS.add((Object)"sslinvalidhostnameallowed");
        GENERAL_OPTIONS_KEYS.add((Object)"tlsallowinvalidhostnames");
        GENERAL_OPTIONS_KEYS.add((Object)"replicaset");
        GENERAL_OPTIONS_KEYS.add((Object)"readconcernlevel");
        GENERAL_OPTIONS_KEYS.add((Object)"serverselectiontimeoutms");
        GENERAL_OPTIONS_KEYS.add((Object)"localthresholdms");
        GENERAL_OPTIONS_KEYS.add((Object)"heartbeatfrequencyms");
        GENERAL_OPTIONS_KEYS.add((Object)"retrywrites");
        GENERAL_OPTIONS_KEYS.add((Object)"retryreads");
        GENERAL_OPTIONS_KEYS.add((Object)"appname");
        GENERAL_OPTIONS_KEYS.add((Object)"uuidrepresentation");
        GENERAL_OPTIONS_KEYS.add((Object)"directconnection");
        GENERAL_OPTIONS_KEYS.add((Object)"loadbalanced");
        GENERAL_OPTIONS_KEYS.add((Object)"srvmaxhosts");
        GENERAL_OPTIONS_KEYS.add((Object)"srvservicename");
        COMPRESSOR_KEYS.add((Object)"compressors");
        COMPRESSOR_KEYS.add((Object)"zlibcompressionlevel");
        READ_PREFERENCE_KEYS.add((Object)"readpreference");
        READ_PREFERENCE_KEYS.add((Object)"readpreferencetags");
        READ_PREFERENCE_KEYS.add((Object)"maxstalenessseconds");
        WRITE_CONCERN_KEYS.add((Object)"safe");
        WRITE_CONCERN_KEYS.add((Object)"w");
        WRITE_CONCERN_KEYS.add((Object)"wtimeoutms");
        WRITE_CONCERN_KEYS.add((Object)"journal");
        AUTH_KEYS.add((Object)"authmechanism");
        AUTH_KEYS.add((Object)"authsource");
        AUTH_KEYS.add((Object)"gssapiservicename");
        AUTH_KEYS.add((Object)"authmechanismproperties");
        ALL_KEYS.addAll(GENERAL_OPTIONS_KEYS);
        ALL_KEYS.addAll(AUTH_KEYS);
        ALL_KEYS.addAll(READ_PREFERENCE_KEYS);
        ALL_KEYS.addAll(WRITE_CONCERN_KEYS);
        ALL_KEYS.addAll(COMPRESSOR_KEYS);
        TRUE_VALUES = new HashSet((Collection)Arrays.asList((Object[])new String[]{"true", "yes", "1"}));
        FALSE_VALUES = new HashSet((Collection)Arrays.asList((Object[])new String[]{"false", "no", "0"}));
    }
}
