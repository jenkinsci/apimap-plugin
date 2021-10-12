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

package io.apimap.plugin.jenkins.step.validate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import hudson.FilePath;
import hudson.model.Result;
import io.apimap.file.metadata.MetadataFile;
import io.apimap.file.taxonomy.TaxonomyFile;
import io.apimap.plugin.jenkins.ApiMap;
import io.apimap.plugin.jenkins.output.ValidateResult;
import io.apimap.plugin.jenkins.step.ValidateStep;
import io.apimap.plugin.jenkins.utils.FileReader;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ValidateStepExecution extends SynchronousStepExecution<ValidateResult> {
    private static final Logger LOGGER = Logger.getLogger(ValidateStepExecution.class.getName());

    public static final String FILEPATH_IS_A_NULL_OBJECT = "Filepath returned as a null object";
    public static final String UNABLE_TO_READ_METADATA_ERROR_MESSAGE = "Unable read metadata content";
    public static final String METADATA_FILE_NOT_FOUND = "Unable to read metadata file";
    public static final String UNABLE_TO_PARSE_METADATA_ERROR_MESSAGE = "Unable to map metadata file values";
    public static final String TAXONOMY_FILE_NOT_FOUND = "Unable to read taxonomy file";
    public static final String UNABLE_TO_READ_TAXONOMY_ERROR_MESSAGE = "Unable read taxonomy content";
    public static final String UNABLE_TO_PARSE_TAXONOMY_ERROR_MESSAGE = "Unable to map taxonomy file values";
    public static final String STEP_COMPLETED_SUCCESSFULLY = "Successfully validated information";

    private static final long serialVersionUID = 1L;

    private final ValidateStep step;


    public ValidateStepExecution(ValidateStep step, StepContext context){
        super(context);
        this.step = step;
    }

    protected ValidateResult failure(String description, ValidateResult.Status result){
        ApiMap.ApiMapDescriptorImpl descImpl = (ApiMap.ApiMapDescriptorImpl) Jenkins.getInstance().getDescriptorByName(ApiMap.class.getName());

        if (descImpl.updateBuildStatus()) {
            getContext().setResult(Result.FAILURE);
            getContext().onFailure(new IOException(description));
        }

        return new ValidateResult(result, description);
    }

    protected ValidateResult success(String description){
        ApiMap.ApiMapDescriptorImpl descImpl = (ApiMap.ApiMapDescriptorImpl) Jenkins.getInstance().getDescriptorByName(ApiMap.class.getName());

        ValidateResult returnValue = new ValidateResult(ValidateResult.Status.VALID, description);

        if(descImpl.updateBuildStatus()) {
            getContext().setResult(Result.SUCCESS);
            getContext().onSuccess(returnValue);
        }

        return returnValue;
    }

    @Override
    protected ValidateResult run() throws Exception {
        FilePath path = getContext().get(FilePath.class);
        if(path == null) { return failure(FILEPATH_IS_A_NULL_OBJECT, ValidateResult.Status.MISSING); }

        LOGGER.log(Level.INFO, "Reading metadata file");
        try {
            MetadataFile metadataFile = FileReader.metadataFile(FileReader.filePath(path, this.step.getMetadataFile()));
            if (metadataFile == null) {
                return failure(UNABLE_TO_READ_METADATA_ERROR_MESSAGE, ValidateResult.Status.MISSING);
            }
        } catch (JsonMappingException e) {
            return failure(UNABLE_TO_PARSE_METADATA_ERROR_MESSAGE, ValidateResult.Status.INVALID);
        } catch (JsonParseException e) {
            return failure(UNABLE_TO_READ_METADATA_ERROR_MESSAGE, ValidateResult.Status.FAILED);
        } catch (IOException e) {
            return failure(METADATA_FILE_NOT_FOUND, ValidateResult.Status.MISSING);
        }

        LOGGER.log(Level.INFO, "Reading taxonomy file");
        try {
            TaxonomyFile taxonomyFile = FileReader.taxonomyFile(FileReader.filePath(path, this.step.getTaxonomyFile()));
            if (taxonomyFile == null) {
                return failure(UNABLE_TO_READ_TAXONOMY_ERROR_MESSAGE, ValidateResult.Status.MISSING);
            }
        } catch (JsonMappingException e) {
            return failure(UNABLE_TO_PARSE_TAXONOMY_ERROR_MESSAGE, ValidateResult.Status.INVALID);
        } catch (JsonParseException e) {
            return failure(UNABLE_TO_READ_TAXONOMY_ERROR_MESSAGE, ValidateResult.Status.FAILED);
        } catch (IOException e) {
            return failure(TAXONOMY_FILE_NOT_FOUND, ValidateResult.Status.MISSING);
        }

        return success(STEP_COMPLETED_SUCCESSFULLY);
    }
}
