package org.tomahawk.libtomahawk.resolver;/*
* Diff Match and Patch
*
* Copyright 2006 Google Inc.
* http://code.google.com/p/google-diff-match-patch/
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* Major modifications in 2015 by Enno Gottschalk <mrmaffen@googlemail.com>
*/

import java.util.HashMap;
import java.util.Map;

public class Bitap {

    public static class Result {

        public int index = -1;

        public int errors = -1;
    }

    /**
     * Locate the best instance of 'pattern' in 'text' near 'loc' using the Bitap algorithm. Returns
     * -1 if no match found.
     *
     * @param text    The text to search.
     * @param pattern The pattern to search for.
     * @return Best match index or -1.
     */
    public static Result indexOf(String text, String pattern, int tolerance) {
        Result result = new Result();

        // Is there an exact match? (speedup)
        int exactIndex = text.indexOf(pattern);
        if (exactIndex != -1) {
            result.index = exactIndex;
            result.errors = 0;
            return result;
        }

        // Initialise the alphabet.
        Map<Character, Integer> alphabet = initAlphabet(pattern);

        // Initialise the bit arrays.
        int matchmask = 1 << (pattern.length() - 1);

        int[] last_rd = new int[0];
        for (int d = 0; d <= tolerance; d++) {

            int[] rd = new int[text.length() + pattern.length() + 2];
            rd[text.length() + pattern.length() + 1] = (1 << d) - 1;
            for (int j = text.length() + pattern.length(); j > 0; j--) {
                int charMatch;
                if (text.length() <= j - 1 || !alphabet.containsKey(text.charAt(j - 1))) {
                    // Out of range.
                    charMatch = 0;
                } else {
                    charMatch = alphabet.get(text.charAt(j - 1));
                }
                if (d == 0) {
                    // First pass: exact match.
                    rd[j] = ((rd[j + 1] << 1) | 1) & charMatch;
                } else {
                    // Subsequent passes: fuzzy match.
                    rd[j] = (((rd[j + 1] << 1) | 1) & charMatch)
                            | (((last_rd[j + 1] | last_rd[j]) << 1) | 1) | last_rd[j + 1];
                }
                if ((rd[j] & matchmask) != 0) {
                    result.index = j - 1;
                    result.errors = d;
                    return result;
                }
            }
            last_rd = rd;
        }
        return result;
    }

    /**
     * Initialise the alphabet for the Bitap algorithm.
     *
     * @param pattern The text to encode.
     * @return Hash of character locations.
     */
    public static Map<Character, Integer> initAlphabet(String pattern) {
        Map<Character, Integer> s = new HashMap<>();
        char[] char_pattern = pattern.toCharArray();
        for (char c : char_pattern) {
            s.put(c, 0);
        }
        int i = 0;
        for (char c : char_pattern) {
            s.put(c, s.get(c) | (1 << (pattern.length() - i - 1)));
            i++;
        }
        return s;
    }

}
