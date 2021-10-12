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

package io.apimap;

import io.apimap.api.rest.ClassificationRootRestEntity;
import io.apimap.api.rest.MetadataDataRestEntity;
import io.apimap.client.RestClientConfiguration;
import io.apimap.client.exception.IncorrectTokenException;
import io.apimap.file.metadata.MetadataFile;
import io.apimap.file.taxonomy.TaxonomyFile;
import io.apimap.plugin.jenkins.exceptions.PublishErrorException;
import io.apimap.plugin.jenkins.output.PublishResult;
import io.apimap.plugin.jenkins.step.PublishStep;
import io.apimap.plugin.jenkins.step.publish.PublishStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.IOException;

public class SurrogatePublishStepExecution extends PublishStepExecution {
    public SurrogatePublishStepExecution(PublishStep step, StepContext context) {
        super(step, context);
    }

    public PublishResult failure(String description){
        return super.failure(description);
    }

    public PublishResult success(String description, String token){
        return super.success(description, token);
    }

    public PublishResult run() throws Exception {
        return super.run();
    }

    public MetadataDataRestEntity uploadMetadata(MetadataFile metadataFile, RestClientConfiguration configuration) throws IOException, InterruptedException, IncorrectTokenException, PublishErrorException {
        return super.uploadMetadata(metadataFile, configuration);
    }

    public ClassificationRootRestEntity uploadTaxonomy(String apiName, String apiVersion, TaxonomyFile taxonomyFile, RestClientConfiguration configuration) throws IOException, IncorrectTokenException, PublishErrorException {
        return super.uploadTaxonomy(apiName, apiVersion, taxonomyFile, configuration);
    }
}
