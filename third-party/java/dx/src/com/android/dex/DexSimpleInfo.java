package com.android.dex;


/**
 * Created by zongwu on 16/9/7.
 */
public class DexSimpleInfo {

  protected int fieldNum;
  protected int methodNum;
  protected int classNum;

  public DexSimpleInfo(int fieldNum, int methodNum, int classNum) {
    this.methodNum = methodNum;
    this.fieldNum = fieldNum;
    this.classNum = classNum;
  }

  public DexSimpleInfo() {

  }

  public int getFieldNum() {
    return fieldNum;
  }

  public void setFieldNum(int fieldNum) {
    this.fieldNum = fieldNum;
  }

  public int getMethodNum() {
    return methodNum;
  }

  public void setMethodNum(int methodNum) {
    this.methodNum = methodNum;
  }

  public int getClassNum() {
    return classNum;
  }

  public void setClassNum(int classNum) {
    this.classNum = classNum;
  }
}
