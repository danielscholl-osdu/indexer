// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.util.parser;

import com.google.common.base.Strings;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.Numbers;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class NumberParser {

    public int parseInteger(String attributeName, Object attributeVal) {
        String value = attributeVal == null ? null : String.valueOf(attributeVal);

        // elastic allows empty value for numeric types
        if (Strings.isNullOrEmpty(value)) {
            return 0;
        }

        if (!NumberUtils.isCreatable(value)) {
            throw new IllegalArgumentException(String.format("number parsing error: attribute: %s | value: %s", attributeName, attributeVal));
        }

        double doubleValue = objectToDouble(value);
        if (doubleValue < Integer.MIN_VALUE || doubleValue > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(String.format("number parsing error, integer out of range: attribute: %s | value: %s", attributeName, attributeVal));
        }

        if (attributeVal instanceof Number) {
            return ((Number) attributeVal).intValue();
        }

        return (int) doubleValue;
    }

    public long parseLong(String attributeName, Object attributeVal) {
        String value = attributeVal == null ? null : String.valueOf(attributeVal);

        // elastic allows empty value for numeric types
        if (Strings.isNullOrEmpty(value)) {
            return 0L;
        }

        if (!NumberUtils.isCreatable(value)) {
            throw new IllegalArgumentException(String.format("number parsing error: attribute: %s | value: %s", attributeName, attributeVal));
        }

        if (attributeVal instanceof Long) {
            return (Long) attributeVal;
        }

        try {
            double doubleValue = objectToDouble(value);
            // this check does not guarantee that value is inside MIN_VALUE/MAX_VALUE because values up to 9223372036854776832 will
            // be equal to Long.MAX_VALUE after conversion to double. More checks ahead.
            if (doubleValue < Long.MIN_VALUE || doubleValue > Long.MAX_VALUE) {
                throw new IllegalArgumentException(String.format("number parsing error, long out of range: attribute: %s | value: %s", attributeName, attributeVal));
            }
        } catch (NumberFormatException e) {
            // Numbers.toLong will try with BigDecimal
        }

        // longs need special handling so we don't lose precision while parsing
        String stringValue = (attributeVal instanceof BytesRef) ? ((BytesRef) attributeVal).utf8ToString() : value.toString();
        return Numbers.toLong(stringValue, false);
    }

    public float parseFloat(String attributeName, Object attributeVal) {
        String value = attributeVal == null ? null : String.valueOf(attributeVal);

        // elastic allows empty value for numeric types
        if (Strings.isNullOrEmpty(value)) {
            return 0.0f;
        }

        if (!NumberUtils.isCreatable(value)) {
            throw new IllegalArgumentException(String.format("number parsing error: attribute: %s | value: %s", attributeName, attributeVal));
        }

        final float result;
        if (attributeVal instanceof Number) {
            result = ((Number) attributeVal).floatValue();
        } else {
            if (attributeVal instanceof BytesRef) {
                attributeVal = ((BytesRef) attributeVal).utf8ToString();
            }
            result = Float.parseFloat(attributeVal.toString());
        }

        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException(String.format("number parsing error, float only supports finite values: attribute: %s | value: %s", attributeName, attributeVal));
        }

        return result;
    }

    public double parseDouble(String attributeName, Object attributeVal) {
        String value = attributeVal == null ? null : String.valueOf(attributeVal);

        // elastic allows empty value for numeric types
        if (Strings.isNullOrEmpty(value)) {
            return 0.0d;
        }

        if (!NumberUtils.isCreatable(value)) {
            throw new IllegalArgumentException(String.format("number parsing error: attribute: %s | value: %s", attributeName, attributeVal));
        }

        double doubleValue = objectToDouble(value);
        if (!Double.isFinite(doubleValue)) {
            throw new IllegalArgumentException(String.format("number parsing error, double only supports finite values: attribute: %s | value: %s", attributeName, attributeVal));
        }

        return doubleValue;
    }

    /**
     * Converts an Object to a double by checking it against known types first
     */
    private static double objectToDouble(Object value) {
        double doubleValue;

        if (value instanceof Number) {
            doubleValue = ((Number) value).doubleValue();
        } else if (value instanceof BytesRef) {
            doubleValue = Double.parseDouble(((BytesRef) value).utf8ToString());
        } else {
            doubleValue = Double.parseDouble(value.toString());
        }

        return doubleValue;
    }
}