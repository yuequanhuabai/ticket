package com.ticket.model;

/**
 * 车站信息
 */
public class Station {

    /** 车站简称（拼音首字母），如 bjb */
    private String abbr;

    /** 车站名称（中文），如 北京北 */
    private String name;

    /** 车站电报码，如 VAP */
    private String code;

    /** 车站拼音全拼，如 beijingbei */
    private String pinyin;

    /** 拼音首字母，如 bjb */
    private String initial;

    /** 序号 */
    private String index;

    public Station() {}

    public Station(String abbr, String name, String code, String pinyin, String initial, String index) {
        this.abbr = abbr;
        this.name = name;
        this.code = code;
        this.pinyin = pinyin;
        this.initial = initial;
        this.index = index;
    }

    public String getAbbr() { return abbr; }
    public void setAbbr(String abbr) { this.abbr = abbr; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getPinyin() { return pinyin; }
    public void setPinyin(String pinyin) { this.pinyin = pinyin; }

    public String getInitial() { return initial; }
    public void setInitial(String initial) { this.initial = initial; }

    public String getIndex() { return index; }
    public void setIndex(String index) { this.index = index; }

    @Override
    public String toString() {
        return name + "(" + code + ")";
    }
}
