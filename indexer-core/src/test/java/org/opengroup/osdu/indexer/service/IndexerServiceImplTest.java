/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.service;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.*;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.storage.ConversionStatus;
import org.opengroup.osdu.core.common.provider.interfaces.IRequestInfo;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.indexer.logging.AuditLogger;
import org.opengroup.osdu.indexer.model.indexproperty.AugmenterConfiguration;
import org.opengroup.osdu.indexer.provider.interfaces.IPublisher;
import org.opengroup.osdu.indexer.util.AugmenterSetting;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.opengroup.osdu.indexer.util.IndexerQueueTaskBuilder;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class IndexerServiceImplTest {

    private static MockedStatic<Acl> mockedAcls;

    @InjectMocks
    private IndexerServiceImpl sut;
    @Mock
    private ElasticClientHandler elasticClientHandler;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Mock
    private IndicesService indicesService;
    @Mock
    private StorageService storageService;
    @Mock
    private StorageIndexerPayloadMapper storageIndexerPayloadMapper;
    @InjectMocks
    private JobStatus jobStatus;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private BulkResponse bulkResponse;
    @Mock
    private IRequestInfo requestInfo;
    @Mock
    private RestHighLevelClient restHighLevelClient;
    @Mock
    private IndexSchemaService schemaService;
    @Mock
    private JaxRsDpsLog jaxRsDpsLog;
    @Mock
    private IMappingService mappingService;
    @Mock
    private IPublisher progressPublisher;
    @Mock
    private AugmenterConfigurationService augmenterConfigurationService;
    @Mock
    private IndexerQueueTaskBuilder indexerQueueTaskBuilder;
    @Mock
    private AugmenterSetting augmenterSetting;
    @Mock
    private IFeatureFlag asIngestedCoordinatesFeatureFlag;

    private List<RecordInfo> recordInfos = new ArrayList<>();

    private final String pubsubMsg = "[{\"id\":\"opendes:doc:test1\",\"kind\":\"opendes:testindexer1:well:1.0.0\",\"op\":\"update\"}," +
            "{\"id\":\"opendes:doc:test2\",\"kind\":\"opendes:testindexer2:well:1.0.0\",\"op\":\"create\"}," +
            " {\"id\":\"opendes:doc:test3\",\"kind\":\"opendes:testindexer2:well:1.0.0\",\"op\":\"create\"}," +
            " {\"id\":\"opendes:doc:test4\",\"kind\":\"osdu:wks:reference-data--IndexPropertyPathConfiguration:1.0.0\",\"op\":\"create\"}]";
    private final String pubsubMsgForDeletion = "[{\"id\":\"opendes:doc:test1\",\"kind\":\"opendes:testindexer1:well:1.0.0\",\"op\":\"delete\"}," +
            "{\"id\":\"opendes:doc:test2\",\"kind\":\"opendes:testindexer2:well:1.0.0\",\"op\":\"delete\"}, " +
            "{\"id\":\"opendes:doc:test3\",\"kind\":\"opendes:testindexer2:well:1.0.0\",\"op\":\"delete\"}," +
            "{\"id\":\"opendes:doc:test4\",\"kind\":\"osdu:wks:reference-data--IndexPropertyPathConfiguration:1.0.0\",\"op\":\"delete\"}]";

    private final String kind1 = "opendes:testindexer1:well:1.0.0";
    private final String kind2 = "opendes:testindexer2:well:1.0.0";
    private final String kind3 = "osdu:wks:reference-data--IndexPropertyPathConfiguration:1.0.0";
    private final String recordId1 = "opendes:doc:test1";
    private final String recordId2 = "opendes:doc:test2";
    private final String recordId3 = "opendes:doc:test3";
    private final String recordId4 = "opendes:doc:test4";
    private final String failureMassage = "test failure";
    private final String badRequestMessage = "Elasticsearch exception [type=mapper_parsing_exception, reason=failed to parse field [data.SpatialLocation.Wgs84Coordinates] of type [geo_shape]]";

    private DpsHeaders dpsHeaders;
    private RecordChangedMessages recordChangedMessages;

    @Before
    public void setup() throws IOException {
        jobStatus = spy(new JobStatus());
        mockedAcls = mockStatic(Acl.class);
        initMocks(this);
        when(augmenterSetting.isEnabled()).thenReturn(true);
    }

    @After
    public void close() {
        mockedAcls.close();
    }

    @Test
    public void processSchemaMessagesTest() throws Exception {
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setId("opendes:ds:mytest3-d9033ae1-fb15-496c-9ba0-880fd1d2b2qf");
        recordInfo.setKind("opendes:ds:mytest2:1.0.0");
        recordInfo.setOp("purge_schema");
        this.recordInfos.add(recordInfo);

        initMocks(this);

        this.sut.processSchemaMessages(recordInfos);

        verify(this.elasticClientHandler, times(1)).createRestClient();
        verify(this.elasticIndexNameResolver, times(1)).getIndexNameFromKind(any());
        verify(this.indicesService, times(1)).isIndexExist(any(), any());
    }

    @Test
    public void should_properlyUpdateAuditLogs_givenValidCreateAndUpdateRecords() {
        try {
            prepareTestDataAndEnv(this.pubsubMsg);

            // test
            JobStatus jobStatus = this.sut.processRecordChangedMessages(recordChangedMessages, recordInfos);

            // validate
            assertEquals(4, jobStatus.getStatusesList().size());
            assertEquals(2, jobStatus.getIdsByIndexingStatus(IndexingStatus.FAIL).size());
            assertEquals(2, jobStatus.getIdsByIndexingStatus(IndexingStatus.SUCCESS).size());

            verify(restHighLevelClient, times(2)).bulk(any(), any());
            verify(this.auditLogger).indexCreateRecordSuccess(Arrays.asList("RecordStatus(id=opendes:doc:test2, kind=opendes:testindexer2:well:1.0.0, operationType=create, status=SUCCESS)",
                    "RecordStatus(id=opendes:doc:test4, kind=osdu:wks:reference-data--IndexPropertyPathConfiguration:1.0.0, operationType=create, status=SUCCESS)"));
            verify(this.auditLogger).indexUpdateRecordFail(singletonList("RecordStatus(id=opendes:doc:test1, kind=opendes:testindexer1:well:1.0.0, operationType=update, status=FAIL, message=test failure)"));
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }


    @Test
    public void should_properlyUpdateAuditLogs_givenPartiallyValidRecords() {
        try {
            preparePartiallyValidTestData(this.pubsubMsg);

            // test
            JobStatus jobStatus = this.sut.processRecordChangedMessages(recordChangedMessages, recordInfos);

            // validate
            assertEquals(4, jobStatus.getStatusesList().size());
            assertEquals(1, jobStatus.getIdsByIndexingStatus(IndexingStatus.WARN).size());
            assertEquals(1, jobStatus.getIdsByIndexingStatus(IndexingStatus.FAIL).size());
            assertEquals(2, jobStatus.getIdsByIndexingStatus(IndexingStatus.SUCCESS).size());

            verify(restHighLevelClient, times(2)).bulk(any(), any());
            verify(this.auditLogger).indexStarted(Arrays.asList("id=opendes:doc:test1 kind=opendes:testindexer1:well:1.0.0 operationType=update", "id=opendes:doc:test2 kind=opendes:testindexer2:well:1.0.0 operationType=create",
                    "id=opendes:doc:test3 kind=opendes:testindexer2:well:1.0.0 operationType=create", "id=opendes:doc:test4 kind=osdu:wks:reference-data--IndexPropertyPathConfiguration:1.0.0 operationType=create"));
            verify(this.auditLogger).indexCreateRecordSuccess(Arrays.asList("RecordStatus(id=opendes:doc:test2, kind=opendes:testindexer2:well:1.0.0, operationType=create, status=SUCCESS)",
                    "RecordStatus(id=opendes:doc:test4, kind=osdu:wks:reference-data--IndexPropertyPathConfiguration:1.0.0, operationType=create, status=SUCCESS)"));
            verify(this.auditLogger).indexCreateRecordFail(singletonList("RecordStatus(id=opendes:doc:test3, kind=opendes:testindexer2:well:1.0.0, operationType=create, status=FAIL, message=null)"));
            verify(this.auditLogger).indexUpdateRecordPartialSuccess(singletonList("RecordStatus(id=opendes:doc:test1, kind=opendes:testindexer1:well:1.0.0, operationType=update, status==PARTIAL_SUCCESS, message=Indexed Successfully)"));
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_updateAssociatedRecords_givenValidCreateAndUpdateRecords() {
        try {
            prepareTestDataAndEnv(this.pubsubMsg);

            // setup property configuration
            when(this.augmenterConfigurationService.isConfigurationEnabled(eq(kind1))).thenReturn(true);
            when(this.augmenterConfigurationService.isConfigurationEnabled(eq(kind2))).thenReturn(true);
            when(this.augmenterConfigurationService.getRelatedKindsOfConfigurations(any())).thenReturn(new ArrayList<>());
            ArgumentCaptor<Map<String, List<String>>> upsertArgumentCaptor = ArgumentCaptor.forClass(Map.class);
            ArgumentCaptor<Map<String, List<String>>> deleteArgumentCaptor = ArgumentCaptor.forClass(Map.class);

            // test
            this.sut.processRecordChangedMessages(recordChangedMessages, recordInfos);

            // validate
            verify(this.augmenterConfigurationService, times(1)).updateAssociatedRecords(any(), upsertArgumentCaptor.capture(), deleteArgumentCaptor.capture(), any());
            Map<String, List<String>> upsertKindIds = upsertArgumentCaptor.getValue();
            Map<String, List<String>> deleteKindIds = deleteArgumentCaptor.getValue();
            assertEquals(2, upsertKindIds.size());
            assertEquals(1, upsertKindIds.get(kind1).size());
            assertEquals(2, upsertKindIds.get(kind2).size());
            assertEquals(0, deleteKindIds.size());
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_updateAssociatedRecords_givenValidDeleteRecords() {
        try {
            prepareTestDataAndEnv(this.pubsubMsgForDeletion);

            // setup property configuration
            when(this.augmenterConfigurationService.isConfigurationEnabled(any())).thenReturn(true);
            ArgumentCaptor<Map<String, List<String>>> upsertArgumentCaptor = ArgumentCaptor.forClass(Map.class);
            ArgumentCaptor<Map<String, List<String>>> deleteArgumentCaptor = ArgumentCaptor.forClass(Map.class);

            // test
            this.sut.processRecordChangedMessages(recordChangedMessages, recordInfos);

            // validate
            verify(this.augmenterConfigurationService, times(1)).updateAssociatedRecords(any(), upsertArgumentCaptor.capture(), deleteArgumentCaptor.capture(), any());
            Map<String, List<String>> upsertKindIds = upsertArgumentCaptor.getValue();
            Map<String, List<String>> deleteKindIds = deleteArgumentCaptor.getValue();
            assertEquals(0, upsertKindIds.size());
            assertEquals(3, deleteKindIds.size());
            assertEquals(1, deleteKindIds.get(kind1).size());
            assertEquals(2, deleteKindIds.get(kind2).size());
            assertEquals(1, deleteKindIds.get(kind3).size());
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_mergeExtendedProperties_givenValidCreateAndUpdateRecords_and_kindsHavingPropertyConfigurations() {
        try {
            prepareTestDataAndEnv(this.pubsubMsg);

            // setup property configuration
            when(this.augmenterConfigurationService.isConfigurationEnabled(eq(kind1))).thenReturn(true);
            when(this.augmenterConfigurationService.isConfigurationEnabled(eq(kind2))).thenReturn(true);
            when(this.augmenterConfigurationService.getRelatedKindsOfConfigurations(any())).thenReturn(new ArrayList<>());
            when(this.augmenterConfigurationService.getConfiguration(any())).thenReturn(new AugmenterConfiguration());

            // test
            this.sut.processRecordChangedMessages(recordChangedMessages, recordInfos);

            // validate
            verify(this.augmenterConfigurationService, times(2)).getConfiguration(any());
            verify(this.augmenterConfigurationService, times(2)).getExtendedProperties(any(), any(), any());
            verify(this.augmenterConfigurationService, times(3)).cacheDataRecord(any(), any(), any());
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_updateSchemaMappingOfRelatedKinds_givenValidCreateAndUpdateRecords() {
        try {
            prepareTestDataAndEnv(this.pubsubMsg);

            // setup property configuration
            List<String> relatedKinds = Arrays.asList("related_kind:1.0.0", "related_kind:1.1.0", "related_kind:1.2.0");
            ArgumentCaptor<List<String>> configurationIdArgumentCaptor = ArgumentCaptor.forClass(List.class);
            when(this.augmenterConfigurationService.getRelatedKindsOfConfigurations(configurationIdArgumentCaptor.capture())).thenReturn(relatedKinds);

            // test
            this.sut.processRecordChangedMessages(recordChangedMessages, recordInfos);

            // validate
            List<String> configurationIds = configurationIdArgumentCaptor.getValue();
            assertEquals(1, configurationIds.size());
            assertEquals(recordId4, configurationIds.get(0));
            verify(this.schemaService, times(relatedKinds.size())).processSchemaUpsertEvent(any(), anyString());
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    private void prepareTestDataAndEnv(String pubsubMsg) throws IOException, URISyntaxException {

        // setup headers
        this.dpsHeaders = new DpsHeaders();
        this.dpsHeaders.put(DpsHeaders.AUTHORIZATION, "testAuth");

        // setup message
        Type listType = new TypeToken<List<RecordInfo>>() {}.getType();
        this.recordInfos = (new Gson()).fromJson(pubsubMsg, listType);
        Map<String, String> messageAttributes = new HashMap<>();
        messageAttributes.put(DpsHeaders.DATA_PARTITION_ID, "opendes");
        this.recordChangedMessages = RecordChangedMessages.builder().attributes(messageAttributes).messageId("xxxx").publishTime("2000-01-02T10:10:44+0000").data("{}").build();

        // setup schema
        Map<String, Object> schema = createSchema();
        indexSchemaServiceMock(kind3, schema);
        indexSchemaServiceMock(kind2, schema);
        indexSchemaServiceMock(kind1, null);

        // setup storage records
        Map<String, Object> storageData = new HashMap<>();
        storageData.put("schema1", "test-value");
        List<Records.Entity> validRecords = new ArrayList<>();
        validRecords.add(Records.Entity.builder().id(recordId2).kind(kind2).data(storageData).build());
        validRecords.add(Records.Entity.builder().id(recordId3).kind(kind2).data(storageData).build());
        validRecords.add(Records.Entity.builder().id(recordId4).kind(kind3).data(storageData).build());
        List<ConversionStatus> conversionStatus = new LinkedList<>();
        Records storageRecords = Records.builder().records(validRecords).conversionStatuses(conversionStatus).build();
        when(this.storageService.getStorageRecords(any(), any())).thenReturn(storageRecords);

        // setup elastic, index and mapped document
        when(this.indicesService.createIndex(any(), any(), any(), any(), any())).thenReturn(true);
        when(this.mappingService.getIndexMappingFromRecordSchema(any())).thenReturn(new HashMap<>());

        when(this.elasticClientHandler.createRestClient()).thenReturn(this.restHighLevelClient);
        when(this.restHighLevelClient.bulk(any(), any(RequestOptions.class))).thenReturn(this.bulkResponse);

        Map<String, Object> indexerMappedPayload = new HashMap<>();
        indexerMappedPayload.put("id", "keyword");
        when(this.storageIndexerPayloadMapper.mapDataPayload(any(), any(), any(), any())).thenReturn(indexerMappedPayload);

        BulkItemResponse[] responses = new BulkItemResponse[]{prepareFailedResponse(recordId1), prepareSuccessfulResponse(recordId2),
                prepareSuccessfulResponse(recordId4), prepareFailed400Response(recordId3)};
        when(this.bulkResponse.getItems()).thenReturn(responses);
    }

    private void preparePartiallyValidTestData(String pubsubMsg) throws IOException, URISyntaxException {

        // setup headers
        this.dpsHeaders = new DpsHeaders();
        this.dpsHeaders.put(DpsHeaders.AUTHORIZATION, "testAuth");

        // setup message
        Type listType = new TypeToken<List<RecordInfo>>() {}.getType();
        this.recordInfos = (new Gson()).fromJson(pubsubMsg, listType);
        Map<String, String> messageAttributes = new HashMap<>();
        messageAttributes.put(DpsHeaders.DATA_PARTITION_ID, "opendes");
        this.recordChangedMessages = RecordChangedMessages.builder().attributes(messageAttributes).messageId("xxxx").publishTime("2000-01-02T10:10:44+0000").data("{}").build();

        // setup schema
        Map<String, Object> schema = createSchema();
        indexSchemaServiceMock(kind3, schema);
        indexSchemaServiceMock(kind2, schema);
        indexSchemaServiceMock(kind1, null);

        // setup storage records
        Map<String, Object> storageData = new HashMap<>();
        storageData.put("schema1", "test-value");
        List<Records.Entity> validRecords = new ArrayList<>();
        validRecords.add(Records.Entity.builder().id(recordId2).kind(kind2).data(storageData).build());
        validRecords.add(Records.Entity.builder().id(recordId3).kind(kind2).data(storageData).build());
        validRecords.add(Records.Entity.builder().id(recordId4).kind(kind3).data(storageData).build());
        List<ConversionStatus> conversionStatus = new LinkedList<>();
        Records storageRecords = Records.builder().records(validRecords).conversionStatuses(conversionStatus).build();
        when(this.storageService.getStorageRecords(any(), any())).thenReturn(storageRecords);

        // setup elastic, index and mapped document
        when(this.indicesService.createIndex(any(), any(), any(), any(), any())).thenReturn(true);
        when(this.mappingService.getIndexMappingFromRecordSchema(any())).thenReturn(new HashMap<>());

        when(this.elasticClientHandler.createRestClient()).thenReturn(this.restHighLevelClient);
        when(this.restHighLevelClient.bulk(any(), any(RequestOptions.class))).thenReturn(this.bulkResponse);

        Map<String, Object> indexerMappedPayload = new HashMap<>();
        indexerMappedPayload.put("id", "keyword");
        when(this.storageIndexerPayloadMapper.mapDataPayload(any(), any(), any(), any())).thenReturn(indexerMappedPayload);

        BulkItemResponse[] responses = new BulkItemResponse[]{prepareSuccessfulResponse(recordId1), prepareSuccessfulResponse(recordId2),
                prepareSuccessfulResponse(recordId4), prepareFailed400Response(recordId3)};
        when(this.bulkResponse.getItems()).thenReturn(responses);
    }

    private BulkItemResponse prepareFailedResponse(String recordId) {
        BulkItemResponse responseFail = mock(BulkItemResponse.class);
        when(responseFail.isFailed()).thenReturn(true);
        when(responseFail.getFailureMessage()).thenReturn(failureMassage);
        when(responseFail.getId()).thenReturn(recordId);
        when(responseFail.getFailure()).thenReturn(new BulkItemResponse.Failure("failure index", "failure type", "failure id", new Exception("test failure")));
        return responseFail;
    }

    private BulkItemResponse prepareFailed400Response(String recordId) {
        BulkItemResponse responseFail = mock(BulkItemResponse.class);
        when(responseFail.isFailed()).thenReturn(true);
        when(responseFail.getId()).thenReturn(recordId);
        when(responseFail.getFailure()).thenReturn(new BulkItemResponse.Failure("failure index", "failure type", "failure id", new Exception(badRequestMessage), RestStatus.BAD_REQUEST));
        return responseFail;
    }

    private BulkItemResponse prepareSuccessfulResponse(String recordId) {
        BulkItemResponse responseSuccess = mock(BulkItemResponse.class);
        when(responseSuccess.getId()).thenReturn(recordId);
        return responseSuccess;
    }

    private void indexSchemaServiceMock(String kind, Map<String, Object> schema) throws UnsupportedEncodingException, URISyntaxException {
        IndexSchema indexSchema = schema == null
                ? IndexSchema.builder().kind(kind).dataSchema(new HashMap<>()).build()
                : IndexSchema.builder().kind(kind).dataSchema(schema).build();
        when(schemaService.getIndexerInputSchema(kind, new ArrayList<>())).thenReturn(indexSchema);
    }

    private Map<String, Object> createSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("schema1", "keyword");
        schema.put("schema2", "boolean");
        schema.put("schema3", "date");
        schema.put("schema6", "object");
        return schema;
    }
}
