/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */

package io.apimap.plugin.jenkins.utils;

import io.apimap.client.RestClientConfiguration;
import io.apimap.plugin.jenkins.ApiMap;
import jenkins.model.Jenkins;

import java.io.IOException;

public class RestClientUtil {
    public static RestClientConfiguration configuration(String token) throws IOException {
        Jenkins instance = Jenkins.getInstanceOrNull();

        if (instance == null) {
            throw new IOException("Unable to find Jenkins Instance");
        }

        ApiMap.ApiMapDescriptorImpl descImpl = (ApiMap.ApiMapDescriptorImpl) instance.getDescriptorByName(ApiMap.class.getName());

        if (descImpl.getUrl() == null) {
            throw new IOException("Missing required root URL");
        }

        return new RestClientConfiguration(
                token,
                descImpl.getUrl(),
                descImpl.isDebugMode()
        );
    }
}