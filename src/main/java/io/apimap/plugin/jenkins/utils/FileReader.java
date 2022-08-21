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

import hudson.FilePath;
import io.apimap.file.FileFactory;
import io.apimap.file.exceptions.MissingRequiredFieldException;
import io.apimap.file.exceptions.UnsupportedVersionException;
import io.apimap.file.metadata.MetadataFile;
import io.apimap.file.taxonomy.TaxonomyFile;
import io.apimap.plugin.jenkins.exceptions.FileUnreadableException;
import io.apimap.plugin.jenkins.exceptions.IncorrectFileTypeException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class FileReader {
    public static MetadataFile metadataFile(final FilePath filePath) throws InterruptedException, MissingRequiredFieldException, UnsupportedVersionException, IOException, FileUnreadableException {
        if (filePath == null) throw new FileNotFoundException("[ERROR] Empty metadata file path");

        try (final InputStream fileReader = FileReader.readFileInDirectory(filePath)) {
            return FileFactory.metadataFromInputStream(fileReader);
        } catch (IOException | InterruptedException ignored) {
            throw new FileUnreadableException("Unable to read file");
        }
    }

    public static TaxonomyFile taxonomyFile(final FilePath filePath) throws IOException, InterruptedException, FileUnreadableException {
        if (filePath == null) throw new FileNotFoundException("[ERROR] Empty taxonomy file path");

        try (final InputStream fileReader = FileReader.readFileInDirectory(filePath)) {
            return FileFactory.taxonomyFromInputStream(fileReader);
        } catch (IOException | InterruptedException ignored) {
            throw new FileUnreadableException("Unable to read file");
        }
    }

    public static String readDocument(final FilePath filePath) throws IncorrectFileTypeException, FileUnreadableException {
        if(!filePath.getName().endsWith(".md")){
            throw new IncorrectFileTypeException("File must be of type markdown, ending with .md");
        }

        try (final InputStream fileReader = FileReader.readFileInDirectory(filePath);
             final InputStreamReader reader = new InputStreamReader(fileReader, StandardCharsets.UTF_8);
             final BufferedReader bufferedReader = new BufferedReader(reader)) {

            return bufferedReader
                    .lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException | InterruptedException ignored) {
            throw new FileUnreadableException("Unable to read file");
        }
    }

    public static InputStream readFileInDirectory(final FilePath file) throws IOException, InterruptedException {
        if (file == null) throw new IOException();
        if (!file.exists()) throw new FileNotFoundException();
        return file.read();
    }

    public static FilePath filePath(final FilePath basePath, final String additionalFilePath) {
        FilePath filePath = basePath;
        if (additionalFilePath != null) filePath = new FilePath(basePath, additionalFilePath);
        return filePath;
    }
}
