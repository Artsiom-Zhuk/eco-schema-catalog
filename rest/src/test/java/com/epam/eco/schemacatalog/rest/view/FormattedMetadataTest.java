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
package com.epam.eco.schemacatalog.rest.view;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import com.epam.eco.commons.json.JsonMapper;
import com.epam.eco.schemacatalog.domain.metadata.FieldMetadataKey;
import com.epam.eco.schemacatalog.domain.metadata.Metadata;
import com.epam.eco.schemacatalog.domain.metadata.MetadataValue;
import com.epam.eco.schemacatalog.domain.metadata.format.DocFormatter;
import com.epam.eco.schemacatalog.domain.metadata.format.HtmlPartFormatter;
import com.epam.eco.schemacatalog.domain.metadata.format.ToStringPartFormatter;

/**
 * @author Raman_Babich
 */
public class FormattedMetadataTest {

    @Test
    public void testSerializedToJsonAndBack() throws Exception {
        Date date = new Date();
        FieldMetadataKey key = FieldMetadataKey.with("subject", 1, "schemaFullName", "field");
        MetadataValue value = MetadataValue.builder()
                .doc("doc")
                .attribute("a", "a")
                .updatedAt(date)
                .updatedBy("updateBy")
                .build();
        FormattedMetadata origin = FormattedMetadata.with(key, value, ToStringPartFormatter.INSTANCE);

        String json = JsonMapper.toJson(origin);
        Assert.assertNotNull(json);

        FormattedMetadata deserialized = JsonMapper.jsonToObject(json, FormattedMetadata.class);
        Assert.assertNotNull(deserialized);
        Assert.assertEquals(origin, deserialized);
    }

    @Test
    public void testMetadataToFormattedMetadataConversion() {
        Date date = new Date();
        FieldMetadataKey key = FieldMetadataKey.with("subject", 1, "schemaFullName", "field");
        MetadataValue value = MetadataValue.builder()
                .doc("text @{link google|https://google.com} text")
                .attribute("a", "a")
                .updatedAt(date)
                .updatedBy("updateBy")
                .build();
        Metadata metadata = Metadata.with(key, value);

        FormattedMetadata formattedMetadata = FormattedMetadata.with(metadata, HtmlPartFormatter.INSTANCE);

        Assert.assertEquals(metadata.getKey(), formattedMetadata.getKey());
        Assert.assertEquals(metadata.getValue().getDoc(), formattedMetadata.getValue().getDoc());
        Assert.assertEquals(metadata.getValue().getUpdatedAt(), formattedMetadata.getValue().getUpdatedAt());
        Assert.assertEquals(metadata.getValue().getUpdatedBy(), formattedMetadata.getValue().getUpdatedBy());
        Assert.assertEquals(metadata.getValue().getAttributes(), formattedMetadata.getValue().getAttributes());
        String format = new DocFormatter("text @{link google|https://google.com} text").format(HtmlPartFormatter.INSTANCE);
        Assert.assertEquals(format, formattedMetadata.getValue().getFormattedDoc());
    }

}
