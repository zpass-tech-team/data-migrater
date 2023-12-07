package io.mosip.packet.core.constant;

import java.util.Arrays;

public enum FilterCondition {
    EQUAL("="),
    LESS_THEN("<"),
    GREATER_THEN(">"),
    LESS_THEN_AND_EQUAL("<="),
    GREATER_THEN_AND_EQUAL(">="),
    BETWEEN("BETWEEN"),
    IN("IN"),
    LIKE("LIKE");

    private String condition;

    FilterCondition(String condition) {
        this.condition=condition;
    }

    public String format(String fromVal, String toVal, FieldType fieldType) throws Exception {
        String fromValue = ((!FieldType.NUMBER.equals(fieldType) && !condition.equals("IN")) ? "'"+fromVal+"'" : fromVal);
        String toValue = ((!FieldType.NUMBER.equals(fieldType) && !condition.equals("IN")) ? "'"+toVal+"'" : toVal);

        if(condition.equals("BETWEEN")) {
            return " " + condition + " " + fromValue + " AND " + toValue + " ";
        } if(condition.equals("IN")) {
            String[] val = fromVal.split(",");
            return " " + condition + " (" + String.join(",", Arrays.copyOf(Arrays.stream(val).map(s-> "'" + s + "'").toArray(), val.length, String[].class)) +  ") ";
        } if(condition.equals("LIKE")) {
            String val =fromValue.replace("*", "%");
            return " " + condition + " " + val +  " ";
        } else {
            return " " + condition + " " + fromValue + " ";
        }
    }
}
