package io.mosip.data.constant;

public enum FilterCondition {
    EQUAL("="),
    LESS_THEN("<"),
    GREATER_THEN(">"),
    LESS_THEN_AND_EQUAL("<="),
    GREATER_THEN_AND_EQUAL(">="),
    BETWEEN("BETWEEN");

    private String condition;

    FilterCondition(String condition) {
        this.condition=condition;
    }

    public String format(String fromVal, String toVal, FieldType fieldType) {
        String fromValue = ((!FieldType.NUMBER.equals(fieldType)) ? "'"+fromVal+"'" : fromVal);
        String toValue = ((!FieldType.NUMBER.equals(fieldType)) ? "'"+toVal+"'" : toVal);

        if(condition.equals("BETWEEN")) {
            return " " + condition + " " + fromValue + " AND " + toValue + " ";
        } else {
            return " " + condition + " " + fromValue + " ";
        }
    }
}
