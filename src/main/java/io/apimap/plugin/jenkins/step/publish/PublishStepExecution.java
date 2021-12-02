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

package io.apimap.plugin.jenkins.step.publish;

import hudson.FilePath;
import hudson.model.Result;
import io.apimap.api.rest.ApiDataRestEntity;
import io.apimap.api.rest.ApiVersionDataRestEntity;
import io.apimap.api.rest.ClassificationDataRestEntity;
import io.apimap.api.rest.ClassificationRootRestEntity;
import io.apimap.api.rest.MetadataDataRestEntity;
import io.apimap.api.rest.jsonapi.JsonApiRestResponseWrapper;
import io.apimap.client.IRestClient;
import io.apimap.client.RestClientConfiguration;
import io.apimap.client.exception.IncorrectTokenException;
import io.apimap.file.metadata.MetadataFile;
import io.apimap.file.taxonomy.TaxonomyFile;
import io.apimap.plugin.jenkins.ApiMap;
import io.apimap.plugin.jenkins.exceptions.PublishErrorException;
import io.apimap.plugin.jenkins.output.PublishResult;
import io.apimap.plugin.jenkins.step.PublishStep;
import io.apimap.plugin.jenkins.utils.FileReader;
import io.apimap.plugin.jenkins.utils.RestClientUtil;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PublishStepExecution extends SynchronousStepExecution<PublishResult> {
    private static final Logger LOGGER = Logger.getLogger(PublishStepExecution.class.getName());

    public static final String FILEPATH_IS_A_NULL_OBJECT = "Filepath returned as a null object";
    public static final String METADATA_FILE_MISSING_ERROR = "Unable to read metadata file";
    public static final String TAXONOMY_FILE_MISSING_ERROR = "Unable to read taxonomy file";
    public static final String UNABLE_TO_UPLOAD_METADATA_ERROR_MESSAGE = "Unable to upload metadata";
    public static final String UNABLE_TO_UPLOAD_TAXONOMY_ERROR_MESSAGE = "Unable to upload taxonomy classifications";
    public static final String MISSING_OR_INVALID_API_TOKEN_ERROR_MESSAGE = "Unable to find correct API token";
    public static final String STEP_COMPLETED_SUCCESSFULLY = "Successfully published information";

    private static final long serialVersionUID = 1L;

    private final PublishStep step;

    public PublishStepExecution(PublishStep step, StepContext context){
        super(context);
        this.step = step;
    }

    protected PublishResult failure(String description){
        ApiMap.ApiMapDescriptorImpl descImpl = (ApiMap.ApiMapDescriptorImpl) Jenkins.getInstance().getDescriptorByName(ApiMap.class.getName());

        if (descImpl.updateBuildStatus()) {
            getContext().setResult(Result.FAILURE);
            getContext().onFailure(new IOException(description));
        }

        return new PublishResult(PublishResult.Status.FAILED, description);
    }

    protected PublishResult success(String description, String token){
        ApiMap.ApiMapDescriptorImpl descImpl = (ApiMap.ApiMapDescriptorImpl) Jenkins.getInstance().getDescriptorByName(ApiMap.class.getName());

        PublishResult returnValue;

        if(token == null) {
            returnValue = new PublishResult(PublishResult.Status.UPDATED, description);
        }else{
            returnValue = new PublishResult(PublishResult.Status.CREATED, description, token);
        }

        if(descImpl.updateBuildStatus()) {
            getContext().setResult(Result.SUCCESS);
            getContext().onSuccess(returnValue);
        }

        return returnValue;
    }

    @Override
    protected PublishResult run() throws Exception {
        FilePath path = getContext().get(FilePath.class);
        if(path == null) { return failure(FILEPATH_IS_A_NULL_OBJECT); }

        MetadataFile metadataFile;
        RestClientConfiguration configuration;

        LOGGER.log(Level.INFO, "Reading metadata file");

        try {
            metadataFile = FileReader.metadataFile(FileReader.filePath(path, this.step.getMetadataFile()));
            if (metadataFile == null) {
                return failure(METADATA_FILE_MISSING_ERROR);
            }

            LOGGER.log(Level.INFO, "Creating rest client configuration");
            configuration = RestClientUtil.configuration(this.step.getToken());

            LOGGER.log(Level.INFO, "Uploading metadata content");
            MetadataDataRestEntity metadataReturnObject = uploadMetadata(metadataFile, configuration);
            if (metadataReturnObject == null) {
                return failure(UNABLE_TO_UPLOAD_METADATA_ERROR_MESSAGE);
            }
        } catch (FileNotFoundException e){
            LOGGER.log(Level.INFO, e.getMessage());
            return failure(METADATA_FILE_MISSING_ERROR);
        } catch (IOException e) {
            LOGGER.log(Level.INFO, e.getMessage());
            return failure(UNABLE_TO_UPLOAD_METADATA_ERROR_MESSAGE);
        } catch (IncorrectTokenException e) {
            LOGGER.log(Level.INFO, e.getMessage());
            return failure(MISSING_OR_INVALID_API_TOKEN_ERROR_MESSAGE);
        } catch (PublishErrorException e){
            LOGGER.log(Level.INFO, e.getMessage());
            return failure(e.getMessage());
        }

        LOGGER.log(Level.INFO, "Reading taxonomy file");

        try {
            TaxonomyFile taxonomyFile = FileReader.taxonomyFile(FileReader.filePath(path, this.step.getTaxonomyFile()));
            if (taxonomyFile == null) { return failure(TAXONOMY_FILE_MISSING_ERROR); }

            LOGGER.log(Level.INFO, "Uploading taxonomy content");
            ClassificationRootRestEntity classificationReturnObject = uploadTaxonomy(
                    metadataFile.getData().getName(),
                    metadataFile.getData().getApiVersion(),
                    taxonomyFile,
                    configuration
            );
            if (classificationReturnObject == null) { return failure(UNABLE_TO_UPLOAD_TAXONOMY_ERROR_MESSAGE); }
        } catch (FileNotFoundException e){
            LOGGER.log(Level.INFO, e.getMessage());
            return failure(TAXONOMY_FILE_MISSING_ERROR);
        } catch (IncorrectTokenException e) {
            LOGGER.log(Level.INFO, e.getMessage());
            return failure(MISSING_OR_INVALID_API_TOKEN_ERROR_MESSAGE);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, e.getMessage());
            return failure(UNABLE_TO_UPLOAD_TAXONOMY_ERROR_MESSAGE);
        }

        if(this.step.getToken() == null) {
            return success(STEP_COMPLETED_SUCCESSFULLY, configuration.getToken());
        }

        return success(STEP_COMPLETED_SUCCESSFULLY, null);
    }

    protected MetadataDataRestEntity uploadMetadata(MetadataFile metadataFile, RestClientConfiguration configuration) throws IOException, InterruptedException, IncorrectTokenException, PublishErrorException {
        /* Assemble REST entities */
        LOGGER.log(Level.INFO, "Assembling REST entities");

        MetadataDataRestEntity metadataDataApiEntity = new MetadataDataRestEntity(
                metadataFile.getData().getName(),
                metadataFile.getData().getDescription(),
                metadataFile.getData().getVisibility(),
                metadataFile.getData().getApiVersion(),
                metadataFile.getData().getReleaseStatus(),
                metadataFile.getData().getInterfaceSpecification(),
                metadataFile.getData().getInterfaceDescriptionLanguage(),
                metadataFile.getData().getArchitectureLayer(),
                metadataFile.getData().getBusinessUnit(),
                metadataFile.getData().getSystemIdentifier(),
                metadataFile.getData().getDocumentation()
        );

        ApiDataRestEntity apiDataApiEntity = new ApiDataRestEntity(
                metadataFile.getData().getName(),
                this.step.getRepositoryURL()
        );

        ApiVersionDataRestEntity apiVersionDataApiEntity = new ApiVersionDataRestEntity(
                metadataDataApiEntity.getApiVersion()
        );

        /* Setup callback methods */
        LOGGER.log(Level.INFO, "Creating callback methods");

        Consumer<Object> apiCreatedCallback = content -> {
            LOGGER.log(Level.INFO, "Setting token " + ((ApiDataRestEntity) content).getMeta().getToken());
            configuration.setToken(((ApiDataRestEntity) content).getMeta().getToken());
        };

        Consumer<Object> apiVersionCreatedCallback = content -> {
            LOGGER.log(Level.INFO, "apiVersionCreatedCallback");
            LOGGER.log(Level.INFO, content.toString());
        };

        AtomicReference<String> errorMessage = new AtomicReference<>();
        Consumer<String> errorHandlerCallback = content -> {
            if(content != null) errorMessage.set(content);
        };

        /* Performing REST calls */
        LOGGER.log(Level.INFO, "Performing REST calls");

        MetadataDataRestEntity object = IRestClient.withConfiguration(configuration)
                .withErrorHandler(errorHandlerCallback)
                .followCollection(JsonApiRestResponseWrapper.API_COLLECTION)
                .followCollection(metadataDataApiEntity.getName(), JsonApiRestResponseWrapper.VERSION_COLLECTION)
                .onMissingCreate(metadataDataApiEntity.getName(), apiDataApiEntity, apiCreatedCallback)
                .followResource(metadataDataApiEntity.getApiVersion())
                .onMissingCreate(metadataDataApiEntity.getApiVersion(), apiVersionDataApiEntity, apiVersionCreatedCallback)
                .followCollection(JsonApiRestResponseWrapper.METADATA_COLLECTION)
                .createOrUpdateResource(metadataDataApiEntity);

        if(errorMessage.get() != null){
            throw new PublishErrorException(errorMessage.get());
        }

        return object;
    }

    protected ClassificationRootRestEntity uploadTaxonomy(String apiName, String apiVersion, TaxonomyFile taxonomyFile, RestClientConfiguration configuration) throws IOException, IncorrectTokenException, PublishErrorException {
        /* Assemble REST entities */
        LOGGER.log(Level.INFO, "Assembling REST entities");

        ClassificationRootRestEntity classificationRootApiEntity = new ClassificationRootRestEntity(
                taxonomyFile
                        .getData()
                        .getClassifications()
                        .stream()
                        .map(e -> new ClassificationDataRestEntity(e, taxonomyFile.getVersion()))
                        .collect(Collectors.toCollection(ArrayList::new))
        );

        /* Setup callback methods */
        LOGGER.log(Level.INFO, "Creating callback methods");

        AtomicReference<String> errorMessage = new AtomicReference<>();
        Consumer<String> errorHandlerCallback = content -> {
            if(content != null) errorMessage.set(content.toString());
        };


        /* Performing REST calls */
        LOGGER.log(Level.INFO, "Performing REST calls");

        ClassificationRootRestEntity returnValue = IRestClient.withConfiguration(configuration)
                .withErrorHandler(errorHandlerCallback)
                .followCollection(JsonApiRestResponseWrapper.API_COLLECTION)
                .followCollection(apiName, JsonApiRestResponseWrapper.VERSION_COLLECTION)
                .followResource(apiVersion)
                .followCollection(JsonApiRestResponseWrapper.CLASSIFICATION_COLLECTION)
                .createOrUpdateResource(classificationRootApiEntity);

        if(errorMessage.get() != null){
            throw new PublishErrorException(errorMessage.get());
        }

        return returnValue;
    }
}
