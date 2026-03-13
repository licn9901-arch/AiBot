package com.deskpet.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicAclTest {

    @Test
    void subscribeAcl_allowsCmdAndRespOnly() {
        assertTrue(TopicAcl.isValidSubscribe("pet-1", "pet/pet-1/cmd"));
        assertTrue(TopicAcl.isValidSubscribe("pet-1", "pet/pet-1/resp"));
        assertFalse(TopicAcl.isValidSubscribe("pet-1", "pet/pet-1/telemetry"));
    }

    @Test
    void publishAcl_allowsTelemetryAckEventAndReqOnly() {
        assertTrue(TopicAcl.isValidPublish("pet-1", "pet/pet-1/telemetry"));
        assertTrue(TopicAcl.isValidPublish("pet-1", "pet/pet-1/cmd/ack"));
        assertTrue(TopicAcl.isValidPublish("pet-1", "pet/pet-1/event"));
        assertTrue(TopicAcl.isValidPublish("pet-1", "pet/pet-1/req"));
        assertFalse(TopicAcl.isValidPublish("pet-1", "pet/pet-1/resp"));
    }

    @Test
    void downlinkAcl_allowsCmdAndRespOnly() {
        assertTrue(TopicAcl.isValidDownlink("pet-1", "pet/pet-1/cmd"));
        assertTrue(TopicAcl.isValidDownlink("pet-1", "pet/pet-1/resp"));
        assertFalse(TopicAcl.isValidDownlink("pet-1", "pet/pet-1/event"));
    }

    @Test
    void callbackPath_mapsReqToInternalRequest() {
        assertEquals("/internal/telemetry/", TopicAcl.resolveCallbackPath("pet/pet-1/telemetry"));
        assertEquals("/internal/event/", TopicAcl.resolveCallbackPath("pet/pet-1/event"));
        assertEquals("/internal/request/", TopicAcl.resolveCallbackPath("pet/pet-1/req"));
        assertEquals("/internal/ack/", TopicAcl.resolveCallbackPath("pet/pet-1/cmd/ack"));
    }
}
