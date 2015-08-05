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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

public class TextGlassClient {
  
  public final static String VERSION = "1.0.0";

  private String domain;
  private String domainVersion;

  //INPUT PARSING
  private List<Transformer> transformers;
  private List<String> tokenSeperators;
  private int ngramConcatSize;

  //PATTERN MATCHING
  private Map<String, List<Pattern>> patterns;
  private Map<String, Attributes> attributes;

  private String defaultId;

  public TextGlassClient() {
    domain = null;
    domainVersion = null;

    transformers = new ArrayList<>();
    tokenSeperators = new ArrayList<>();
    ngramConcatSize = 1;

    patterns = new HashMap<>();
    attributes = new HashMap<>();

    defaultId = null;
  }

  public void loadPatterns(JsonFile patternFile) throws Exception {

    //PARSE PATTERN FILE JSON
    
    if(!patternFile.getType().equals("pattern") && !patternFile.getType().equals("patternPatch")) {
      throw new Exception("Unknown pattern file type: " + patternFile.getType());
    }
    
    boolean patch = false;

    if((patternFile.getType().equals("pattern") && domain != null) ||
        (patternFile.getType().equals("patternPatch") && domain == null)) {

    }

    if(patternFile.getType().equals("pattern")) {
      if(domain != null) {
        throw new Exception("Invalid pattern file type: " + patternFile.getType());
      } else {
        domain = patternFile.getDomain();
      }
    } else if(domain == null) {
      throw new Exception("Invalid pattern file type: " + patternFile.getType());
    } else if(!domain.equals(patternFile.getDomain())) {
      throw new Exception("Domains do not match: " + domain + " != " + patternFile.getDomain());
    } else {
      patch = true;
    }

    if(domainVersion == null) {
      domainVersion = patternFile.getDomainVersion();
    } else if(!domainVersion.equals(patternFile.getDomainVersion())) {
      throw new Exception("DomainVersions do not match: " + domainVersion + " != " + patternFile.getDomainVersion());
    }

    Main.log("Loading pattern domain: " + patternFile.getDomain() + ", version: " +
        patternFile.getDomainVersion() + (patch ? ", patch" : ""), 1);

    //INPUT PARSER

    if(JsonFile.get(patternFile.getJsonNode(), "inputParser").isObject()) {
      JsonNode inputParser = patternFile.getJsonNode().get("inputParser");

      //TRANSFORMERS

      if(JsonFile.get(inputParser, "transformers").isArray()) {
        if(patch) {
          transformers = new ArrayList<>();
        }

        for(int i = 0; i < inputParser.get("transformers").size(); i++) {
          JsonNode transformerNode = inputParser.get("transformers").get(i);

          Transformer transformer = getTransformer(transformerNode);

          transformers.add(transformer);

          Main.log("Found transformer: " + transformer, 2);
        }
      }

      Main.log("Found " + transformers.size() + " transformer(s)", 1);

      //TOKEN SEPERATORS

      if(JsonFile.get(inputParser, "tokenSeperators").isArray()) {
        if(patch) {
          tokenSeperators = new ArrayList<>();
        }
        
        for(int i = 0; i < inputParser.get("tokenSeperators").size(); i++) {
          JsonNode tokenSeperatorNode = inputParser.get("tokenSeperators").get(i);

          if(tokenSeperatorNode.asText().isEmpty()) {
            throw new Exception("Empty tokenSeperator not allowed");
          }

          String tokenSeperator = tokenSeperatorNode.asText();

          tokenSeperators.add(tokenSeperator);

          Main.log("Found tokenSeperator: '" + tokenSeperator + "'", 2);
        }

        Main.log("Found " + tokenSeperators.size() + " tokenSeperator(s)", 1);
      }

      //NGRAM SIZE

      if(inputParser.get("ngramConcatSize") != null) {
        String ngramConcatSizeStr = inputParser.get("ngramConcatSize").asText();
        ngramConcatSize = Integer.parseInt(ngramConcatSizeStr);

        if(ngramConcatSize < 1) {
          throw new Exception("Invalid value of ngramConcatSize: " + ngramConcatSize);
        }

        Main.log("Found ngramConcatSize: " + ngramConcatSize, 2);
      }
    }

    //PATTERN SET

    int simpleHashCount = 0;

    if(JsonFile.get(patternFile.getJsonNode(), "patternSet").isObject()) {
      JsonNode patternSet = patternFile.getJsonNode().get("patternSet");

      if(!JsonFile.empty(patternSet, "defaultId")) {
        defaultId = patternSet.get("defaultId").asText();

        Main.log("Found defaultId: " + defaultId, 2);
      }

      if(patternSet.get("simpleHashCount") != null) {
        String simpleHashCountStr = patternSet.get("simpleHashCount").asText();
        simpleHashCount = Integer.parseInt(simpleHashCountStr);

        if(simpleHashCount < 1) {
          throw new Exception("Invalid value of simpleHashCount: " + simpleHashCount);
        }

        if(!patch) {
          patterns = new HashMap<>(simpleHashCount);
        }
      }

      //PATTERNS

      int patternCount = 0;

      if(JsonFile.get(patternSet, "patterns").isArray()) {
        for(int i = 0; i < patternSet.get("patterns").size(); i++) {
          JsonNode patternNode = patternSet.get("patterns").get(i);

          Pattern pattern = new Pattern(patternNode);

          for(String patternToken : pattern.getPatternTokens()) {
            List<Pattern> tokenPatterns = patterns.get(patternToken);

            if(tokenPatterns == null) {
              tokenPatterns = new ArrayList<>();
            }

            tokenPatterns.add(pattern);

            patterns.put(patternToken, tokenPatterns);
          }

          Main.log(pattern.toStringFull(), 3);

          patternCount++;
        }
      }

      if(patternCount == 0 && defaultId == null) {
        throw new Exception("No patterns found");
      }

      if(simpleHashCount > 0 && (patternCount != simpleHashCount)) {
        throw new Exception("Bad simpleHashCount value, found: " + simpleHashCount + ", expected: " + patternCount);
      }

      Main.log("Found " + patternCount + " pattern(s), total: " + patterns.size(), 1);
    }

    //OPTIONAL ATTRIBUTES IN PATTERN FILE
    
    if(JsonFile.get(patternFile.getJsonNode(), "attributes").isArray()) {
      loadAttributes(patternFile);
    }
  }

