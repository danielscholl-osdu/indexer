package org.opengroup.osdu.indexer.azure.publish;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.servicebus.ITopicClientFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.springframework.test.util.ReflectionTestUtils;
import javax.inject.Inject;
import javax.inject.Named;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PublisherImplTest {

    private static String serviceBusTopicField = "serviceBusTopic";
    private static String serviceBusTopicValue = "recordChangeTopic";
    private static String shouldPublishToServiceBusTopicField = "shouldPublishToServiceBusTopic";
    private static Boolean shouldPublishToServiceBusTopicValue = true;
    private static String partitionId = "opendes";

    @Mock
    public ITopicClientFactory topicClientFactory;

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    private JobStatus jobStatus;

    @InjectMocks
    public PublisherImpl sut;

    @Test
    public void should_invoke_getPartitionIdOfdpsHeaders_when_publishStatusChangedTagsToTopic_isCalled() throws Exception {
        ReflectionTestUtils.setField(sut,serviceBusTopicField,serviceBusTopicValue);
        ReflectionTestUtils.setField(sut,shouldPublishToServiceBusTopicField,shouldPublishToServiceBusTopicValue);
        when(dpsHeaders.getPartitionId()).thenReturn(partitionId);

        sut.publishStatusChangedTagsToTopic(dpsHeaders, jobStatus);

        verify(dpsHeaders, times(3)).getPartitionId();
    }

    @Test
    public void should_invoke_getAccountIdOfDpsHeaders_when_publishStatusChangedTagsToTopic_isCalledWithGetPartitionIdReturningEmptyString() throws Exception {
        ReflectionTestUtils.setField(sut,serviceBusTopicField,serviceBusTopicValue);
        ReflectionTestUtils.setField(sut,shouldPublishToServiceBusTopicField,shouldPublishToServiceBusTopicValue);
        when(dpsHeaders.getPartitionId()).thenReturn("");

        sut.publishStatusChangedTagsToTopic(dpsHeaders, jobStatus);

        verify(dpsHeaders, times(1)).getAccountId();
    }

    @Test
    public void should_invoke_getClientOftopicClientFactory_when_publishStatusChangedTagsToTopic_isCalled() throws Exception {
        ReflectionTestUtils.setField(sut,serviceBusTopicField,serviceBusTopicValue);
        ReflectionTestUtils.setField(sut,shouldPublishToServiceBusTopicField,shouldPublishToServiceBusTopicValue);
        when(dpsHeaders.getPartitionId()).thenReturn(partitionId);

        sut.publishStatusChangedTagsToTopic(dpsHeaders, jobStatus);

        verify(topicClientFactory, times(1)).getClient(partitionId, serviceBusTopicValue);
    }
}
