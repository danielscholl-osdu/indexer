// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.schema.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.indexer.cache.partitionsafe.VirtualPropertiesSchemaCache;
import org.opengroup.osdu.indexer.schema.converter.config.SchemaConverterPropertiesConfig;
import org.opengroup.osdu.indexer.schema.converter.exeption.SchemaProcessingException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class SchemaToStorageFormatImplTest {

    private static final String KIND = "KIND_VAL";

    private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    private JaxRsDpsLog jaxRsDpsLog = Mockito.mock(JaxRsDpsLog.class);

    @InjectMocks
    private SchemaToStorageFormatImpl schemaToStorageFormatImpl
            = new SchemaToStorageFormatImpl(objectMapper, jaxRsDpsLog
            , new SchemaConverterPropertiesConfig());

    @Mock
    private VirtualPropertiesSchemaCache virtualPropertiesSchemaCache;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void dotsDefinitionFormat() {
        testSingleFile("/converter/new-definitions-format/colons-sample.json", "osdu:osdu:Wellbore:1.0.0");
    }

    @Test(expected = SchemaProcessingException.class)
    public void wrongDefinitions() {
        testSingleFile("/converter/bad-schema/wrong-definitions-and-missed-type.json", KIND);
    }

    @Test(expected = SchemaProcessingException.class)
    public void wrongArrayDefinitions() {
        testSingleFile("/converter/bad-schema/wrong-array.json", KIND);
    }

    @Test
    public void firstSchemaPassed() {
        testSingleFile("/converter/basic/schema.json", "osdu:osdu:Wellbore:1.0.0");
    }

    @Test
    public void rootProperties() {
        testSingleFile("/converter/root-properties/schema.json", KIND);
    }

    @Test
    public void integrationTestSchema1() {
        testSingleFile("/converter/integration-tests/index_records_1.schema", KIND);
    }

    @Test
    public void integrationTestSchema2() {
        testSingleFile("/converter/integration-tests/index_records_2.schema", KIND);
    }

    @Test
    public void integrationTestSchema3() {
        testSingleFile("/converter/integration-tests/index_records_3.schema", KIND);
    }

    @Test
    public void wkeSchemaPassed() {
        testSingleFile("/converter/wks/slb_wke_wellbore.json", "slb:wks:wellbore:1.0.6");
    }

    @Test
    public void allOfInsideAllOf() {
        testSingleFile("/converter/tags/allOf/allOf-inside-allOf.json", KIND);
    }

    @Test
    public void allOfInsideProperty() {
        testSingleFile("/converter/tags/allOf/allOf-inside-property.json", KIND);
    }

    @Test
    public void allOfInDefinitions() {
        testSingleFile("/converter/tags/allOf/indefinitions.json", KIND);
    }

    @Test
    public void oneOfInDefinitions() {
        testSingleFile("/converter/tags/oneOf/indefinitions.json", KIND);
    }

    @Test
    public void anyOfInDefinitions() {
        testSingleFile("/converter/tags/anyOf/indefinitions.json", KIND);
    }

    @Test
    public void mixAllAnyOneOf() {
        testSingleFile("/converter/tags/mixAllAnyOneOf/mix.json", KIND);
    }

    @Test
    public void nestedIndexHints() {
        testSingleFile("/converter/index-hints/nested-type-schema.json", "osdu:osdu:Wellbore:1.0.0");
    }

    @Test
    public void virtualProperties() {
        testSingleFile("/converter/index-virtual-properties/virtual-properties-schema.json", "osdu:wks:master-data--Wellbore:1.0.0");
        verify(this.virtualPropertiesSchemaCache, times(1)).put(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void unmatchedVirtualProperties() {
        // The actual property "data.Facility" does not exist for "data.VirtualProperties.DefaultName"
        testSingleFile("/converter/index-virtual-properties/unmatched-virtual-properties-schema.json", "osdu:wks:master-data--Wellbore:1.0.0");
        verify(this.virtualPropertiesSchemaCache, times(1)).put(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void folderPassed() throws URISyntaxException, IOException {

        String folder = "/converter/R3-json-schema";
        Path path = Paths.get(this.getClass().getResource(folder).toURI());
        Files.walk(path)
                .filter(Files::isRegularFile)
                .filter(f -> f.toString().endsWith(".json"))
                .forEach(f -> testSingleFile(f.toString().replaceAll("\\\\", "/").substring(f.toString().replaceAll("\\\\", "/").indexOf(folder)), "osdu:osdu:Wellbore:1.0.0"));
    }

    private void testSingleFile(String filename, String kind) {
        String json = getSchemaFromSchemaService(filename);

        Map<String, Object> converted = schemaToStorageFormatImpl.convertToMap(json, kind);
        Map<String, Object> expected = getStorageSchema(filename + ".res");

        compareSchemas(expected, converted, filename);
    }

    private Map<String, Object> getStorageSchema(String s) {

        TypeReference<Map<String, Object>> typeRef
                = new TypeReference<Map<String, Object>>() {
        };
        try {
            return objectMapper.readValue(this.getClass().getResource(s), typeRef);
        } catch (IOException | IllegalArgumentException e) {
            fail("Failed to load schema from file:" + s);
        }

        return null;
    }

    private String getSchemaFromSchemaService(String s) {
        try {
            return new String(Files.readAllBytes(
                    Paths.get(this.getClass().getResource(s).toURI())), StandardCharsets.UTF_8);
        } catch (Throwable e) {
            fail("Failed to read file:" + s);
        }
        return null;
    }

    private void compareSchemas(Map<String, Object> expected, Map<String, Object> converted, String filename) {
        assertEquals("File:" + filename, expected.size(), converted.size());
        assertEquals("File:" + filename, expected.get("kind"), converted.get("kind"));
        ArrayList<Map<String, String>> conv = (ArrayList<Map<String, String>>) converted.get("schema");
        ArrayList<Map<String, String>> exp = (ArrayList<Map<String, String>>) expected.get("schema");

        checkSchemaIteamsAreEqual(exp, conv, filename);
    }

    private void checkSchemaIteamsAreEqual(ArrayList<Map<String, String>> exp, List<Map<String, String>> conv, String filename) {
        assertEquals("File:" + filename, exp.size(), conv.size());
        conv.forEach((c) -> checkItemIn(c, exp, filename));
    }

    private void checkItemIn(Map<String, String> item, List<Map<String, String>> exp, String filename) {
        String itemPath = item.get("path");
        assertEquals("File:" + filename + ", " + itemPath + " is missed(or too many) see count", exp.stream().filter(e -> itemPath.equals(e.get("path"))).count(), 1L);
        Map<String, String> found = exp.stream().filter(e -> item.get("path").equals(e.get("path"))).findAny().get();
        assertEquals("File:" + filename + ", in " + itemPath, found.get("kind"), item.get("kind"));
    }
}