  public void loadAttributes(JsonFile attributeFile) throws Exception {

    //PARSE ATTRIBUTE FILE JSON
    
    if(!attributeFile.getType().equals("pattern") && !attributeFile.getType().equals("attribute") &&
        !attributeFile.getType().equals("patternPatch") && !attributeFile.getType().equals("attributePatch")) {
      throw new Exception("Unknown pattern file type: " + attributeFile.getType());
    }

    if(!attributeFile.getDomain().equals(domain)) {
      throw new Exception("Domains do not match: " + domain + " != " + attributeFile.getDomain());
    }

    if((attributeFile.getType().contains("Patch") && attributes.isEmpty()) ||
        (!attributeFile.getType().contains("Patch") && !attributes.isEmpty())) {
      throw new Exception("Invalid attribute file type: " + attributeFile.getType());
    }

    if(!attributeFile.getDomainVersion().equals(domainVersion)) {
      throw new Exception("DomainVersions do not match: " + domainVersion + " != " + attributeFile.getDomainVersion());
    }

    Main.log("Loading attributes: " + attributeFile.getDomain() + ", version: " + attributeFile.getDomainVersion(), 1);

    //ATTRIBUTES

    int attributeCount = 0;

    if(JsonFile.get(attributeFile.getJsonNode(), "attributes").isArray()) {
      for(int i = 0; i < attributeFile.getJsonNode().get("attributes").size(); i++) {
        JsonNode attributeNode = attributeFile.getJsonNode().get("attributes").get(i);

        Attributes patternAttributes = new Attributes(attributeNode);

        Main.log(patternAttributes.toString(), 3);

        attributes.put(patternAttributes.getPatternId(), patternAttributes);

        attributeCount++;
      }
    }

    if(attributeCount == 0) {
      throw new Exception("No attributes found");
    }

    Main.log("Found " + attributeCount + " attributes(s), total: " + attributes.size(), 1);
  }

