/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

import org.codehaus.jackson.JsonNode;

public class TransformerSubstring implements Transformer {
  private final int start;
  private final int maxLength;

  public TransformerSubstring(JsonNode json) throws Exception {
    if(JsonFile.empty(json, "start")) {
      throw new Exception("TransformerSubstring start not defined");
    }

    start = Integer.parseInt(json.get("start").asText());

    if(start < 0) {
      throw new Exception("Invalid value of start for TransformerSubstring: " + start);
    }

    String maxLengthStr = "-1";

    if(!JsonFile.empty(json, "maxLength")) {
      maxLengthStr = json.get("maxLength").asText();

      if(Integer.parseInt(maxLengthStr) < 0) {
        throw new Exception("Invalid value of maxLength for TransformerSubstring: " + maxLengthStr);
      }
    }

    maxLength = Integer.parseInt(maxLengthStr);
  }

  @Override
  public String transform(String input) throws Exception {
    if(start >= input.length()) {
      throw new Exception("start position out of range: " + start);
    }

    if(maxLength >= 0 && (maxLength + start) <= input.length()) {
      return input.substring(start, maxLength + start);
    }

    return input.substring(start);
  }

  @Override
  public String toString() {
    return "TransformerSubstring  start: '" + start + (maxLength >= 0 ? "'  maxLength: " + maxLength : "");
  }
}
