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
package com.epam.eco.schemacatalog.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.epam.eco.schemacatalog.domain.metadata.format.HtmlPartFormatter;
import com.epam.eco.schemacatalog.domain.rest.request.SchemaRequest;
import com.epam.eco.schemacatalog.domain.rest.request.SubjectRequest;
import com.epam.eco.schemacatalog.domain.rest.response.VersionResponse;
import com.epam.eco.schemacatalog.domain.schema.FullSchemaInfo;
import com.epam.eco.schemacatalog.domain.schema.LiteSchemaInfo;
import com.epam.eco.schemacatalog.domain.schema.SchemaRegisterParams;
import com.epam.eco.schemacatalog.domain.schema.SubjectCompatibilityUpdateParams;
import com.epam.eco.schemacatalog.fts.SearchParams;
import com.epam.eco.schemacatalog.fts.SearchResult;
import com.epam.eco.schemacatalog.fts.repo.SchemaDocumentRepository;
import com.epam.eco.schemacatalog.rest.convert.SchemaDocumentConverter;
import com.epam.eco.schemacatalog.store.SchemaCatalogStore;

/**
 * @author Raman_Babich
 */
@RestController
@RequestMapping("/api/schemas")
public class SchemaController {

    @Autowired
    private SchemaDocumentRepository schemaDocumentRepository;

    @Autowired
    private SchemaCatalogStore schemaCatalogStore;

    @RequestMapping(value = {"", "/"}, method = RequestMethod.GET)
    public SearchResult<LiteSchemaInfo> getSubjects(SearchParams params) {
        return schemaDocumentRepository.searchByParams(params)
                .map(SchemaDocumentConverter::toLiteSchemaInfo);
    }

    @RequestMapping(value = {"/{subject}", "/{subject}/"}, method = RequestMethod.GET)
    public Object getSubject(
            @PathVariable("subject") String subject,
            @RequestParam(value = "onlyLatest", required = false, defaultValue = "false") Boolean onlyLatest) {
        if (onlyLatest) {
            return schemaCatalogStore.getLatestSchema(subject)
                    .toSchemaWithFormattedMetadata(HtmlPartFormatter.INSTANCE);
        }
        return schemaCatalogStore.getSubjectSchemas(subject)
                .transform(fullSchemaInfo -> fullSchemaInfo.
                        toSchemaWithFormattedMetadata(HtmlPartFormatter.INSTANCE));
    }

    @RequestMapping(value = {"/{subject}/{version}", "/{subject}/{version}/"}, method = RequestMethod.GET)
    public FullSchemaInfo getSchema(
            @PathVariable("subject") String subject,
            @PathVariable("version") Integer version) {
        return schemaCatalogStore.getSchema(subject, version)
                .toSchemaWithFormattedMetadata(HtmlPartFormatter.INSTANCE);
    }

    @RequestMapping(value = {"", "/"}, method = RequestMethod.POST)
    public VersionResponse postSchema(@RequestBody SchemaRequest request) {
        SchemaRegisterParams params = SchemaRegisterParams.builder()
                .subject(request.getSubject())
                .schemaJson(request.getSchemaJson())
                .build();
        FullSchemaInfo fullSchemaInfo = schemaCatalogStore.registerSchema(params);
        return VersionResponse.with(fullSchemaInfo.getVersion());
    }

    @RequestMapping(value = {"/{subject}", "/{subject}/"}, method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putSubject(
            @PathVariable("subject") String subject,
            @RequestBody SubjectRequest request) {
        SubjectCompatibilityUpdateParams params = SubjectCompatibilityUpdateParams.builder()
                .subject(subject)
                .compatibilityLevel(request.getCompatibilityLevel())
                .build();
        schemaCatalogStore.updateSubject(params);
    }

    @RequestMapping(value = {"/{subject}", "/{subject}/"}, method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(@PathVariable("subject") String subject) {
        schemaCatalogStore.deleteSubject(subject);
    }

    @RequestMapping(value = {"/{subject}/{version}", "/{subject}/{version}/"}, method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSchema(
            @PathVariable("subject") String subject,
            @PathVariable("version") Integer version) {
        schemaCatalogStore.deleteSchema(subject, version);
    }

}
