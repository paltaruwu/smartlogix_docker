package com.storechain.kafkaadmin;

import com.storechain.kafkaadmin.dto.TopicRequest;
import com.storechain.kafkaadmin.exception.BusinessRuleException;
import com.storechain.kafkaadmin.service.KafkaAdminService;
import org.apache.kafka.clients.admin.Admin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaAdminApplicationTests {

    private KafkaAdminService service() {
        KafkaAdminService service = new KafkaAdminService();
        ReflectionTestUtils.setField(service, "admin", Mockito.mock(Admin.class));
        return service;
    }

    @Test
    void rejectsTopicWithoutName() {
        assertThrows(BusinessRuleException.class, () -> service().createTopic(new TopicRequest()));
    }

    @Test
    void rejectsTopicWithInvalidPartitions() {
        TopicRequest request = new TopicRequest();
        request.setName("test");
        request.setPartitions(0);
        assertThrows(BusinessRuleException.class, () -> service().createTopic(request));
    }
}
