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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;

public class AttributeTransformer {
  private final String defaultValue;
  private final List<Transformer> transformers;

  public AttributeTransformer(JsonNode json) throws Exception {

    //PARSE ATTRIBUTE TRANSFORMER JSON

    String defaultValueStr = "";

    if(!JsonFile.empty(json, "defaultValue")) {
      defaultValueStr = json.get("defaultValue").asText();
    }

    defaultValue = defaultValueStr;
    transformers = new ArrayList<>();

    //PARSE TRANSFORMER LIST

    if(JsonFile.get(json, "transformers").isArray()) {
      for(int k = 0; k < json.get("transformers").size(); k++) {
        JsonNode transformerNode = json.get("transformers").get(k);

        Transformer transformer = TextGlassClient.getTransformer(transformerNode);

        transformers.add(transformer);
      }
    }

    if(transformers.isEmpty()) {
      throw new Exception("No transformers defined for attributeTranformer");
    }
  }

  public String getValue(String input) throws Exception {

    //TRANSFORM INPUT
    
    for(Transformer transformer : transformers) {
      input = transformer.transform(input);
    }

    return input;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  @Override
  public String toString() {
    return "{defaultValue: '" + defaultValue + "' transformer(s): " + transformers + "}";
  }
}
