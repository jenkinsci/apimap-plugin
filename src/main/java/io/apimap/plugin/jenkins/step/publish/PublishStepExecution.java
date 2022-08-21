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
import io.apimap.plugin.jenkins.exceptions.IncorrectFileTypeException;
import io.apimap.plugin.jenkins.exceptions.PublishErrorException;
import io.apimap.plugin.jenkins.output.PublishResult;
import io.apimap.plugin.jenkins.step.PublishStep;
import io.apimap.plugin.jenkins.utils.FileReader;
import io.apimap.plugin.jenkins.utils.RestClientUtil;
import jenkins.model.Jenkins;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.hc.core5.http.ContentType;
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
    public static final String README_FILE_MISSING_ERROR = "Unable to read README.md file";
    public static final String CHANGELOG_FILE_MISSING_ERROR = "Unable to read CHANGELOG.md file";
    public static final String UNABLE_TO_UPLOAD_METADATA_ERROR_MESSAGE = "Unable to upload metadata. Please contact your system administrator";
    public static final String UNABLE_TO_UPLOAD_TAXONOMY_ERROR_MESSAGE = "Unable to upload taxonomy classifications. Please contact your system administrator";
    public static final String UNABLE_TO_UPLOAD_README_ERROR_MESSAGE = "Unable to upload README.md classifications. Please contact your system administrator";
    public static final String UNABLE_TO_UPLOAD_CHANGELOG_ERROR_MESSAGE = "Unable to upload CHANGELOG.md classifications. Please contact your system administrator";
    public static final String MISSING_OR_INVALID_API_TOKEN_ERROR_MESSAGE = "Unable to find correct API token";
    public static final String STEP_COMPLETED_SUCCESSFULLY = "Successfully published information";
    public static final String MARKDOWN_FILE_FORMAT_REQUIRED = "File must be of type markdown, ending with .md";


    private static final long serialVersionUID = 1L;

    private final PublishStep step;

    public PublishStepExecution(final PublishStep step,
                                final StepContext context){
        super(context);
        this.step = step;
    }

    protected PublishResult failure(final String description,
                                    final String token){
        final ApiMap.ApiMapDescriptorImpl descImpl = (ApiMap.ApiMapDescriptorImpl) Jenkins.getInstance().getDescriptorByName(ApiMap.class.getName());

        if (descImpl.updateBuildStatus()) {
            getContext().setResult(Result.FAILURE);
            getContext().onFailure(new IOException(description));
        }

        return new PublishResult(PublishResult.Status.FAILED, description, token);
    }

    protected PublishResult success(final String description,
                                    final String token,
                                    final MutableBoolean isApiCreate){
        final ApiMap.ApiMapDescriptorImpl descImpl = (ApiMap.ApiMapDescriptorImpl) Jenkins.getInstance().getDescriptorByName(ApiMap.class.getName());

        PublishResult returnValue;

        if(isApiCreate.isTrue()) {
            returnValue = new PublishResult(PublishResult.Status.CREATED, description, token);
        }else{
            returnValue = new PublishResult(PublishResult.Status.UPDATED, description);
        }

        if(descImpl.updateBuildStatus()) {
            getContext().setResult(Result.SUCCESS);
            getContext().onSuccess(returnValue);
        }

        return returnValue;
    }

    @Override
    protected PublishResult run() throws Exception {
        /*
        * Global
        */
        final ApiMap.ApiMapDescriptorImpl descImpl = (ApiMap.ApiMapDescriptorImpl) Jenkins.getInstance().getDescriptorByName(ApiMap.class.getName());

        final FilePath path = getContext().get(FilePath.class);
        if(path == null) { return failure(FILEPATH_IS_A_NULL_OBJECT, null); }

        final RestClientConfiguration configuration;

        LOGGER.log(Level.FINER, "Reading metadata file");

        final MutableBoolean isApiCreated = new MutableBoolean(false);

        LOGGER.log(Level.FINER, "Creating rest client configuration");
        configuration = RestClientUtil.configuration(this.step.getToken());

        /*
         * Metadata
         */

        final MetadataFile metadataFile;

        try {
            metadataFile = FileReader.metadataFile(FileReader.filePath(path, this.step.getMetadataFile()));
            if (metadataFile == null) {
                return failure(METADATA_FILE_MISSING_ERROR, configuration.getToken());
            }

            LOGGER.log(Level.FINER, "Uploading metadata content");
            final MetadataDataRestEntity metadataReturnObject = uploadMetadata(metadataFile, configuration, isApiCreated);
            if (metadataReturnObject == null) {
                return failure(UNABLE_TO_UPLOAD_METADATA_ERROR_MESSAGE, configuration.getToken());
            }
        } catch (FileNotFoundException e){
            LOGGER.log(Level.FINE, e.getMessage());
            return failure(METADATA_FILE_MISSING_ERROR, configuration.getToken());
        } catch (IOException e) {
            LOGGER.log(Level.FINE, e.getMessage());
            return failure(UNABLE_TO_UPLOAD_METADATA_ERROR_MESSAGE, configuration.getToken());
        } catch (IncorrectTokenException e) {
            LOGGER.log(Level.FINE, e.getMessage());
            return failure(MISSING_OR_INVALID_API_TOKEN_ERROR_MESSAGE, configuration.getToken());
        } catch (PublishErrorException e){
            LOGGER.log(Level.FINE, e.getMessage());
            return failure(e.getMessage(), configuration.getToken());
        }

        /*
         * Taxonomy
         */

        LOGGER.log(Level.FINER, "Reading taxonomy file");

        try {
            final TaxonomyFile taxonomyFile = FileReader.taxonomyFile(FileReader.filePath(path, this.step.getTaxonomyFile()));
            if (taxonomyFile == null) { return failure(TAXONOMY_FILE_MISSING_ERROR, configuration.getToken()); }

            LOGGER.log(Level.FINER, "Uploading taxonomy content");
            final ClassificationRootRestEntity classificationReturnObject = uploadTaxonomy(
                    metadataFile.getData().getName(),
                    metadataFile.getData().getApiVersion(),
                    taxonomyFile,
                    configuration
            );
            if (classificationReturnObject == null) { return failure(UNABLE_TO_UPLOAD_TAXONOMY_ERROR_MESSAGE, configuration.getToken()); }
        } catch (FileNotFoundException e){
            LOGGER.log(Level.FINE, e.getMessage());
            return failure(TAXONOMY_FILE_MISSING_ERROR, configuration.getToken());
        } catch (IncorrectTokenException e) {
            LOGGER.log(Level.FINE, e.getMessage());
            return failure(MISSING_OR_INVALID_API_TOKEN_ERROR_MESSAGE, configuration.getToken());
        } catch (Exception e) {
            LOGGER.log(Level.FINE, e.getMessage());
            return failure(UNABLE_TO_UPLOAD_TAXONOMY_ERROR_MESSAGE, configuration.getToken());
        }

        /*
         * Readme.md
         */

        if(descImpl.isAllowReadmeUpload() && this.step.getReadmeFile() != null && !this.step.getReadmeFile().isEmpty()){
            try{
                final String readme = FileReader.readDocument(FileReader.filePath(path, this.step.getReadmeFile()));
                if (readme == null) { return failure(README_FILE_MISSING_ERROR, configuration.getToken()); }

                LOGGER.log(Level.FINER, "Uploading README.md content");
                final String readmeReturnObject = uploadReadme(
                        metadataFile.getData().getName(),
                        metadataFile.getData().getApiVersion(),
                        readme,
                        configuration
                );
                if (readmeReturnObject == null) { return failure(UNABLE_TO_UPLOAD_README_ERROR_MESSAGE, configuration.getToken()); }
            } catch (IncorrectFileTypeException e){
                LOGGER.log(Level.FINE, e.getMessage());
                return failure(MARKDOWN_FILE_FORMAT_REQUIRED, configuration.getToken());
            } catch (FileNotFoundException e){
                LOGGER.log(Level.FINE, e.getMessage());
                return failure(README_FILE_MISSING_ERROR, configuration.getToken());
            } catch (IncorrectTokenException e) {
                LOGGER.log(Level.FINE, e.getMessage());
                return failure(MISSING_OR_INVALID_API_TOKEN_ERROR_MESSAGE, configuration.getToken());
            } catch (Exception e) {
                LOGGER.log(Level.FINE, e.getMessage());
                return failure(UNABLE_TO_UPLOAD_README_ERROR_MESSAGE, configuration.getToken());
            }
        }

        /*
         * Changelog.md
         */

        if(descImpl.isAllowChangelogUpload() && this.step.getChangelogFile() != null && !this.step.getChangelogFile().isEmpty()){
            try{
                final String changelog = FileReader.readDocument(FileReader.filePath(path, this.step.getChangelogFile()));
                if (changelog == null) { return failure(README_FILE_MISSING_ERROR, configuration.getToken()); }

                LOGGER.log(Level.FINER, "Uploading CHANGELOG.md content");
                final String readmeReturnObject = uploadChangelog(
                        metadataFile.getData().getName(),
                        metadataFile.getData().getApiVersion(),
                        changelog,
                        configuration
                );
                if (readmeReturnObject == null) { return failure(UNABLE_TO_UPLOAD_CHANGELOG_ERROR_MESSAGE, configuration.getToken()); }
            } catch (IncorrectFileTypeException e){
                LOGGER.log(Level.FINE, e.getMessage());
                return failure(MARKDOWN_FILE_FORMAT_REQUIRED, configuration.getToken());
            } catch (FileNotFoundException e){
                LOGGER.log(Level.FINE, e.getMessage());
                return failure(CHANGELOG_FILE_MISSING_ERROR, configuration.getToken());
            } catch (IncorrectTokenException e) {
                LOGGER.log(Level.FINE, e.getMessage());
                return failure(MISSING_OR_INVALID_API_TOKEN_ERROR_MESSAGE, configuration.getToken());
            } catch (Exception e) {
                LOGGER.log(Level.FINE, e.getMessage());
                return failure(UNABLE_TO_UPLOAD_CHANGELOG_ERROR_MESSAGE, configuration.getToken());
            }
        }

        return success(STEP_COMPLETED_SUCCESSFULLY, configuration.getToken(), isApiCreated);
    }

    protected MetadataDataRestEntity uploadMetadata(final MetadataFile metadataFile,
                                                    final RestClientConfiguration configuration,
                                                    final MutableBoolean isApiCreated) throws IOException, InterruptedException, IncorrectTokenException, PublishErrorException {
        /* Assemble REST entities */
        LOGGER.log(Level.FINER, "Assembling REST entities");

        final MetadataDataRestEntity metadataDataApiEntity = new MetadataDataRestEntity(
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

        final ApiDataRestEntity apiDataApiEntity = new ApiDataRestEntity(
                metadataFile.getData().getName(),
                this.step.getRepositoryURL()
        );

        final ApiVersionDataRestEntity apiVersionDataApiEntity = new ApiVersionDataRestEntity(
                metadataDataApiEntity.getApiVersion()
        );

        /* Setup callback methods */
        LOGGER.log(Level.FINER, "Creating callback methods");

        final Consumer<Object> apiCreatedCallback = content -> {
            LOGGER.log(Level.FINER, "Setting token " + ((ApiDataRestEntity) content).getMeta().getToken());
            configuration.setToken(((ApiDataRestEntity) content).getMeta().getToken());
            isApiCreated.setValue(true);
        };

        final Consumer<Object> apiVersionCreatedCallback = content -> {
            LOGGER.log(Level.FINER, content.toString());
        };

        final AtomicReference<String> errorMessage = new AtomicReference<>();
        final Consumer<String> errorHandlerCallback = content -> {
            if(content != null) errorMessage.set(content);
        };

        /* Performing REST calls */
        LOGGER.log(Level.FINER, "Performing REST calls");

        final MetadataDataRestEntity object = IRestClient.withConfiguration(configuration)
                .withErrorHandler(errorHandlerCallback)
                .followCollection(JsonApiRestResponseWrapper.API_COLLECTION)
                .followCollection(metadataDataApiEntity.getName(), JsonApiRestResponseWrapper.VERSION_COLLECTION)
                .onMissingCreate(metadataDataApiEntity.getName(), apiDataApiEntity, apiCreatedCallback)
                .followResource(metadataDataApiEntity.getApiVersion())
                .onMissingCreate(metadataDataApiEntity.getApiVersion(), apiVersionDataApiEntity, apiVersionCreatedCallback)
                .followCollection(JsonApiRestResponseWrapper.METADATA_COLLECTION)
                .createOrUpdateResource(metadataDataApiEntity, ContentType.APPLICATION_JSON);

        if(errorMessage.get() != null){
            throw new PublishErrorException(errorMessage.get());
        }

        return object;
    }

    protected ClassificationRootRestEntity uploadTaxonomy(final String apiName,
                                                          final String apiVersion,
                                                          final TaxonomyFile taxonomyFile,
                                                          final RestClientConfiguration configuration) throws IOException, IncorrectTokenException, PublishErrorException {
        /* Assemble REST entities */
        LOGGER.log(Level.FINER, "Assembling REST entities");

        final ClassificationRootRestEntity classificationRootApiEntity = new ClassificationRootRestEntity(
                taxonomyFile
                        .getData()
                        .getClassifications()
                        .stream()
                        .map(e -> new ClassificationDataRestEntity(e, taxonomyFile.getVersion()))
                        .collect(Collectors.toCollection(ArrayList::new))
        );

        /* Setup callback methods */
        LOGGER.log(Level.FINER, "Creating callback methods");

        final AtomicReference<String> errorMessage = new AtomicReference<>();
        final Consumer<String> errorHandlerCallback = content -> {
            if(content != null) errorMessage.set(content.toString());
        };

        /* Performing REST calls */
        LOGGER.log(Level.FINER, "Performing REST calls");

        final ClassificationRootRestEntity returnValue = IRestClient.withConfiguration(configuration)
                .withErrorHandler(errorHandlerCallback)
                .followCollection(JsonApiRestResponseWrapper.API_COLLECTION)
                .followCollection(apiName, JsonApiRestResponseWrapper.VERSION_COLLECTION)
                .followResource(apiVersion)
                .followCollection(JsonApiRestResponseWrapper.CLASSIFICATION_COLLECTION)
                .createOrUpdateResource(classificationRootApiEntity, ContentType.APPLICATION_JSON);

        if(errorMessage.get() != null){
            throw new PublishErrorException(errorMessage.get());
        }

        return returnValue;
    }

    protected String uploadReadme(final String apiName,
                                  final String apiVersion,
                                  final String readme,
                                  final RestClientConfiguration configuration) throws IOException, IncorrectTokenException, PublishErrorException {
        LOGGER.log(Level.FINER, "Uploading README.md");

        /* Setup callback methods */
        LOGGER.log(Level.FINER, "Creating callback methods");

        final AtomicReference<String> errorMessage = new AtomicReference<>();
        final Consumer<String> errorHandlerCallback = content -> {
            if(content != null) errorMessage.set(content.toString());
        };

        /* Performing REST calls */
        LOGGER.log(Level.FINER, "Performing REST calls");
        final String returnObject = IRestClient.withConfiguration(configuration)
                .withErrorHandler(errorHandlerCallback)
                .followCollection(JsonApiRestResponseWrapper.API_COLLECTION)
                .followCollection(apiName, JsonApiRestResponseWrapper.VERSION_COLLECTION)
                .followResource(apiVersion)
                .followCollection(JsonApiRestResponseWrapper.README_ELEMENT)
                .createOrUpdateResource(readme, ContentType.create("text/markdown"));

        if(errorMessage.get() != null){
            throw new PublishErrorException(errorMessage.get());
        }

        return returnObject;
    }

    protected String uploadChangelog(final String apiName,
                                     final String apiVersion,
                                     final String changelog,
                                     final RestClientConfiguration configuration) throws IOException, IncorrectTokenException, PublishErrorException {
        LOGGER.log(Level.FINER, "Uploading CHANGELOG.md");

        /* Setup callback methods */
        LOGGER.log(Level.FINER, "Creating callback methods");

        final AtomicReference<String> errorMessage = new AtomicReference<>();
        final Consumer<String> errorHandlerCallback = content -> {
            if(content != null) errorMessage.set(content.toString());
        };

        /* Performing REST calls */
        LOGGER.log(Level.FINER, "Performing REST calls");
        final String returnObject = IRestClient.withConfiguration(configuration)
                .withErrorHandler(errorHandlerCallback)
                .followCollection(JsonApiRestResponseWrapper.API_COLLECTION)
                .followCollection(apiName, JsonApiRestResponseWrapper.VERSION_COLLECTION)
                .followResource(apiVersion)
                .followCollection(JsonApiRestResponseWrapper.CHANGELOG_ELEMENT)
                .createOrUpdateResource(changelog, ContentType.create("text/markdown"));

        if(errorMessage.get() != null){
            throw new PublishErrorException(errorMessage.get());
        }

        return returnObject;
    }
}
