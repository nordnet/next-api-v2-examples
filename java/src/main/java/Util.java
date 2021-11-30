/*
 * Copyright 2021 Nordnet Bank AB
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

public class Util {

    public static void assertValidateJSONString(String s) {
        if (!s.endsWith("\n")) {
            System.err.println("JSON-strings must end with a newline, \n" + prettyPrintJSON(s));
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        try {
            objectMapper.readTree(s);
        } catch (IOException e) {
            System.err.println("JSON is invalid, double-check that the following is correct, \n" + prettyPrintJSON(s));
        }
    }

    public static String prettyPrintJSON(JsonNode node) {
        if (node == null) {
            return "Json is null";
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        String valueAsString;
        try {
            valueAsString = mapper.writer().writeValueAsString(node);
        } catch (JsonProcessingException | NullPointerException e) {
            System.err.println("Could not parse JsonNode " + (node == null ? "null" : node.toString()));
            valueAsString = node.toString();
        }
        System.out.println(valueAsString);
        return valueAsString;
    }

    public static String prettyPrintJSON(String s) {
        JsonNode node = null;
        try {
            node = new ObjectMapper().readTree(s);
        } catch (JsonProcessingException e) {
            System.err.println("Could not make JsonNode out of " + s);
        }
        return prettyPrintJSON(node);
    }
}