  public Map<String, String> classify(String input) throws Exception {
    if(input == null) {
      input = "";
    }

    Main.log("Classify: '" + input + "'", 2);

    //TRANFORM THE INPUT

    String transformed = input;

    for(Transformer transformer : transformers) {
      transformed = transformer.transform(transformed);
    }

    Main.log("Transformed: '" + transformed + "'", 3);

    //TOKENIZE THE INPUT

    List<String> tokens = split(transformed, tokenSeperators);

    Main.log("Tokens: " + tokens, 3);

    //NGRAM THE INPUT

    List<String> ngramTokenStream = new ArrayList<>();

    for(int i = 0; i < tokens.size(); i++) {
      String ngram = "";
      List<String> ngramParts = new ArrayList<>();

      for(int size = ngramConcatSize; size > 0 && i + ngramConcatSize - size < tokens.size(); size--) {
        ngram += tokens.get(i + ngramConcatSize - size);

        ngramParts.add(0, ngram);
      }

      ngramTokenStream.addAll(ngramParts);

      ngramParts.clear();
    }

    Main.log("Ngrams: " + ngramTokenStream, 3);

    //MATCH THE TOKEN STREAM AGAINST THE PATTERNS

    List<String> matchedTokens = new ArrayList<>();
    List<Pattern> candidates = new ArrayList<>();
    Pattern winner = null;

    for(String token : ngramTokenStream) {
      List<Pattern> matched = patterns.get(token);

      if(matched != null) {
        matchedTokens.add(token);

        for(Pattern match : matched) {
          if(!candidates.contains(match)) {
            candidates.add(match);
          }
        }

        Main.log("Hit: " + token + ", candidates: " + matched, 3);
      }
    }

    //FIND THE WINNER

    if(winner == null) {
      for(Pattern candidate : candidates) {
        if(candidate.isValid(matchedTokens)) {
          Main.log("Candidate: " + candidate.toStringRank(matchedTokens), 3);
          
          if(winner == null) {
            winner = candidate;
          } else if(candidate.getRank() > winner.getRank()) {
            winner = candidate;
          } else if(candidate.getRank() == winner.getRank() &&
              candidate.getMatchedLength(matchedTokens) > winner.getMatchedLength(matchedTokens)) {
            winner = candidate;
          }
        }
      }
    }
    
    Main.log("Winner: " + (winner == null ? "null" : winner.toStringFull()), 3);

    //RETURN THE RESULT

    if(winner == null) {
      if(defaultId != null) {
        return getPatternAttributes(defaultId, input);
      } else {
        return null;
      }
    }
    
    return getPatternAttributes(winner.getPatternId(), input);
  }

  private Map<String, String> getPatternAttributes(String patternId, String input) {
    Attributes patternAttributes = attributes.get(patternId);

    //FOUND ATTRIBUTE MAP

    if(patternAttributes != null) {
      Map<String, String> attributeMap = patternAttributes.getAttributes(input);
      Attributes parent = patternAttributes;

      //COPY PARENT ATTRIBUTES

      if(patternAttributes.getParentId() != null) {
        attributeMap = new HashMap<>(attributeMap);
      }

      while(parent.getParentId() != null) {
        parent = attributes.get(parent.getParentId());

        if(parent == null) {
          break;
        }

        Map<String, String> parentMap = parent.getAttributes(input);

        for(String key : parentMap.keySet()) {
          if(!attributeMap.containsKey(key)) {
            attributeMap.put(key, parentMap.get(key));
          }
        }
      }

      Main.log("Attribute map: " + attributeMap, 3);

      return attributeMap;
    }

    //NO ATTRIBUTES, RETURN MAP WITH JUST PATTERN ID
    Map<String, String> custom = new HashMap<>();

    custom.put("patternId", patternId);

    Main.log("Attribute map: " + custom, 3);

    return Collections.unmodifiableMap(custom);
  }

  //SPLIT A STRING
  public static List<String> split(String source, List<String> tokenSeperators) {
    List<String> tokens = new ArrayList<>();
    int sourcePos = 0;
    int destStart = 0;
    int destEnd = 0;

    source:
    while(sourcePos < source.length()) {
      seperator:
      for(String seperator : tokenSeperators) {
        int i;

        for(i = 0; i < seperator.length(); i++) {
          if(sourcePos + i >= source.length() || source.charAt(sourcePos + i) != seperator.charAt(i)) {
            continue seperator;
          }
        }

        if(destEnd - destStart > 0) {
          tokens.add(source.substring(destStart, destEnd));
        }

        sourcePos += i;
        destStart = destEnd = sourcePos;

        continue source;
      }

      sourcePos++;
      destEnd++;
    }

    if(destEnd - destStart > 0) {
      tokens.add(source.substring(destStart, destEnd));
    }

    return tokens;
  }

  //GET A TRANSFORMER
  public static Transformer getTransformer(JsonNode transformer) throws Exception {
    String type = JsonFile.get(transformer, "type").asText();
    JsonNode parameters = JsonFile.get(transformer, "parameters");

    if(type.equals("LowerCase")) {
      return new TransformerLowerCase();
    } else if(type.equals("UpperCase")) {
      return new TransformerUpperCase();
    } else if(type.equals("ReplaceFirst")) {
      return new TransformerReplaceFirst(parameters);
    } else if(type.equals("ReplaceAll")) {
      return new TransformerReplaceAll(parameters);
    } else if(type.equals("SplitAndGet")) {
      return new TransformerSplitAndGet(parameters);
    } else if(type.equals("IsNumber")) {
      return new TransformerIsNumber();
    } else if(type.equals("Substring")) {
      return new TransformerSubstring(parameters);
    }

    throw new Exception("Transformer not found: " + type);
  }

  public String getDomain() {
    return domain;
  }

  public String getDomainVersion() {
    return domainVersion;
  }

}