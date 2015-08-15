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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

public class Main {

  private static int verbose = 1;
  
  public static void main(String args[]) throws Exception {
    log("TextGlass Reference Client " + TextGlassClient.VERSION, 0);

    String pattern = null;
    String attribute = null;
    String patternPatch = null;
    String attributePatch = null;
    
    List<String> tests = new ArrayList<>();

    String testString = null;

    boolean failure = false;
    String warmup = null;

    long start, time;

    //PARSE THE COMMAND LINE

    for(int i = 0; i < args.length; i++) {
      String option = args[i];

      if(option.equals("-h")) {
        printHelp();
        return;
      } else if(option.equals("-p")) {
        if(pattern != null) {
          throw new Exception("pattern file already defined");
        }
        pattern = getParam(args, ++i, "-p file parameter missing");
      } else if(option.equals("-a")) {
        if(attribute != null) {
          throw new Exception("attribute file already defined");
        }
        attribute = getParam(args, ++i, "-a file parameter missing");
      } else if(option.equals("-pp")) {
        if(patternPatch != null) {
          throw new Exception("pattern patch file already defined");
        }
        patternPatch = getParam(args, ++i, "-pp file parameter missing");
      } else if(option.equals("-ap")) {
        if(attributePatch != null) {
          throw new Exception("attribute patch file already defined");
        }
        attributePatch = getParam(args, ++i, "-ap file parameter missing");
      } else if(option.equals("-t")) {
        tests.add(getParam(args, ++i, "-t file parameter missing"));
      } else if(!option.startsWith("-") && testString == null) {
        testString = option;
      } else if(option.equals("-w")) {
        warmup = getParam(args, ++i, "-w iterations missing");
      } else if(option.equals("-q")) {
        verbose = 0;
      } else if(option.equals("-v")) {
        verbose = 2;
      } else if(option.equals("-vv")) {
        verbose = 3;
      } else {
        printHelp();
        throw new Exception("unknown option: " + option);
      }
    }

    if(pattern == null) {
      printHelp();
      throw new Exception("Pattern file required");
    }

    log("Pattern file: '" + pattern + "'", 1);

    if(patternPatch != null) {
      log("Pattern patch file: '" + patternPatch + "'", 1);
    }

    if(attribute != null) {
      log("Attribute file: '" + attribute + "'", 1);
    }

    if(attributePatch != null) {
      log("Attribute patch file: '" + attributePatch + "'", 1);
    }

    //WARMUP
    
    if(warmup != null) {
      runWarmup(warmup, pattern, patternPatch, attribute, attributePatch, tests);
    }

    //BUILD THE TEXTGLASS CLIENT

    start = System.nanoTime();

    TextGlassClient client = new TextGlassClient();

    client.load(loadJson(pattern), loadJson(patternPatch), loadJson(attribute), loadJson(attributePatch));
    
    time = System.nanoTime() - start;
    log("Domain load time: " + getTime(time), 0);

    //DO THE TESTS

    for(String test : tests) {
      log("Test file: '" + test + "'", 1);
      failure |= test(client, new JsonFile(test));
    }

    if(testString != null) {
      log("Test string: '" + testString + "'", 1);

      start = System.nanoTime();

      Map<String, String> result = client.classify(testString);

      time = System.nanoTime() - start;

      log("Test result: " + result, 0);
      log("Test time: " + getTime(time), 0);
    }

    if(failure) {
      throw new Exception("One or more tests failed");
    }
  }

  private static void printHelp() {
    log("Usage: " + Main.class.getName() + " [OPTIONS] [STRING]\n", 0);
    log("  -p <file>            load TextGlass pattern file (REQUIRED)", 0);
    log("  -a <file>            load TextGlass attribute file", 0);
    log("  -pp <file>           load TextGlass pattern patch file", 0);
    log("  -ap <file>           load TextGlass attribute patch file", 0);
    log("  -t <file>            load TextGlass test file", 0);
    log("  -h                   print help", 0);
    log("  -w <iterations>      run warmup", 0);
    log("  -q                   quiet", 0);
    log("  -v                   verbose", 0);
    log("  -vv                  very verbose", 0);
    log("  STRING               test string", 0);
    log("", 0);
  }

  private static String getParam(String args[], int pos, String error) throws Exception {
    if(pos >= args.length) {
      throw new Exception(error);
    } else if(args[pos].startsWith("-") || args[pos].isEmpty()) {
      throw new Exception(error);
    } else {
      return args[pos];
    }
  }

