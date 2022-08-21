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

package io.apimap.plugin.jenkins.output;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

public class PublishResult {
    public enum Status {
        CREATED,
        UPDATED,
        FAILED,
        ABORTED,
        UNKNOWN
    }

    private final Status status;
    private final String description;
    private final String token;

    public PublishResult(final Status status,
                         final String description) {
        this.status = status;
        this.description = description;
        this.token = null;
    }

    public PublishResult(final Status status,
                         final String description,
                         final String token) {
        this.status = status;
        this.description = description;
        this.token = token;
    }

    @Whitelisted
    public Status getStatus() {
        return status;
    }

    @Whitelisted
    public String getDescription() {
        return description;
    }

    @Whitelisted
    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "PublishResult{" +
                "status=" + status +
                ", description='" + description + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}
