/*
 * Copyright 2017 Splunk, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.splunk.kafka.connect;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class JacksonStructModule extends SimpleModule {
  public JacksonStructModule() {
    addSerializer(Struct.class, new StructSerializer());
  }

  static class StructSerializer extends JsonSerializer<Struct> {
    @Override
    public void serialize(Struct struct, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
      final Map<String, Object> result = new LinkedHashMap<>(struct.schema().fields().size());
      for (Field field : struct.schema().fields()) {
        result.put(field.name(), struct.get(field));
      }
      jsonGenerator.writeObject(result);
    }
  }
}
