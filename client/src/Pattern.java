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

public class Pattern {
  private final String patternId;
  private final PatternType patternType;
  private final List<String> patternTokens;

  private final RankType rankType;
  private final int rankValue;

  private enum PatternType {
    Simple, SimpleAnd, SimpleOrderedAnd
  };

  private enum RankType {
    Strong, Weak, None
  };

  public Pattern(JsonNode json) throws Exception {

    //PARSE PATTERN JSON
    
    if(json.get("patternId") == null || json.get("patternId").asText().isEmpty()) {
      throw new Exception("patternId not found");
    }

    patternId = json.get("patternId").asText();

    if(JsonFile.empty(json, "patternType")) {
      throw new Exception("patternType not found in " + patternId);
    }

    if(!JsonFile.get(json, "patternTokens").isArray()) {
      throw new Exception("patternTokens array not found in " + patternId);
    }

    if(JsonFile.empty(json, "rankType")) {
      throw new Exception("rankType not found in " + patternId);
    }

    patternType = PatternType.valueOf(json.get("patternType").asText());
    rankType = RankType.valueOf(json.get("rankType").asText());

    String rankValueStr = (json.get("rankValue") != null ? json.get("rankValue").asText() : "0");
    rankValue = Integer.parseInt(rankValueStr);

    if(rankValue > 1000 || rankValue < -1000) {
      throw new Exception("Invalid rankValue in " + patternId + ": " + rankValue);
    }

    if(rankType.equals(RankType.Strong) && rankValue != 0) {
      throw new Exception("Strong patterns cannot have a rankValue");
    }

    patternTokens = new ArrayList<>();

    for(int i = 0; i < json.get("patternTokens").size(); i++) {
      JsonNode patternToken = json.get("patternTokens").get(i);

      if(patternToken.asText().isEmpty()) {
        throw new Exception("Empty patternToken in " + patternId);
      }

      patternTokens.add(patternToken.asText());
    }

    if(patternTokens.isEmpty()) {
      throw new Exception("No patternTokens found for: " + patternId);
    }

    if(patternTokens.size() < 2 && (patternType.equals(PatternType.SimpleAnd) ||
        patternType.equals(PatternType.SimpleOrderedAnd))) {
      throw new Exception("patternType " + patternType + " requires more than 1 patternToken: " + patternTokens);
    }
  }

  //IS PATTERN VALID FOR MATCHED TOKENS
  public boolean isValid(List<String> matchedTokens) {
    int lastFound = -1;

    for(String patternToken : patternTokens) {
      int found = matchedTokens.indexOf(patternToken);

      if(found == -1 && (isSimpleAnd() || isSimpleOrderedAnd())) {
        return false;
      }

      if(found >= 0 && isSimple()) {
        return true;
      }

      if(isSimpleOrderedAnd()) {
        if(found <= lastFound) {
          return false;
        } else {
          lastFound = found;
        }
      }
    }

    if(isSimple()) {
      return false;
    } else {
      return true;
    }
  }

  //RANK COMPARED TO OTHER PATTERNS
  public long getRank() {
    long rank = rankValue;

    if(isWeak()) {
      rank += 100000;
    } else if(isStrong()) {
      return 10000000;
    }

    return rank;
  }

  //LENGTH OF MATCHED TOKENS, FOR RANKING
  public long getMatchedLength(List<String> matchedTokens) {
    int length = 0;

    for(String patternToken : patternTokens) {
      int found = matchedTokens.indexOf(patternToken);

      if(found >= 0) {
        length += patternToken.length();
      }
    }
    
    return length;
  }

  @Override
  public String toString() {
    return patternId;
  }

  public String toStringRank(List<String> matchedTokens) {
    return patternId + "(" + getRank() + "," + getMatchedLength(matchedTokens) + ")";
  }

  public String toStringFull() {
    return "patternId: " + patternId + ", patternType: " + patternType +
            patternTokens + ", rankType: " + rankType + ":" + rankValue;
  }

  public String getPatternId() {
    return patternId;
  }

  public List<String> getPatternTokens() {
    return patternTokens;
  }

  public boolean isStrong() {
    return rankType.equals(RankType.Strong);
  }

  public boolean isWeak() {
    return rankType.equals(RankType.Weak);
  }

  public boolean isNone() {
    return rankType.equals(RankType.None);
  }

  public boolean isSimple() {
    return patternType.equals(PatternType.Simple);
  }

  public boolean isSimpleAnd() {
    return patternType.equals(PatternType.SimpleAnd);
  }

  public boolean isSimpleOrderedAnd() {
    return patternType.equals(PatternType.SimpleOrderedAnd);
  }
}
