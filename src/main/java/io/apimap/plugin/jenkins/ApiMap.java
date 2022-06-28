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

package io.apimap.plugin.jenkins;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import io.apimap.api.rest.ApiCollectionRootRestEntity;
import io.apimap.api.rest.jsonapi.JsonApiRestResponseWrapper;
import io.apimap.client.IRestClient;
import io.apimap.client.RestClientConfiguration;
import io.apimap.plugin.jenkins.utils.RestClientUtil;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class ApiMap extends JobProperty<AbstractProject<?, ?>> {

    @Extension
    public static final ApiMapDescriptorImpl DESCRIPTOR = new ApiMapDescriptorImpl();

    @Override
    public JobPropertyDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final class ApiMapDescriptorImpl extends JobPropertyDescriptor {
        public static final String BUILD_STEP_DISPLAY_NAME = "ApiMap.io";

        private String url;
        private boolean updateBuildStatus;
        private boolean dryRunMode;
        private boolean debugMode;

        public ApiMapDescriptorImpl() {
            super(ApiMap.class);
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends Job> aClass) {
            return true; //Supports all types of projects
        }

        @Override
        public String getDisplayName() {
            return BUILD_STEP_DISPLAY_NAME;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean updateBuildStatus() {
            return updateBuildStatus;
        }

        public void setUpdateBuildStatus(boolean updateBuildStatus) {
            this.updateBuildStatus = updateBuildStatus;
        }

        public boolean isDryRunMode() {
            return dryRunMode;
        }

        public void setDryRunMode(boolean dryRunMode) {
            this.dryRunMode = dryRunMode;
        }

        public boolean isDebugMode() {
            return debugMode;
        }

        public void setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
        }
    }
}
