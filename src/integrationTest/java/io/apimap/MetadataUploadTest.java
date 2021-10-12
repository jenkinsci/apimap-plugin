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

import io.apimap.client.RestClientConfiguration;
import io.apimap.client.exception.IncorrectTokenException;
import io.apimap.file.metadata.MetadataDataWrapper;
import io.apimap.file.metadata.MetadataFile;
import io.apimap.plugin.jenkins.exceptions.PublishErrorException;
import io.apimap.plugin.jenkins.step.PublishStep;
import org.apache.http.HttpEntity;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class MetadataUploadTest {
    @Test
    public void uploadFile_didSucceed() throws IncorrectTokenException, IOException, PublishErrorException, InterruptedException {
        PublishStep publishStep = mock(PublishStep.class);
        when(publishStep.getRepositoryURL()).thenReturn("http://localhost:888");

        StepContext context = mock(StepContext.class);

        SurrogatePublishStepExecution publishStepExecution = new SurrogatePublishStepExecution(publishStep, context);

        MetadataDataWrapper data = new MetadataDataWrapper(
                "Hello World",
                "My first API",
                "Public",
                "1.0.0",
                "Design",
                "JSON:API v1.1",
                "OpenAPI Specification",
                "My department",
                "Apimap.io",
                "System1",
                Arrays.asList("http://localhost:8080")
        );

        MetadataFile metadataFile = new MetadataFile("1", data);
        RestClientConfiguration configuration = new RestClientConfiguration(null, "http://localhost:8080", true);
        publishStepExecution.uploadMetadata(metadataFile, configuration);
    }
}
