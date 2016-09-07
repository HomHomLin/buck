package com.facebook.buck.android;


import com.android.dex.DexSimpleInfo;

import org.apache.commons.lang.StringUtils;

/**
 * Created by zongwu on 16/9/7.
 */
public class DexInfo extends DexSimpleInfo {

  public DexInfo() {

  }

  public DexInfo(int fieldNum, int methodNum, int classNum) {
    super(fieldNum, methodNum, classNum);
  }
  public DexInfo(DexSimpleInfo info){
    super(info.getFieldNum(),info.getMethodNum(),info.getClassNum());
  }

  @Override
  public String toString() {
    return fieldNum + "," + methodNum + "," + classNum;
  }

  public static DexInfo fromString(String string) {
    if (StringUtils.isNotEmpty(string)) {
      String[] str = string.split(",");
      if (str.length > 0) {
        return new DexInfo(
            Integer.valueOf(str[0]),
            Integer.valueOf(str[1]),
            Integer.valueOf(str[2]));
      }
    }
    return new DexInfo();
  }
}