  public static void log(String s, int level) {
    if(level <= verbose) {
      for(int i = 0; i < level; i++) {
        System.out.print("  ");
      }
      
      System.out.println(s);
    }
  }

  private static boolean test(TextGlassClient client, JsonFile tests) throws Exception {

    //PARSE TEST FILE JSON
    
    if(!tests.getType().equals("test")) {
      throw new Exception("Unknown test type: " + tests.getType());
    }

    if(!tests.getDomain().equals(client.getDomain())) {
      throw new Exception("Domains do not match: " + client.getDomain() + " != " + tests.getDomain());
    }

    if(!tests.getDomainVersion().equals(client.getDomainVersion())) {
      throw new Exception("DomainVersions do not match: " + client.getDomainVersion() + " != " + tests.getDomainVersion());
    }

    log("Loading test: " + tests.getDomain() + ", version: " + tests.getDomainVersion(), 1);

    int testCount = 0;
    int passCount = 0;

    long start, time;

    //ITERATE THRU THE TESTS

    start = System.nanoTime();
    
    if(JsonFile.get(tests.getJsonNode(), "tests").isArray()) {
        for(int i = 0; i < tests.getJsonNode().get("tests").size(); i++) {
          JsonNode test = tests.getJsonNode().get("tests").get(i);

          if(JsonFile.get(test, "input").isNull()) {
            throw new Exception("Bad test input found, position: " + testCount);
          }

          String input = test.get("input").asText();
          String resultPatternId = null;

          if(!JsonFile.get(test, "resultPatternId").isNull()) {
            resultPatternId = JsonFile.get(test, "resultPatternId").asText();
          }

          //PERFORM CLASSIFICATION

          Map<String, String> result = client.classify(input);
          String patternId = null;

          if(result != null) {
            patternId = result.get("patternId");
          }

          //CHECK IF IT PASSES

          boolean pass = false;
          int failedAttributes = 0;

          if(resultPatternId == null) {
            if(patternId == null) {
              pass = true;
            }
          } else if(resultPatternId.equals(patternId)) {
            pass = true;
          }

          if(pass && JsonFile.get(test, "resultAttributes").isObject()) {
            for(Iterator<String> j = test.get("resultAttributes").getFieldNames(); j.hasNext();) {
              String key = j.next();
              String expectedValue = test.get("resultAttributes").get(key).asText();
              String value = null;

              if(result != null) {
                value = result.get(key);
              }

              if(!expectedValue.equals(value)) {
                log("FAILED, expected attribute for " + key + ": " + expectedValue + ", found: " + value, 2);
                failedAttributes++;
              }
            }
          }

          //PRINT RESULTS

          if(pass && failedAttributes == 0) {
            passCount++;
            log("Passed, expected patternId: " + resultPatternId, 2);
          } else if(!pass) {
            log("FAILED, expected patternId: " + resultPatternId + ", found: " + patternId, 1);
          }

          testCount++;
        }
      }

    time = System.nanoTime() - start;

    log("Test passed " + passCount + " out of " + testCount + ". " + (testCount == passCount ? "PASS" : "FAIL"), 0);
    log("Test time: " + getTime(time), 0);

    return testCount != passCount;
  }

  public static void runWarmup(String warmupStr, String p, String pp, String a, String ap, List<String> t) throws Exception {
    int warmup = 0;

    try {
      warmup = Integer.parseInt(warmupStr);
    } catch(NumberFormatException e) {
      throw new Exception("Invalid warmup value: " + warmupStr);
    }

    if(warmup < 1) {
      throw new Exception("Invalid warmup value: " + warmup);
    }

    log("Warmup " + warmup + " iterations(s)...", 0);

    int origVerbose = verbose;
    verbose = -1;

    long iterations = 0;

    while(iterations < warmup) {
      TextGlassClient client = new TextGlassClient();

      client.load(loadJson(p), loadJson(pp), loadJson(a), loadJson(ap));

      for(String test : t) {
        test(client, new JsonFile(test));
      }

      iterations++;
    }

    System.gc();
    System.runFinalization();

    verbose = origVerbose;

    log("Warmup completed", 0);
  }

  private static JsonFile loadJson(String path) throws Exception {
    if(path == null) {
      return null;
    } else {
      return new JsonFile(path);
    }
  }

  public static String getTime(long ns)
  {
      return ns / (1000 * 1000 * 1000) + "s " +
          ns / (1000 * 1000) % 1000 + "ms " +
          ns / 1000 % 1000 + "." + ns % 1000 + "us";
  }
}
