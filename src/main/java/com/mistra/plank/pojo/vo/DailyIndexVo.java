package com.mistra.plank.pojo.vo;

import com.mistra.plank.pojo.entity.DailyIndex;

public class DailyIndexVo extends DailyIndex {

    private static final long serialVersionUID = 1L;

    private String name;
    private String abbreviation;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

}