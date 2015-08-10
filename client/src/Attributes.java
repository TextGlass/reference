/*
 * Copyright (c) 2015 TextGlass
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

public class Attributes {
  private final String patternId;
  private final String parentId;
  private final Map<String, String> attributes;
  private final Map<String, AttributeTransformer> attributeTransformers;

  public Attributes(String patternId, JsonNode json) throws Exception {

    //PARSE ATTRIBUTE JSON

    this.patternId = patternId;
    parentId = (JsonFile.empty(json, "parentId") ? null : json.get("parentId").asText());
    attributeTransformers = new HashMap<>();

    //ATTRIBUTE MAP

    Map<String, String> attributeMap = new HashMap<>();

    if(JsonFile.get(json, "attributes").isObject()) {
      for(Iterator<String> j = json.get("attributes").getFieldNames(); j.hasNext();) {
        String key = j.next();
        String value = json.get("attributes").get(key).asText();

        if(key.isEmpty()) {
          throw new Exception("Empty attribute name not allowed for " + patternId);
        }

        if(key.equals("patternId")) {
          throw new Exception("patternId is a reserved attribute name");
        }

        if(key.endsWith("_error")) {
          String ekey = key.substring(0, key.length() - 6);
          if(attributeMap.containsKey(ekey)) {
            throw new Exception("This attribute is reserved: " + key);
          }
        } else if(attributeMap.containsKey(key + "_error")) {
          throw new Exception("This attribute is reserved: " + key + "_error");
        }

        attributeMap.put(key, value);
      }
    }

    attributeMap.put("patternId", patternId);

    //ATTRIBUTE TRANFORMERS

    if(JsonFile.get(json, "attributeTransformers").isObject()) {
      for(Iterator<String> j = json.get("attributeTransformers").getFieldNames(); j.hasNext();) {
        String key = j.next();
        JsonNode attributeTransformerNode = json.get("attributeTransformers").get(key);

        AttributeTransformer attributeTransformer = new AttributeTransformer(attributeTransformerNode);

        attributeTransformers.put(key, attributeTransformer);
      }

      if(attributeTransformers.isEmpty()) {
        throw new Exception("No attribute transformers found for " + patternId);
      }
    }

    attributes = Collections.unmodifiableMap(attributeMap);
  }

  public Map<String, String> getAttributes(String input) {

    //RETURN MAP
    
    if(attributeTransformers.isEmpty()) {
      return attributes;
    }

    //ADD TRANSFORMED ATTRIBUTES
    
    Map<String, String> custom = new HashMap<>(attributes);

    for(String key : attributeTransformers.keySet()) {
      AttributeTransformer attributeTransformer = attributeTransformers.get(key);

      try {
        String value = attributeTransformer.getValue(input);

        custom.put(key, value);
      } catch(Exception e) {
        custom.put(key, attributeTransformer.getDefaultValue());
        custom.put(key + "_error", e.toString());
      }
    }

    return Collections.unmodifiableMap(custom);
  }

  @Override
  public String toString() {
    return "Attribute patternId: " + patternId + ", attribute(s): " + attributes +
        (attributeTransformers.size() > 0 ? " attributeTransformer(s): " + attributeTransformers : "");
  }

  public String getPatternId() {
    return patternId;
  }

  public String getParentId() {
    return parentId;
  }
}
