/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.libtomahawk.resolver;

import java.util.ArrayList;
import java.util.List;

public class ResultScoring {

    private static final int ERROR_TOLERANCE_RATIO = 5;

    private static final char[] sDelimiters =
            new char[]{'(', '[', '{', ' ', '\n', '-', '/', '\\', ' ', ')', '[', '}'};

    /**
     * This method determines how similar the given result is to the search string.
     */
    public static float calculateScore(String result, String query) {
        float totalScore = 0f;
        int lastIndex = 0;
        List<String> queryParts = splitUp(query, 32); // bitap only allows a max of 32 chars per run
        for (String queryPart : queryParts) {
            // how many errors do we allow
            int tolerance = queryPart.length() / ERROR_TOLERANCE_RATIO;
            String partialResult = result.substring(lastIndex, result.length());
            Bitap.Result r = Bitap.indexOf(partialResult, queryPart, tolerance);
            if (r.index >= 0) {
                float errorPenalty = 0f;
                if (tolerance > 0) {
                    // worst case 30% score penalty
                    errorPenalty = (float) r.errors / tolerance * .3f;
                }
                float patternRatio;
                float denominator = (float) Math.max(result.length(), queryPart.length());
                if (denominator > 0) {
                    patternRatio =
                            (float) Math.min(result.length(), queryPart.length()) / denominator;
                } else {
                    // both query and result are empty Strings
                    patternRatio = 1f;
                }
                totalScore += patternRatio * (1f - errorPenalty); // apply the error penalty
                lastIndex = r.index + queryPart.length();
                if (lastIndex >= result.length()) {
                    // nothing to search for anymore
                    break;
                }
            }
        }
        return totalScore;
    }

    private static List<String> splitUp(String s, int maxLength) {
        List<String> parts = new ArrayList<>();
        if (s.length() <= maxLength) {
            // Nothing to do
            parts.add(s);
            return parts;
        }
        boolean foundNothing = false;
        while (s.length() > maxLength && !foundNothing) {
            for (int i = 0; i < sDelimiters.length; i++) {
                char delimiter = sDelimiters[i];
                int index = s.indexOf(delimiter, s.length() - maxLength);
                if (index != -1) {
                    // We found a delimiter
                    String lastPart = s.substring(index, s.length());
                    if (lastPart.length() > maxLength) {
                        parts.addAll(0, splitUp(lastPart, maxLength));
                    } else {
                        parts.add(0, lastPart);
                    }
                    s = s.substring(0, index);
                    break;
                } else if (i == sDelimiters.length - 1) {
                    // we weren't able to find any more delimiters
                    foundNothing = true;
                    break;
                }
            }
        }
        while (s.length() > maxLength) {
            // Still too long? Well then we have to just chop it off :/
            String lastPart = s.substring(s.length() - maxLength, s.length());
            parts.add(0, lastPart);
            s = s.substring(0, s.length() - maxLength);
        }
        // Prepend all that's left
        parts.add(0, s);
        return parts;
    }

    /**
     * Clean up the given String.
     *
     * @param replaceArticle whether or not the prefix "the " should be removed
     * @return the clean String
     */
    public static String cleanUpString(String in, boolean replaceArticle) {
        String out = in.toLowerCase().trim().replaceAll("[\\s]{2,}", " ");
        if (replaceArticle && out.startsWith("the ")) {
            out = out.substring(4);
        }
        return out;
    }

}
