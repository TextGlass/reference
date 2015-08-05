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

import java.io.File;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class JsonFile {

  private static JsonNode nullNode = null;
  
  private final JsonNode json;

  private final String type;
  private final String domain;
  private final String domainVersion;

  public JsonFile(String path) throws Exception {

    //LOAD A JSON FILE AND VALIDATE IT
    
    ObjectMapper mapper = new ObjectMapper();
    json = mapper.readTree(new File(path));

    if(!json.isObject()) {
      throw new Exception("JsonFile is not an object");
    }

    if(get(json, "specVersion").asDouble(0d) != 1.0d) {
      throw new Exception("Bad specVersion found: " + get(json, "specVersion").asDouble(0d));
    }

    if(empty(json, "type")) {
      throw new Exception("type not defined");
    }

    if(empty(json, "domain")) {
      throw new Exception("domain not defined");
    }

    if(empty(json, "domainVersion")) {
      throw new Exception("domainVersion not defined");
    }

    type = json.get("type").asText();
    domain = json.get("domain").asText();
    domainVersion = json.get("domainVersion").asText();
  }

  //GET HELPER, RETURN A NULL NODE IF NOT FOUND
  public static JsonNode get(JsonNode node, String name) throws Exception {
    JsonNode ret = null;
    
    if(node != null) {
      ret = node.get(name);
    }

    if(ret != null) {
      return ret;
    }

    if(nullNode == null) {
      ObjectMapper mapper = new ObjectMapper();
      nullNode = mapper.readTree("null");
    }

    return nullNode;
  }

  //EMPTY HELPER
  public static boolean empty(JsonNode node, String name) {
    return (node == null || node.get(name) == null || node.get(name).asText().isEmpty());
  }

  public String getType() {
    return type;
  }

  public String getDomain() {
    return domain;
  }

  public String getDomainVersion() {
    return domainVersion;
  }

  public JsonNode getJsonNode() {
    return json;
  }
}
