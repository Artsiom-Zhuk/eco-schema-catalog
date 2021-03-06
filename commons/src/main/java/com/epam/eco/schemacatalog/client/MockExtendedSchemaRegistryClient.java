/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.eco.schemacatalog.client;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.avro.Schema;
import org.apache.commons.lang3.Validate;

import com.epam.eco.commons.avro.modification.CachedSchemaModifications;
import com.epam.eco.commons.avro.modification.SchemaModification;
import com.epam.eco.schemacatalog.domain.schema.BasicSchemaInfo;
import com.epam.eco.schemacatalog.domain.schema.Mode;
import com.epam.eco.schemacatalog.domain.schema.SubjectSchemas;

import io.confluent.kafka.schemaregistry.avro.AvroCompatibilityLevel;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.rest.entities.Config;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;

import static java.util.Collections.emptyMap;

/**
 * @author Aliaksei_Valyaev
 */
public final class MockExtendedSchemaRegistryClient
        extends MockSchemaRegistryClient
        implements ExtendedSchemaRegistryClient {

    @Override
    public SchemaRegistryServiceInfo getServiceInfo() {
        return SchemaRegistryServiceInfo.with("fake-url.com");
    }

    @Override
    public Schema getBySubjectAndVersion(String subject, int version) {
        return getSchemaInfo(subject, version).getSchemaAvro();
    }

    @Override
    public AvroCompatibilityLevel getGlobalCompatibilityLevel() {
        return retrieveCompatibility(null);
    }

    @Override
    public Optional<AvroCompatibilityLevel> getCompatibilityLevel(String subject) {
        Validate.notBlank(subject, "Subject is blank");

        return Optional.ofNullable(retrieveCompatibility(subject));
    }

    @Override
    public AvroCompatibilityLevel getEffectiveCompatibilityLevel(String subject) {
        Validate.notBlank(subject, "Subject is blank");

        AvroCompatibilityLevel compatilityLevel = retrieveCompatibility(subject);
        if (compatilityLevel == null) {
            compatilityLevel = retrieveCompatibility(null);
        }

        if (compatilityLevel == null) {
            throw new RuntimeException(
                    String.format(
                            "Can't determine effective compatibility level for subject '%s'",
                            subject));
        }

        return compatilityLevel;
    }

    @Override
    public Mode getModeValue() {
        return retrieveMode();
    }

    @Override
    public Optional<Mode> getModeValue(String subject) {
        Validate.notBlank(subject, "Subject is blank");

        return Optional.ofNullable(retrieveMode(subject));
    }

    @Override
    public Mode getEffectiveModeValue(String subject) {
        Validate.notBlank(subject, "Subject is blank");

        Mode mode = retrieveMode(subject);
        if (mode == null) {
            mode = retrieveMode();
        }
        return mode;
    }

    @Override
    public BasicSchemaInfo getSchemaInfo(String subject, int version) {
        Validate.notBlank(subject, "Subject is blank");
        Validate.isTrue(version >= 0, "Version is invalid");

        return getCachedSchemaInfoOrRetrieve(subject, version, null);
    }

    @Override
    public BasicSchemaInfo getLatestSchemaInfo(String subject) {
        Validate.notBlank(subject, "Subject is blank");

        return getCachedSchemaInfoOrRetrieve(subject, null, null);
    }

    @Override
    public SubjectSchemas<BasicSchemaInfo> getSubjectSchemaInfos(String subject) {
        Validate.notBlank(subject, "Subject is blank");

        List<BasicSchemaInfo> schemaInfos = new ArrayList<>();

        List<Integer> versions = getAllVersions(subject);
        versions.forEach(version -> schemaInfos.add(getCachedSchemaInfoOrRetrieve(subject, version, null)));

        return SubjectSchemas.with(schemaInfos);
    }

    @Override
    public BasicSchemaInfo modifyAndRegisterSchema(
            String sourceSubject,
            int sourceVersion,
            String destinationSubject,
            SchemaModification... modifications) {
        return modifyAndRegisterSchema(
                sourceSubject,
                sourceVersion,
                destinationSubject,
                modifications != null ? Arrays.asList(modifications) : null);
    }

    @Override
    public BasicSchemaInfo modifyAndRegisterSchema(
            String sourceSubject,
            int sourceVersion,
            String destinationSubject,
            List<SchemaModification> modifications) {
        return modifyAndRegisterSchema(
                sourceSubject,
                getBySubjectAndVersion(sourceSubject, sourceVersion),
                destinationSubject,
                modifications);
    }

    @Override
    public BasicSchemaInfo modifyAndRegisterSchema(
            String sourceSubject,
            Schema sourceSchema,
            String destinationSubject,
            SchemaModification... modifications) {
        return modifyAndRegisterSchema(
                sourceSubject,
                sourceSchema,
                destinationSubject,
                modifications != null ? Arrays.asList(modifications) : null);
    }

    @Override
    public BasicSchemaInfo modifyAndRegisterSchema(
            String sourceSubject,
            Schema sourceSchema,
            String destinationSubject,
            List<SchemaModification> modifications) {
        Validate.notNull(sourceSchema, "Source schema is null");
        Validate.notBlank(sourceSubject, "Source subject is blank");
        Validate.notBlank(destinationSubject, "Destination subject is blank");

        Schema destinationSchema =
                CachedSchemaModifications.of(modifications).applyTo(sourceSchema);

        registerUnchecked(destinationSubject, destinationSchema);
        int destinationVersion = getVersionUnchecked(destinationSubject, destinationSchema);

        return getCachedSchemaInfoOrRetrieve(
                destinationSubject,
                destinationVersion,
                schemaInfo -> replicateCompatibilityIfNeeded(sourceSubject, destinationSubject));
    }

    @Override
    public void updateCompatibility(String subject, AvroCompatibilityLevel compatibilityLevel) {
        Validate.notBlank(subject, "Subject is blank");
        Validate.notNull(compatibilityLevel, "Compatibility level is null");

        updateCompatibilityUnchecked(subject, compatibilityLevel);
    }

    @Override
    public void updateMode(String subject, Mode mode) {
        Validate.notBlank(subject, "Subject is blank");
        Validate.notNull(mode, "Mode is null");

        updateModeUnchecked(subject, mode);
    }

    @Override
    public boolean subjectExists(String subject) {
        Validate.notBlank(subject, "Subject is blank");

        return getSchemaCache().containsKey(subject);
    }

    @Override
    public List<Integer> deleteSubjectUnchecked(String subject) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer deleteSchema(String subject, int version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getVersionUnchecked(String subject, Schema schema) {
        Map<Schema, Integer> versionCache;
        if (getVersionsCache().containsKey(subject)) {
            versionCache = getVersionsCache().get(subject);
            for (Map.Entry<Schema, Integer> entry : versionCache.entrySet()) {
                if (entry.getKey().toString().equals(schema.toString())) {
                    return entry.getValue();
                }
            }
        }
        throw new RuntimeException("Cannot get version from schema registry!");
    }

    @Override
    public boolean testCompatibilityUnchecked(String subject, Schema schema) {
        try {
            return testCompatibility(subject, schema);
        } catch (IOException | RestClientException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int registerUnchecked(String subject, Schema schema) {
        Map<Schema, Integer> schemaIdMap;
        if (getSchemaCache().containsKey(subject)) {
            schemaIdMap = getSchemaCache().get(subject);
        } else {
            schemaIdMap = new IdentityHashMap<>();
            getSchemaCache().put(subject, schemaIdMap);
        }

        Integer id = schemaIdMap.get(schema);
        if (id == null) {
            id = getIdFromRegistry(subject, schema);
            schemaIdMap.put(schema, id);
            getIdCache().get(null).put(id, schema);
        }
        return id;
    }

    @Override
    public Collection<String> getAllSubjectsUnchecked() {
        try {
            return getAllSubjects();
        } catch (IOException | RestClientException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<Integer> getAllVersionsUnchecked(String subject) {
        return new ArrayList<>(getVersionsCache().getOrDefault(subject, emptyMap()).values());
    }

    @Override
    public boolean checkSchemaWritable(String subject, Schema schema) {
        return true;
    }

    @Override
    public boolean checkSchemaWritable(String subject, int version) {
        return true;
    }

    @Override
    public boolean checkLatestSchemaWritable(String subject) {
        return true;
    }

    private void replicateCompatibilityIfNeeded(String sourceSubject, String destinationSubject) {
        AvroCompatibilityLevel sourceCompatibilityLevel =
                retrieveCompatibility(sourceSubject);
        if (sourceCompatibilityLevel == null) {
            return;
        }

        AvroCompatibilityLevel destinationCompatibilityLevel =
                retrieveCompatibility(destinationSubject);
        if (destinationCompatibilityLevel != null) {
            return;
        }

        updateCompatibilityUnchecked(destinationSubject, sourceCompatibilityLevel);
    }

    private BasicSchemaInfo getCachedSchemaInfoOrRetrieve(
            String subject,
            Integer version,
            Consumer<BasicSchemaInfo> initConsumer) {
        return retrieveSchemaAndConvertToInfo(subject, version, initConsumer);
    }

    private BasicSchemaInfo retrieveSchemaAndConvertToInfo(
            String subject,
            Integer version,
            Consumer<BasicSchemaInfo> initConsumer) {
        SchemaMetadata schemaMetadata;
        if (version == null) {
            try {
                schemaMetadata = getLatestSchemaMetadata(subject);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            schemaMetadata = super.getSchemaMetadata(subject, version);
        }
        BasicSchemaInfo schemaInfo = toSchemaInfo(subject, schemaMetadata);
        if (initConsumer != null) {
            initConsumer.accept(schemaInfo);
        }
        return schemaInfo;
    }

    private Mode retrieveMode() {
        try {
            String mode = super.getMode();
            return mode != null ? Mode.valueOf(mode) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AvroCompatibilityLevel retrieveCompatibility(String subject) {
        try {
            String compatibility = super.getCompatibility(subject);
            return compatibility != null ? AvroCompatibilityLevel.valueOf(compatibility) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Mode retrieveMode(String subject) {
        try {
            String mode = super.getMode(subject);
            return mode != null ? Mode.valueOf(mode) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getIdFromRegistry(String subject, Schema schema) {
        Map<Integer, Schema> idSchemaMap;
        if (getIdCache().containsKey(subject)) {
            idSchemaMap = getIdCache().get(subject);
            for (Map.Entry<Integer, Schema> entry : idSchemaMap.entrySet()) {
                if (entry.getValue().toString().equals(schema.toString())) {
                    return entry.getKey();
                }
            }
        }

        return (int) callClientMethod(
                "getIdFromRegistry",
                new Class[] {String.class, Schema.class, boolean.class},
                subject, schema, Boolean.TRUE);
    }

    private void updateCompatibilityUnchecked(String subject, AvroCompatibilityLevel compatibilityLevel) {
        try {
            updateCompatibility(subject, compatibilityLevel.name());
        } catch (IOException | RestClientException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void updateModeUnchecked(String subject, Mode mode) {
        try {
            setMode(mode.name(), subject);
        } catch (IOException | RestClientException ex) {
            throw new RuntimeException(ex);
        }
    }

    private BasicSchemaInfo toSchemaInfo(
            String subject,
            SchemaMetadata schemaMetadata) {
        return BasicSchemaInfo.builder().
                subject(subject).
                version(schemaMetadata.getVersion()).
                schemaRegistryId(schemaMetadata.getId()).
                schemaJson(schemaMetadata.getSchema()).
                build();
    }

    @SuppressWarnings("unused")
    private AvroCompatibilityLevel toCompatibilityLevel(
            Config configEntity) {
        return AvroCompatibilityLevel.forName(configEntity.getCompatibilityLevel());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<Schema, Integer>> getVersionsCache() {
        return getClientPrivateMap("versionCache");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<Schema, Integer>> getSchemaCache() {
        return getClientPrivateMap("schemaCache");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<Integer, Schema>> getIdCache() {
        return getClientPrivateMap("idCache");
    }

    @SuppressWarnings("rawtypes")
    private Map getClientPrivateMap(String name) {
        try {
            Field field = MockSchemaRegistryClient.class.getDeclaredField(name);
            field.setAccessible(true);
            Map map = (Map) field.get(this);
            return map == null ? emptyMap() : map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object callClientMethod(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = MockSchemaRegistryClient.class
                    .getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(this, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
