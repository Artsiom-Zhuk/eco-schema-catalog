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
package com.epam.eco.schemacatalog.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import io.confluent.kafka.schemaregistry.client.rest.utils.UrlList;

/**
 * @author Andrei_Tytsik
 */
public abstract class UrlListExtractor {

    private UrlListExtractor() {
    }

    public static List<String> extract(UrlList urlList) {
        Validate.notNull(urlList, "URL list is null");

        List<String> urls = new ArrayList<>(urlList.size());
        for (int i = 0; i < urlList.size(); i++) {
            String url = urlList.current();
            urls.add(url);
            urlList.fail(url);
        }
        return urls;
    }

}
