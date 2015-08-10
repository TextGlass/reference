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

import org.codehaus.jackson.JsonNode;

public class TransformerReplaceFirst implements Transformer {
  private final String find;
  private final String replaceWith;

  public TransformerReplaceFirst(JsonNode json) throws Exception {
    if(JsonFile.empty(json, "find")) {
      throw new Exception("TransformerReplaceFirst find not defined");
    }

    if(json.get("replaceWith") == null) {
      throw new Exception("TransformerReplaceFirst replaceWith not defined");
    }

    find = json.get("find").asText();
    replaceWith = json.get("replaceWith").asText();
  }

  @Override
  public String transform(String input) {
    int pos = input.indexOf(find);

    if(pos > 0) {
      return input.substring(0, pos) + replaceWith + input.substring(pos + find.length());
    }

    return input;
  }

  @Override
  public String toString() {
    return "TransformerReplaceFirst  find: '" + find + "'  replaceWith: '" + replaceWith + "'";
  }
}
