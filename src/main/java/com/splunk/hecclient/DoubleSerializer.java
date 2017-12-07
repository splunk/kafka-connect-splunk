package com.splunk.hecclient;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Created by kchen on 12/7/17.
 */
public class DoubleSerializer extends JsonSerializer<Double> {
    @Override
    public void serialize(Double value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        String d = new BigDecimal(value).setScale(6, BigDecimal.ROUND_HALF_UP).toPlainString();
        jgen.writeNumber(d);
    }
}
