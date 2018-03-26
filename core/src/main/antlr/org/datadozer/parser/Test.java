package org.datadozer.parser;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

class Test {

    Date dateNow() {
        return Date.from(Instant.EPOCH);
    }

    Double sort(Map<String, String> p, Map<String, String> d) {
        return ((0.3 * Double.parseDouble(p.get("sap"))) / 10.0) + (0.7 * Double.parseDouble(p.get("score")));
    }

    Double score(Map<String, String> p, Map<String, String> d) {
        return ((0.3 * Double.parseDouble(p.get("sap"))) / 10.0) + (0.7 * Double.parseDouble(p.get("score")));
    }

    Object column(Map<String, String> p, Map<String, String> d) {
        if (p.get("bypass") == "1") {
            return "";
        } else {
            return "";
        }
    }
}