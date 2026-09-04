package com.transit.ticketing.contracts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class EventJson {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    private EventJson() {}
    public static String toJson(Object object) throws JsonProcessingException { return MAPPER.writeValueAsString(object); }
    public static String toPrettyJson(Object object) throws JsonProcessingException { return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object); }
    public static <T> T fromJson(String json, TypeReference<T> type) throws JsonProcessingException { return MAPPER.readValue(json, type); }
}
