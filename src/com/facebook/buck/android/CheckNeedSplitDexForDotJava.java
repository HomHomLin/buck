package com.facebook.buck.android;

import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.log.Logger;
import com.facebook.buck.model.BuildTargets;
import com.facebook.buck.rules.AbstractBuildRule;
import com.facebook.buck.rules.BuildContext;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildableContext;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.step.ExecutionContext;
import com.facebook.buck.step.Step;
import com.facebook.buck.step.StepExecutionResult;
import com.facebook.buck.step.fs.MakeCleanDirectoryStep;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;

import javax.annotation.Nullable;

/**
 * Created by zongwu on 16/9/3.
 */
public class CheckNeedSplitDexForDotJava extends AbstractBuildRule {

  private static final int MAX_DEX_FIELD_NUM = 60000;
  private static final Logger logger = Logger.get(CheckNeedSplitDexForDotJava.class);
  private String primePackageName;
  private AaptPackageResources aaptPackageResources;

  protected CheckNeedSplitDexForDotJava(
      BuildRuleParams buildRuleParams,
      SourcePathResolver resolver,
      String primePackageName,
      AaptPackageResources aaptPackageResources) {
    super(buildRuleParams, resolver);
    this.primePackageName = primePackageName;
    this.aaptPackageResources = aaptPackageResources;
  }

  @Override
  public ImmutableList<Step> getBuildSteps(
      BuildContext context, BuildableContext buildableContext) {
    buildableContext.recordArtifact(getPathToOutput());

    return ImmutableList.of(
        new MakeCleanDirectoryStep(getProjectFilesystem(), getPathToOutput().getParent()),
        new CheckStep());
  }

  @Nullable
  @Override
  public Path getPathToOutput() {
    return BuildTargets.getGenPath(
        getProjectFilesystem(),
        getBuildTarget(),
        "/%s/_check_split_dex_r_dot_java.txt");
  }

  public List<String> getAppPackageNameList(Optional<Integer> index) {
    try {
      if (!index.isPresent()) {
        return new ArrayList<>();
      }
      List<String> stringList = getProjectFilesystem().readLines(getPathToOutput());
      if (stringList.size() >= index.get()) {
        String content = stringList.get(index.get());
        if (StringUtils.isNotEmpty(content)) {
          List<String> result = new ArrayList<>();
          String[] str = content.split(",");
          result.addAll(Arrays.asList(str));
          return result;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new ArrayList<>();
  }

  private class CheckStep implements Step {

    @Override
    public StepExecutionResult execute(ExecutionContext context) throws IOException, InterruptedException {
      checkNeedSplitDex(primePackageName, aaptPackageResources);
      return StepExecutionResult.SUCCESS;
    }

    @Override
    public String getShortName() {
      return "check_split_dex";
    }

    @Override
    public String getDescription(ExecutionContext context) {
      return "check_split_dex result : " + getPathToOutput();
    }
  }


  /**
   * if all the R.java field num bigger than 65535, need split R.java to different dexes.
   *
   * @param primePackageName     exclude package name
   * @param aaptPackageResources for get R.java file path
   * @return result split package name
   */
  public void checkNeedSplitDex(
      String primePackageName, AaptPackageResources aaptPackageResources) {
    final Path sourceDir = aaptPackageResources.getPathToGeneratedRDotJavaSrcFiles();
    final ProjectFilesystem projectFilesystem = aaptPackageResources.getProjectFilesystem();
    final LinkedHashMap<String, Integer> countResult = new LinkedHashMap<>();
    List<List<String>> result = new ArrayList<>();
    try {
      if (!projectFilesystem.exists(sourceDir)) {
        throw new RuntimeException(String.format("%s not exist! ", sourceDir));
      }
      projectFilesystem.walkFileTree(
          sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(
                Path file, BasicFileAttributes attrs) throws IOException {
              if (attrs.isDirectory()) {
                return FileVisitResult.CONTINUE;
              }
              if (!file.getFileName().toString().endsWith(".java")) {
                return FileVisitResult.CONTINUE;
              }
              String packageNamesKey = null;
              Matcher m;
              int num = 0;
              List<String> lines = projectFilesystem.readLines(file);
              for (String line : lines) {
                if (packageNamesKey == null) {
                  m = TrimUberRDotJava.R_DOT_JAVA_PACKAGE_NAME_PATTERN.matcher(line);
                  if (m.find()) {
                    packageNamesKey = m.group(1);
                  } else {
                    continue;
                  }
                }
                m = TrimUberRDotJava.R_DOT_JAVA_LINE_PATTERN.matcher(line);
                if (m.find()) {
                  num++;
                }
              }
              countResult.put(packageNamesKey, num);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      e.printStackTrace();
    }
    countResult.remove(primePackageName);
    logger.warn("countResult " + countResult.toString());

    if (countResult.size() > 1) {
      int num = 0;
      List<String> tmp = new ArrayList<>();
      for (String key : countResult.keySet()) {
        if (num + countResult.get(key) < MAX_DEX_FIELD_NUM) {
          num += countResult.get(key);
          tmp.add(key);
        } else {
          List<String> copy = new ArrayList<>();
          copy.addAll(tmp);
          result.add(copy);
          //logger.error("result add " + copy.size());
          tmp.clear();
          num = 0;
          tmp.add(key);
        }
      }
      if (tmp.size() > 0) {
        result.add(tmp);
        //logger.error("result add " + tmp.size());
      }
    }
    //logger.error("result " + result.size());
    try {
      BufferedWriter out = new BufferedWriter(
          new FileWriter(
              projectFilesystem.resolve(getPathToOutput()).toFile()));
      for (List<String> stringList : result) {
        if (stringList.size() > 0) {
          String content = StringUtils.join(stringList, ",");
          out.write(content);
          out.newLine();
        }
      }
      out.flush();
      out.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
