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

package io.apimap.plugin.jenkins.step;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.apimap.plugin.jenkins.step.validate.ValidateStepExecution;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class ValidateStep extends Step implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(PublishStep.class.getName());

    public static final String BUILD_STEP_DISPLAY_NAME = "File content validation";
    public static final String BUILD_STEP_FUNCTION_NAME = "validateAPI";

    public static final String DEFAULT_METADATA_FILE_VALUE = "apimap/metadata.apimap";
    public static final String DEFAULT_TAXONOMY_FILE_VALUE = "apimap/taxonomy.apimap";

    public String metadataFile;
    public String taxonomyFile;

    @DataBoundConstructor
    public ValidateStep(String metadataFile, String taxonomyFile) {
        this.metadataFile = metadataFile;
        this.taxonomyFile = taxonomyFile;
    }

    public String getMetadataFile() {
        if(metadataFile == null) return DEFAULT_METADATA_FILE_VALUE;
        return metadataFile;
    }

    @DataBoundSetter
    public void setMetadataFile(String metadataFile) {
        this.metadataFile = metadataFile;
    }

    public String getTaxonomyFile() {
        if(taxonomyFile == null) return DEFAULT_TAXONOMY_FILE_VALUE;
        return taxonomyFile;
    }

    @DataBoundSetter
    public void setTaxonomyFile(String taxonomyFile) {
        this.taxonomyFile = taxonomyFile;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new ValidateStepExecution(this, stepContext);
    }

    @Symbol(BUILD_STEP_FUNCTION_NAME)
    @Extension
    public static final class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> contexts = new HashSet<>();
            contexts.add(TaskListener.class);
            contexts.add(Run.class);
            return contexts;
        }

        @Override
        public String getFunctionName() {
            return BUILD_STEP_FUNCTION_NAME;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return BUILD_STEP_DISPLAY_NAME;
        }
    }
}
