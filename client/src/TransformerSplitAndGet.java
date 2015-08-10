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

public class TransformerSplitAndGet implements Transformer {
  private final List<String> delimeter;
  private final int get;

  public TransformerSplitAndGet(JsonNode json) throws Exception {
    if(JsonFile.empty(json, "delimeter")) {
      throw new Exception("SplitAndGet delimeter not defined");
    }

    if(JsonFile.empty(json, "get")) {
      throw new Exception("SplitAndGet get not defined");
    }

    delimeter = new ArrayList<>();
    delimeter.add(json.get("delimeter").asText());
    
    get = Integer.parseInt(json.get("get").asText());

    if(get < -1) {
      throw new Exception("Invalid value of get for SplitAndGet: " + get);
    }
  }

  @Override
  public String transform(String input) throws Exception {
    List<String> parts = TextGlassClient.split(input, delimeter);
    
    int i = get;

    if(get == -1) {
      i = parts.size() - 1;
    }

    if(i < 0 || i >= parts.size()) {
      throw new Exception("SplitAndGet index out of range: " + i);
    }

    return parts.get(i);
  }

  @Override
  public String toString() {
    return "SplitAndGet  delimeter: '" + delimeter + "'  get: " + get;
  }
}
