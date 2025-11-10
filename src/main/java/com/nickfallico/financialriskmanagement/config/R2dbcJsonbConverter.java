package com.nickfallico.financialriskmanagement.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class R2dbcJsonbConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @WritingConverter
    public static class MapToJsonConverter implements Converter<Map<String, Object>, Json> {
        @Override
        public Json convert(Map<String, Object> source) {
            try {
                return Json.of(objectMapper.writeValueAsString(source));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Error converting Map to JSON", e);
            }
        }
    }

    @ReadingConverter
    public static class JsonToMapConverter implements Converter<Json, Map<String, Object>> {
        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> convert(Json source) {
            try {
                return objectMapper.readValue(source.asString(), Map.class);
            } catch (IOException e) {
                throw new IllegalArgumentException("Error converting JSON to Map", e);
            }
        }
    }

    public static R2dbcCustomConversions customConversions() {
        List<Object> converters = Arrays.asList(
            new MapToJsonConverter(),
            new JsonToMapConverter()
        );
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, converters);
    }
}