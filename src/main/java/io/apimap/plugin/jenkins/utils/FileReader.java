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

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import hudson.FilePath;
import io.apimap.file.FileFactory;
import io.apimap.file.exceptions.MissingRequiredFieldException;
import io.apimap.file.exceptions.UnsupportedVersionException;
import io.apimap.file.metadata.MetadataFile;
import io.apimap.file.taxonomy.TaxonomyFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FileReader {
    public static MetadataFile metadataFile(FilePath filePath) throws InterruptedException, MissingRequiredFieldException, UnsupportedVersionException, IOException {
        if (filePath == null) throw new FileNotFoundException("[ERROR] Empty metadata file path");

        InputStream fileReader = FileReader.readFileInDirectory(filePath);
        return FileFactory.metadataFromInputStream(fileReader);
    }

    public static TaxonomyFile taxonomyFile(FilePath filePath) throws IOException, InterruptedException {
        if (filePath == null) throw new FileNotFoundException("[ERROR] Empty taxonomy file path");

        InputStream fileReader = FileReader.readFileInDirectory(filePath);
        return FileFactory.taxonomyFromInputStream(fileReader);
    }

    public static InputStream readFileInDirectory(FilePath file) throws IOException, InterruptedException {
        if (file == null) throw new IOException();
        if (!file.exists()) throw new FileNotFoundException();
        return file.read();
    }

    public static FilePath filePath(FilePath basePath, String additionalFilePath) {
        FilePath filePath = basePath;
        if (additionalFilePath != null) filePath = new FilePath(basePath, additionalFilePath);
        return filePath;
    }
}
