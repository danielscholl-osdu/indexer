package org.opengroup.osdu.common;

import com.google.gson.Gson;

import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.models.Setup;
import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.response.ResponseBase;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.HTTPClient;

import com.sun.jersey.api.client.ClientResponse;
import cucumber.api.Scenario;
import lombok.extern.java.Log;
import org.opengroup.osdu.util.IndexerClientUtil;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opengroup.osdu.util.Config.*;

@Log
public abstract class TestsBase {
    protected HTTPClient httpClient;
    protected Scenario scenario;
    protected Map<String, String> tenantMap = new HashMap<>();
    protected Map<String, TestIndex> inputRecordMap = new HashMap<>();
    protected ElasticUtils elasticUtils;
    protected IndexerClientUtil indexerClientUtil;

    public TestsBase(HTTPClient httpClient) {
        this.httpClient = httpClient;
        tenantMap.put("tenant1", getDataPartitionIdTenant1());
        tenantMap.put("tenant2", getDataPartitionIdTenant2());
        tenantMap.put("common", "common");

        elasticUtils = new ElasticUtils();
        this.indexerClientUtil = new IndexerClientUtil(this.httpClient);
    }

    public TestsBase(HTTPClient httpClient, ElasticUtils elasticUtils) {
        this.httpClient = httpClient;
        tenantMap.put("tenant1", getDataPartitionIdTenant1());
        tenantMap.put("tenant2", getDataPartitionIdTenant2());
        tenantMap.put("common", "common");

        this.elasticUtils = elasticUtils;
        this.indexerClientUtil = new IndexerClientUtil(this.httpClient);
    }

    protected TestIndex getTextIndex(){
        return new TestIndex(elasticUtils);
    }

    protected void setUp(List<Setup> inputList, String timeStamp) {
        for (Setup input : inputList) {
            TestIndex testIndex = getTextIndex();
            testIndex.setHttpClient(httpClient);
            testIndex.setIndex(generateActualName(input.getIndex(), timeStamp));
            testIndex.setKind(generateActualName(input.getKind(), timeStamp));
            testIndex.setMappingFile(input.getMappingFile());
            testIndex.setRecordFile(input.getRecordFile());
            List<String> dataGroup = new ArrayList<>();
            String[] viewerGroup = input.getViewerGroup().split(",");
            for (int i = 0; i < viewerGroup.length; i++) {
                viewerGroup[i] = generateActualName(viewerGroup[i], timeStamp) + "." + getEntitlementsDomain();
                dataGroup.add(viewerGroup[i]);
            }
            String[] ownerGroup = input.getOwnerGroup().split(",");
            for (int i = 0; i < ownerGroup.length; i ++) {
                ownerGroup[i] = generateActualName(ownerGroup[i], timeStamp) + "." + getEntitlementsDomain();
                if (dataGroup.indexOf(ownerGroup[i]) > 0) {
                    dataGroup.add(ownerGroup[i]);
                }
            }
            testIndex.setViewerGroup(viewerGroup);
            testIndex.setOwnerGroup(ownerGroup);
            testIndex.setDataGroup(dataGroup.toArray(new String[dataGroup.size()]));
            inputRecordMap.put(testIndex.getKind(), testIndex);
        }
        /******************One time setup for whole feature**************/
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                tearDown();
            }
        });
        for (String kind : inputRecordMap.keySet()) {
            TestIndex testIndex = inputRecordMap.get(kind);
            testIndex.setupIndex();
        }

    }

    /******************One time cleanup for whole feature**************/
    public void tearDown() {
        for (String kind : inputRecordMap.keySet()) {
            TestIndex testIndex = inputRecordMap.get(kind);
            testIndex.cleanupIndex(kind);
        }
    }

    protected abstract String getApi();

    protected abstract String getHttpMethod();

    protected <T extends ResponseBase> T executeQuery(String api, String payLoad, Map<String, String> headers, String token, Class<T> typeParameterClass) {
        ClientResponse clientResponse = httpClient.send(this.getHttpMethod(), api, payLoad, headers, token);
        logCorrelationIdWithFunctionName(clientResponse.getHeaders());
        return getResponse(clientResponse, typeParameterClass);
    }

    private <T extends ResponseBase> T getResponse(ClientResponse clientResponse, Class<T> typeParameterClass) {
        log.info(String.format("Response status: %s, type: %s", clientResponse.getStatus(), clientResponse.getType().toString()));
        assertTrue(clientResponse.getType().toString().contains(MediaType.APPLICATION_JSON));
        String responseEntity = clientResponse.getEntity(String.class);

        T response = new Gson().fromJson(responseEntity, typeParameterClass);
        response.setHeaders(clientResponse.getHeaders());
        response.setResponseCode(clientResponse.getStatus());
        return response;
    }


    private void logCorrelationIdWithFunctionName(MultivaluedMap<String, String> headers) {
        log.info(String.format("Scenario Name: %s, Correlation-Id: %s", scenario.getId(), headers.get("correlation-id")));
    }

    protected String generateActualName(String rawName, String timeStamp) {
        for (Map.Entry<String, String> tenant : tenantMap.entrySet()) {
            rawName = rawName.replaceAll(tenant.getKey(), tenant.getValue());
        }
        return rawName.replaceAll("<timestamp>", timeStamp);
    }

    protected String generateActualId(String rawName, String timeStamp, String kind) {
        
        rawName = generateActualName(rawName, timeStamp);

        String kindSubType = kind.split(":")[2];

        return rawName.replaceAll("<kindSubType>", kindSubType);

    }


    public String generateActualNameWithoutTs(String rawName) {
        for (Map.Entry<String, String> tenant : tenantMap.entrySet()) {
            rawName = rawName.replaceAll(tenant.getKey(), tenant.getValue());
        }
        return rawName.replaceAll("<timestamp>", "");
    }

    public String generateActualIdWithoutTs(String rawName, String kind) {
        rawName = generateActualNameWithoutTs(rawName);

        String kindSubType = kind.split(":")[2];

        return rawName.replaceAll("<kindSubType>", kindSubType);
    }

    protected Legal generateLegalTag() {
        Legal legal = new Legal();
        Set<String> legalTags = new HashSet<>();
        legalTags.add(getLegalTag());
        legal.setLegaltags(legalTags);
        Set<String> otherRelevantCountries = new HashSet<>();
        otherRelevantCountries.add(getOtherRelevantDataCountries());
        legal.setOtherRelevantDataCountries(otherRelevantCountries);
        return legal;
    }
}
