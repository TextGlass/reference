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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
  private final Map<String, List<Pattern>> patterns;
  private final Map<String, Attributes> attributes;

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

  public void load(JsonFile patternFile, JsonFile patternPatchFile,
      JsonFile attributeFile, JsonFile attributePatchFile) throws Exception
  {

    if(domain != null) {
      throw new Exception("Client has already been initialized");
    }

    //GET THE DOMAIN
    
    if(!patternFile.getType().equals("pattern")) {
      throw new Exception("Unknown pattern file type: " + patternFile.getType());
    }

    domain = patternFile.getDomain();
    domainVersion = patternFile.getDomainVersion();

    //VALIDATE PATTERN PATCH FILE

    if(patternPatchFile != null) {
      if(!patternPatchFile.getType().equals("patternPatch")) {
        throw new Exception("Unknown pattern patch file type: " + patternPatchFile.getType());
      }

      if(!domain.equals(patternPatchFile.getDomain())) {
        throw new Exception("Domains do not match: " + domain + " != " + patternPatchFile.getDomain());
      }

      if(!domainVersion.equals(patternPatchFile.getDomainVersion())) {
        throw new Exception("Versions do not match: " + domainVersion + " != " + patternPatchFile.getDomainVersion());
      }
    }

    //VALIDATE ATTRIBUTE FILE

    if(attributeFile != null) {
      if(!attributeFile.getType().equals("attribute")) {
        throw new Exception("Unknown attribute file type: " + attributeFile.getType());
      }

      if(!domain.equals(attributeFile.getDomain())) {
        throw new Exception("Domains do not match: " + domain + " != " + attributeFile.getDomain());
      }

      if(!domainVersion.equals(attributeFile.getDomainVersion())) {
        throw new Exception("Versions do not match: " + domainVersion + " != " + attributeFile.getDomainVersion());
      }
    }

    //VALIDATE ATTRIBUTE PATCH FILE

    if(attributePatchFile != null) {
      if(!attributePatchFile.getType().equals("attributePatch")) {
        throw new Exception("Unknown attribute patch file type: " + attributePatchFile.getType());
      }

      if(!domain.equals(attributePatchFile.getDomain())) {
        throw new Exception("Domains do not match: " + domain + " != " + attributePatchFile.getDomain());
      }

      if(!domainVersion.equals(attributePatchFile.getDomainVersion())) {
        throw new Exception("Versions do not match: " + domainVersion + " != " + attributePatchFile.getDomainVersion());
      }
    }

    //LOAD THE FILES

    loadPatterns(patternFile, false);

    if(patternPatchFile != null) {
      loadPatterns(patternPatchFile, true);
    }

    if(attributeFile != null) {
      loadAttributes(attributeFile);
    }

    if(attributePatchFile != null) {
      loadAttributes(attributePatchFile);
    }
  }

  private void loadPatterns(JsonFile patternFile, boolean patch) throws Exception {
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
    
    if(JsonFile.get(patternFile.getJsonNode(), "attributes").isObject()) {
      loadAttributes(patternFile);
    }
  }

  private void loadAttributes(JsonFile attributeFile) throws Exception {
    Main.log("Loading attributes: " + attributeFile.getDomain() +
        ", version: " + attributeFile.getDomainVersion(), 1);

    //ATTRIBUTES

    int attributeCount = 0;

    if(JsonFile.get(attributeFile.getJsonNode(), "attributes").isObject()) {
      JsonNode attributesJson = attributeFile.getJsonNode().get("attributes");
      for(Iterator<String> i = attributesJson.getFieldNames(); i.hasNext();) {
        String patternId = i.next();

        if(patternId.isEmpty()) {
          throw new Exception("Empty patternId not allowed");
        }

        if(!JsonFile.get(attributesJson, patternId).isObject()) {
          throw new Exception("Invalid attribute map for: " + patternId);
        }

        JsonNode attributeNode = attributesJson.get(patternId);

        Attributes patternAttributes = new Attributes(patternId, attributeNode);

        Main.log(patternAttributes.toString(), 3);

        attributes.put(patternId, patternAttributes);

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
