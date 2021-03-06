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
package com.epam.eco.schemacatalog.rest.convert;

import com.epam.eco.schemacatalog.domain.schema.LiteSchemaInfo;
import com.epam.eco.schemacatalog.fts.SchemaDocument;

/**
 * @author Raman_Babich
 */
public abstract class SchemaDocumentConverter {

    public static LiteSchemaInfo toLiteSchemaInfo(SchemaDocument source) {
        if (source == null) {
            return null;
        }

        return LiteSchemaInfo.builder()
                .subject(source.getSubject())
                .version(source.getVersion())
                .schemaRegistryId(source.getSchemaRegistryId())
                .name(source.getRootName())
                .namespace(source.getRootNamespace())
                .fullName(source.getRootFullname())
                .compatibility(source.getCompatibility())
                .mode(source.getMode())
                .versionLatest(source.getVersionLatest())
                .deleted(source.getDeleted())
                .build();
    }

    private SchemaDocumentConverter() {
    }
}
