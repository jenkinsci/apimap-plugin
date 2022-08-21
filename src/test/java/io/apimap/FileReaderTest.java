package io.apimap;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import hudson.FilePath;
import io.apimap.api.rest.ApiDataRestEntity;
import io.apimap.file.exceptions.MissingRequiredFieldException;
import io.apimap.file.exceptions.UnsupportedVersionException;
import io.apimap.file.metadata.MetadataFile;
import io.apimap.file.taxonomy.TaxonomyFile;
import io.apimap.plugin.jenkins.utils.FileReader;
import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

public class FileReaderTest {
    @Test
    public void readMetadataFile_didFailWithNotFound() throws MissingRequiredFieldException, IOException, InterruptedException, UnsupportedVersionException {
        Assert.assertThrows(FileNotFoundException.class,
                ()->{
                    FileReader.metadataFile(null);
                });
    }

    @Test
    public void readTaxonomyFile_didFailWithNotFound() throws MissingRequiredFieldException, IOException, InterruptedException, UnsupportedVersionException {
        Assert.assertThrows(FileNotFoundException.class,
                ()->{
                    FileReader.taxonomyFile(null);
                });
    }

    @Test
    public void readMetadataFile_didFailWithJsonParsing() {
        FilePath filePath = Mockito.mock(FilePath.class);

        MockedStatic<FileReader> fileReader = Mockito.mockStatic(FileReader.class, Mockito.CALLS_REAL_METHODS);
        StringInputStream inputStream = new StringInputStream("{" +
                "\"api catalog version\": 1," +
                "\"failure\":" +
                "{\"name\":\"name\"," +
                "\"visibility\":\"visibility\"," +
                "\"description\":\"description\"," +
                "\"api version\":\"apiVersion\"," +
                "\"release status\":\"releaseStatus\"," +
                "\"system identifier\":\"systemIdentifier\"," +
                "\"documentation\":[\"url1\",\"url2\"]," +
                "\"interface specification\":\"interfaceSpecification\"," +
                "\"interface description language\":\"interfaceDescriptionLanguage\"," +
                "\"architecture layer\":\"architectureLayer\"," +
                "\"business unit\":\"businessUnit\"}}}"
        );
        fileReader.when(() -> FileReader.readFileInDirectory(any())).thenReturn(inputStream);

        Assert.assertThrows(IOException.class,
                ()->{
                       MetadataFile object = FileReader.metadataFile(filePath);
                    fileReader.close();

                });
    }

    @Test
    public void readMetadataFile_didSucceed() throws MissingRequiredFieldException, IOException, InterruptedException, UnsupportedVersionException {
        FilePath filePath = Mockito.mock(FilePath.class);

        MockedStatic<FileReader> fileReader = Mockito.mockStatic(FileReader.class, Mockito.CALLS_REAL_METHODS);
        StringInputStream inputStream = new StringInputStream("{" +
                "\"api catalog version\": 1," +
                "\"data\":" +
                "{\"name\":\"name\"," +
                "\"visibility\":\"visibility\"," +
                "\"description\":\"description\"," +
                "\"api version\":\"apiVersion\"," +
                "\"release status\":\"releaseStatus\"," +
                "\"system identifier\":\"systemIdentifier\"," +
                "\"documentation\":[\"url1\",\"url2\"]," +
                "\"interface specification\":\"interfaceSpecification\"," +
                "\"interface description language\":\"interfaceDescriptionLanguage\"," +
                "\"architecture layer\":\"architectureLayer\"," +
                "\"business unit\":\"businessUnit\"}}}"
        );
        fileReader.when(() -> FileReader.readFileInDirectory(any())).thenReturn(inputStream);

        MetadataFile object = FileReader.metadataFile(filePath);
        fileReader.close();

        assertEquals("name", object.getData().getName());
        assertEquals("visibility", object.getData().getVisibility());
        assertEquals("description", object.getData().getDescription());
        assertEquals("apiVersion", object.getData().getApiVersion());
        assertEquals("releaseStatus", object.getData().getReleaseStatus());
        assertEquals("systemIdentifier", object.getData().getSystemIdentifier());
        assertEquals("interfaceSpecification", object.getData().getInterfaceSpecification());
        assertEquals("interfaceDescriptionLanguage", object.getData().getInterfaceDescriptionLanguage());
        assertEquals("architectureLayer", object.getData().getArchitectureLayer());
        assertEquals("businessUnit", object.getData().getBusinessUnit());
    }

    //-
    @Test
    public void readTaxonomyFile_didFailWithJsonParsing() {
        FilePath filePath = Mockito.mock(FilePath.class);

        MockedStatic<FileReader> fileReader = Mockito.mockStatic(FileReader.class, Mockito.CALLS_REAL_METHODS);
        StringInputStream inputStream = new StringInputStream("{" +
                "\"api catalog version\": 1," +
                "\"failure\":" +
                    "{\"taxonomy\": \"apimap\"," +
                    "\"classifications\": [\"urn:apimap:1\"]}}}"
        );
        fileReader.when(() -> FileReader.readFileInDirectory(any())).thenReturn(inputStream);
        fileReader.close();

        Assert.assertThrows(IOException.class,
                ()->{
                    TaxonomyFile object = FileReader.taxonomyFile(filePath);
                    fileReader.close();

                });
    }

    @Test
    public void readTaxonomyFile_didSucceed() throws MissingRequiredFieldException, IOException, InterruptedException, UnsupportedVersionException {
        FilePath filePath = Mockito.mock(FilePath.class);

        MockedStatic<FileReader> fileReader = Mockito.mockStatic(FileReader.class, Mockito.CALLS_REAL_METHODS);
        StringInputStream inputStream = new StringInputStream("{" +
                "\"api catalog version\": 1," +
                "\"data\":" +
                "{\"taxonomy\": \"apimap\", \"classifications\": [\"urn:apimap:1\"]}}}"
        );
        fileReader.when(() -> FileReader.readFileInDirectory(any())).thenReturn(inputStream);

        TaxonomyFile object = FileReader.taxonomyFile(filePath);
        fileReader.close();

        assertEquals("apimap", object.getData().getTaxonomy());
    }
}
