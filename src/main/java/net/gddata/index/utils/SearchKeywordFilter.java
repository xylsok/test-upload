package net.gddata.index.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhangzf on 17/4/28.
 */
public class SearchKeywordFilter {


    public static String escape(String str) {
        if ("".equals(str) || null == str) {
            return "";
        }
        String regEx = "";
        //非专业搜索排除 双引号
        regEx = "((?=[\\x21-\\x7e]+)[^A-Za-z0-9\"])";
        str = str.replaceAll(regEx, " ");
        if (!appearNumber(str, "\"")) {
            StringBuilder sb = new StringBuilder(str);
            int i = str.lastIndexOf("\"");
            str = sb.replace(i, i+1, " ").toString();
        }
        return str;
    }

    public static String removeInvalidChart(String str) {
        if ("".equals(str) || null == str) {
            return "";
        }
        str = str.replaceAll("((?=[\\x21-\\x7e]+)[^A-Za-z0-9])", " ");
        return str;
    }


    public static Boolean appearNumber(String srcText, String findText) {
        int count = 0;
        Pattern p = Pattern.compile(findText);
        Matcher m = p.matcher(srcText);
        while (m.find()) {
            count++;
        }
        if (count % 2 == 0) {
            return true;
        } else {
            return false;
        }
    }
}
